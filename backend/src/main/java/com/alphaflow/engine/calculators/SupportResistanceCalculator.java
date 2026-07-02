package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.configs.SupportResistanceConfig;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.persistence.entities.SRTouchPoint;
import com.alphaflow.persistence.entities.SupportResistance;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.SRCurrentType;
import com.alphaflow.persistence.enums.SRFilterReason;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.SupportResistanceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class SupportResistanceCalculator {

  private final TickerRepository tickerRepository;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final SupportResistanceRepository srRepo;

  private final SupportResistanceConfig config;

  @Autowired @org.springframework.context.annotation.Lazy private SupportResistanceCalculator self;

  @Autowired
  public SupportResistanceCalculator(
      TickerRepository tickerRepository,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      SupportResistanceRepository srRepo,
      SupportResistanceConfig config) {
    this.tickerRepository = tickerRepository;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.srRepo = srRepo;
    this.config = config;
  }

  public void computeAll() {
    log.info("Starting Support and Resistance computation...");
    List<Ticker> tickers = tickerRepository.findByIsActiveTrue();
    for (Ticker ticker : tickers) {
      try {
        self.computeForTicker(ticker);
      } catch (Exception e) {
        log.error("Failed to compute SR for {}: {}", ticker.getTickerSymbol(), e.getMessage(), e);
      }
    }
    log.info("Support and Resistance computation completed.");
  }

  @Transactional
  public void computeForTicker(Ticker ticker) {
    log.info("Ticker {}: Computing SR lines...", ticker.getTickerSymbol());

    srRepo.deleteByTicker_TickerSymbol(ticker.getTickerSymbol());

    List<PriceBar> dailyBars = loadDailyBars(ticker);
    List<PriceBar> weeklyBars = loadWeeklyBars(ticker);

    List<ActiveLine> dailyActiveLines =
        compute(
            dailyBars,
            config.getDailyWindow(),
            config.getDailyHorizontalMinTouches(),
            config.getDailyMaxBreaks(),
            config.getDailyCbHorizontalPct(),
            config.getDailyCbAngularPct(),
            config.getDailyTolerancePct(),
            config.getDailyProximityPct(),
            config.getDailyAngularMinTouches());
    int dailyLatestIndex = dailyBars.isEmpty() ? 0 : dailyBars.size() - 1;
    List<SupportResistance> dailySr =
        mapToEntities(dailyActiveLines, ticker, Timeframe.DAILY, dailyLatestIndex);
    if (!dailySr.isEmpty()) {
      srRepo.saveAll(dailySr);
    }

    List<ActiveLine> weeklyActiveLines =
        compute(
            weeklyBars,
            config.getWeeklyWindow(),
            config.getWeeklyHorizontalMinTouches(),
            config.getWeeklyMaxBreaks(),
            config.getWeeklyCbHorizontalPct(),
            config.getWeeklyCbAngularPct(),
            config.getWeeklyTolerancePct(),
            config.getWeeklyProximityPct(),
            config.getWeeklyAngularMinTouches());
    int weeklyLatestIndex = weeklyBars.isEmpty() ? 0 : weeklyBars.size() - 1;
    List<SupportResistance> weeklySr =
        mapToEntities(weeklyActiveLines, ticker, Timeframe.WEEKLY, weeklyLatestIndex);
    if (!weeklySr.isEmpty()) {
      srRepo.saveAll(weeklySr);
    }

    log.info(
        "Ticker {}: SR computation completed. Saved {} daily, {} weekly.",
        ticker.getTickerSymbol(),
        dailySr.size(),
        weeklySr.size());
  }

  private static class Pivot {
    int index;
    BigDecimal price;
    LocalDate date;

    Pivot(int index, BigDecimal price, LocalDate date) {
      this.index = index;
      this.price = price;
      this.date = date;
    }
  }

  private static class ActiveLine {
    List<SRTouchPoint> touchPoints;
    SRCurrentType type;
    int breakCount;
    int importance;
    BigDecimal slope;
    BigDecimal intercept;
    SRFilterReason filterReason;
    boolean isHorizontal;
    int establishedAtIndex;

    ActiveLine(
        List<SRTouchPoint> touchPoints,
        SRCurrentType type,
        BigDecimal slope,
        BigDecimal intercept,
        boolean isHorizontal,
        int establishedAtIndex) {
      this.touchPoints = touchPoints;
      this.type = type;
      this.breakCount = 0;
      this.importance = touchPoints.size();
      this.slope = slope;
      this.intercept = intercept;
      this.isHorizontal = isHorizontal;
      this.establishedAtIndex = establishedAtIndex;
    }
  }

  private List<ActiveLine> compute(
      List<PriceBar> bars,
      int window,
      int horizontalMinTouches,
      int maxBreaks,
      int cbHorizontalPct,
      int cbAngularPct,
      double tolerancePct,
      double proximityPct,
      int angularMinTouches) {
    List<ActiveLine> activeLines = new ArrayList<>();
    if (bars.size() < window * 2) return activeLines;

    List<Pivot> pivotHighs = new ArrayList<>();
    List<Pivot> pivotLows = new ArrayList<>();
    BigDecimal tolerance = BigDecimal.valueOf(tolerancePct).divide(new BigDecimal("100"));

    int startIndex = window;

    for (int i = startIndex; i < bars.size() - window; i++) {
      boolean isHigh = true;
      boolean isLow = true;
      BigDecimal currentHigh = bars.get(i).high();
      BigDecimal currentLow = bars.get(i).low();

      for (int j = i - window; j <= i + window; j++) {
        if (j == i) continue;
        if (bars.get(j).high().compareTo(currentHigh) > 0) isHigh = false;
        if (bars.get(j).low().compareTo(currentLow) < 0) isLow = false;
      }

      if (isHigh) {
        Pivot currentPivot = new Pivot(i, currentHigh, bars.get(i).date());

        // 1. Horizontal logic
        boolean addedToExisting = false;
        for (ActiveLine line : activeLines) {
          if (line.filterReason == null
              && line.isHorizontal
              && line.type == SRCurrentType.RESISTANCE) {
            BigDecimal avgPrice = line.intercept;
            BigDecimal diff = currentHigh.subtract(avgPrice).abs();
            BigDecimal thresh = avgPrice.multiply(tolerance);
            if (diff.compareTo(thresh) <= 0) {
              line.touchPoints.add(new SRTouchPoint(currentPivot.date, currentPivot.price));
              line.importance = line.touchPoints.size();

              BigDecimal sum = BigDecimal.ZERO;
              for (SRTouchPoint tp : line.touchPoints) {
                sum = sum.add(tp.getPrice());
              }
              line.intercept =
                  sum.divide(new BigDecimal(line.touchPoints.size()), 4, RoundingMode.HALF_UP);
              addedToExisting = true;
              break;
            }
          }
        }

        if (!addedToExisting) {
          for (Pivot past : pivotHighs) {
            BigDecimal diff = currentHigh.subtract(past.price).abs();
            BigDecimal thresh = past.price.multiply(tolerance);
            if (diff.compareTo(thresh) <= 0) {
              BigDecimal avgPrice =
                  currentHigh.add(past.price).divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
              List<SRTouchPoint> pts = new ArrayList<>();
              pts.add(new SRTouchPoint(past.date, past.price));
              pts.add(new SRTouchPoint(currentPivot.date, currentPivot.price));
              activeLines.add(
                  new ActiveLine(
                      pts,
                      SRCurrentType.RESISTANCE,
                      BigDecimal.ZERO,
                      avgPrice,
                      true,
                      currentPivot.index));
              break;
            }
          }
        }

        // 2. Angular logic
        pivotHighs.add(currentPivot);
        if (pivotHighs.size() >= angularMinTouches) {
          List<Pivot> lastN =
              pivotHighs.subList(pivotHighs.size() - angularMinTouches, pivotHighs.size());
          ActiveLine angularLine = createRegressionLine(lastN, SRCurrentType.RESISTANCE);
          if (angularLine != null) {
            activeLines.add(angularLine);
          }
        }
      }

      if (isLow) {
        Pivot currentPivot = new Pivot(i, currentLow, bars.get(i).date());

        // 1. Horizontal logic
        boolean addedToExisting = false;
        for (ActiveLine line : activeLines) {
          if (line.filterReason == null
              && line.isHorizontal
              && line.type == SRCurrentType.SUPPORT) {
            BigDecimal avgPrice = line.intercept;
            BigDecimal diff = currentLow.subtract(avgPrice).abs();
            BigDecimal thresh = avgPrice.multiply(tolerance);
            if (diff.compareTo(thresh) <= 0) {
              line.touchPoints.add(new SRTouchPoint(currentPivot.date, currentPivot.price));
              line.importance = line.touchPoints.size();

              BigDecimal sum = BigDecimal.ZERO;
              for (SRTouchPoint tp : line.touchPoints) {
                sum = sum.add(tp.getPrice());
              }
              line.intercept =
                  sum.divide(new BigDecimal(line.touchPoints.size()), 4, RoundingMode.HALF_UP);
              addedToExisting = true;
              break;
            }
          }
        }

        if (!addedToExisting) {
          for (Pivot past : pivotLows) {
            BigDecimal diff = currentLow.subtract(past.price).abs();
            BigDecimal thresh = past.price.multiply(tolerance);
            if (diff.compareTo(thresh) <= 0) {
              BigDecimal avgPrice =
                  currentLow.add(past.price).divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
              List<SRTouchPoint> pts = new ArrayList<>();
              pts.add(new SRTouchPoint(past.date, past.price));
              pts.add(new SRTouchPoint(currentPivot.date, currentPivot.price));
              activeLines.add(
                  new ActiveLine(
                      pts,
                      SRCurrentType.SUPPORT,
                      BigDecimal.ZERO,
                      avgPrice,
                      true,
                      currentPivot.index));
              break;
            }
          }
        }

        // 2. Angular logic
        pivotLows.add(currentPivot);
        if (pivotLows.size() >= angularMinTouches) {
          List<Pivot> lastN =
              pivotLows.subList(pivotLows.size() - angularMinTouches, pivotLows.size());
          ActiveLine angularLine = createRegressionLine(lastN, SRCurrentType.SUPPORT);
          if (angularLine != null) {
            activeLines.add(angularLine);
          }
        }
      }

      // Check breaks
      checkBreaks(i, bars.get(i), activeLines, maxBreaks);
    }

    // Check breaks for the remaining bars that weren't checked for pivots
    for (int i = Math.max(startIndex, bars.size() - window); i < bars.size(); i++) {
      checkBreaks(i, bars.get(i), activeLines, maxBreaks);
    }

    for (ActiveLine al : activeLines) {
      if (al.filterReason == null) {
        if (al.isHorizontal) {
          if (al.touchPoints.size() < horizontalMinTouches) {
            al.filterReason = SRFilterReason.NOT_ENOUGH_TOUCHES;
          }
        } else {
          if (al.touchPoints.size() < angularMinTouches) {
            al.filterReason = SRFilterReason.NOT_ENOUGH_TOUCHES;
          }
        }
      }
    }

    List<ActiveLine> validForProximity =
        activeLines.stream()
            .filter(al -> al.filterReason == null)
            .sorted((a, b) -> Integer.compare(b.importance, a.importance))
            .collect(Collectors.toList());

    List<ActiveLine> merged = new ArrayList<>();
    BigDecimal proximityThresh = BigDecimal.valueOf(proximityPct).divide(new BigDecimal("100"));
    int currentIndex = bars.size() - 1;
    BigDecimal currentClose = bars.get(currentIndex).close();

    for (ActiveLine line : validForProximity) {
      BigDecimal lineCurrentExpected =
          line.slope.multiply(new BigDecimal(currentIndex)).add(line.intercept);

      // 1. Circuit Breaker
      int cbPct = line.isHorizontal ? cbHorizontalPct : cbAngularPct;
      BigDecimal upperLimitFactor =
          BigDecimal.ONE.add(BigDecimal.valueOf(cbPct).divide(new BigDecimal("100")));
      BigDecimal lowerLimitFactor =
          BigDecimal.ONE.subtract(BigDecimal.valueOf(cbPct).divide(new BigDecimal("100")));

      BigDecimal upperLimit = currentClose.multiply(upperLimitFactor);
      BigDecimal lowerLimit = currentClose.multiply(lowerLimitFactor);

      if (lineCurrentExpected.compareTo(upperLimit) > 0
          || lineCurrentExpected.compareTo(lowerLimit) < 0) {
        line.filterReason = SRFilterReason.CIRCUIT_BREAKER;
        continue;
      }

      // 2. Proximity check
      boolean drop = false;
      for (ActiveLine stronger : merged) {
        BigDecimal strongerCurrentExpected =
            stronger.slope.multiply(new BigDecimal(currentIndex)).add(stronger.intercept);
        BigDecimal diff = lineCurrentExpected.subtract(strongerCurrentExpected).abs();
        BigDecimal allowedDiff = strongerCurrentExpected.multiply(proximityThresh);

        if (diff.compareTo(allowedDiff) <= 0) {
          drop = true;
          break;
        }
      }
      if (drop) {
        line.filterReason = SRFilterReason.PROXIMITY;
      } else {
        merged.add(line);
      }
    }

    // Structural polarity is preserved

    return activeLines;
  }

  private void checkBreaks(int index, PriceBar bar, List<ActiveLine> activeLines, int maxBreaks) {
    BigDecimal close = bar.close();
    for (ActiveLine line : activeLines) {
      // Avoid lookahead bias by only evaluating bars that occur after the line was established
      if (index <= line.establishedAtIndex) continue;

      BigDecimal expectedPrice = line.slope.multiply(new BigDecimal(index)).add(line.intercept);

      if (line.type == SRCurrentType.RESISTANCE && close.compareTo(expectedPrice) > 0) {
        line.breakCount++;
        if (line.breakCount > maxBreaks) {
          line.filterReason = SRFilterReason.TOO_MANY_BREAKS;
        } else {
          line.type = SRCurrentType.SUPPORT; // Flip polarity
        }
      } else if (line.type == SRCurrentType.SUPPORT && close.compareTo(expectedPrice) < 0) {
        line.breakCount++;
        if (line.breakCount > maxBreaks) {
          line.filterReason = SRFilterReason.TOO_MANY_BREAKS;
        } else {
          line.type = SRCurrentType.RESISTANCE; // Flip polarity
        }
      }
    }
  }

  private ActiveLine createRegressionLine(List<Pivot> pivots, SRCurrentType type) {
    if (pivots.size() < 3) return null;
    BigDecimal sumX = BigDecimal.ZERO;
    BigDecimal sumY = BigDecimal.ZERO;
    for (Pivot p : pivots) {
      sumX = sumX.add(new BigDecimal(p.index));
      sumY = sumY.add(p.price);
    }
    BigDecimal size = new BigDecimal(pivots.size());
    BigDecimal meanX = sumX.divide(size, 8, RoundingMode.HALF_UP);
    BigDecimal meanY = sumY.divide(size, 8, RoundingMode.HALF_UP);

    BigDecimal num = BigDecimal.ZERO;
    BigDecimal den = BigDecimal.ZERO;
    for (Pivot p : pivots) {
      BigDecimal xDiff = new BigDecimal(p.index).subtract(meanX);
      BigDecimal yDiff = p.price.subtract(meanY);
      num = num.add(xDiff.multiply(yDiff));
      den = den.add(xDiff.multiply(xDiff));
    }
    if (den.compareTo(BigDecimal.ZERO) == 0) return null;

    BigDecimal slope = num.divide(den, 4, RoundingMode.HALF_UP);
    BigDecimal intercept = meanY.subtract(slope.multiply(meanX)).setScale(4, RoundingMode.HALF_UP);

    List<SRTouchPoint> touchPoints = new ArrayList<>();
    for (Pivot p : pivots) {
      touchPoints.add(new SRTouchPoint(p.date, p.price));
    }

    int establishedAtIndex = pivots.get(pivots.size() - 1).index;

    return new ActiveLine(touchPoints, type, slope, intercept, false, establishedAtIndex);
  }

  private List<SupportResistance> mapToEntities(
      List<ActiveLine> lines, Ticker ticker, Timeframe timeframe, int latestBarIndex) {
    return lines.stream()
        .map(
            al -> {
              SupportResistance entity = new SupportResistance();
              entity.setTicker(ticker);
              entity.setTimeframe(timeframe);
              entity.setSlope(al.slope);
              entity.setIntercept(al.intercept);

              entity.setCurrentPrice(
                  al.slope.multiply(BigDecimal.valueOf(latestBarIndex)).add(al.intercept));

              entity.setCurrentType(al.type);
              entity.setImportance(al.importance);
              entity.setTouchPoints(al.touchPoints);
              entity.setBreakCount(al.breakCount);
              entity.setFilterReason(al.filterReason);
              return entity;
            })
        .collect(Collectors.toList());
  }

  private List<PriceBar> loadDailyBars(Ticker ticker) {
    return dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
        .map(
            d ->
                new PriceBar(
                    d.getPriceDate(),
                    d.getPriceOpen(),
                    d.getPriceHigh(),
                    d.getPriceLow(),
                    d.getPriceClose(),
                    d.getVolume()))
        .toList();
  }

  private List<PriceBar> loadWeeklyBars(Ticker ticker) {
    return weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
        .map(
            w ->
                new PriceBar(
                    w.getPriceDate(),
                    w.getPriceOpen(),
                    w.getPriceHigh(),
                    w.getPriceLow(),
                    w.getPriceClose(),
                    w.getVolume()))
        .toList();
  }
}
