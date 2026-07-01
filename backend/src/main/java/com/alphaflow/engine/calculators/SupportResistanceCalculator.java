package com.alphaflow.engine.calculators;

import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.persistence.entities.DailySupportResistance;
import com.alphaflow.persistence.entities.SRTouchPoint;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklySupportResistance;
import com.alphaflow.persistence.enums.SRCurrentType;
import com.alphaflow.persistence.enums.SRFilterReason;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.DailySupportResistanceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import com.alphaflow.persistence.repositories.WeeklySupportResistanceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
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
  private final DailySupportResistanceRepository dailySrRepo;
  private final WeeklySupportResistanceRepository weeklySrRepo;

  @Autowired @org.springframework.context.annotation.Lazy private SupportResistanceCalculator self;

  @Autowired
  public SupportResistanceCalculator(
      TickerRepository tickerRepository,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailySupportResistanceRepository dailySrRepo,
      WeeklySupportResistanceRepository weeklySrRepo) {
    this.tickerRepository = tickerRepository;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailySrRepo = dailySrRepo;
    this.weeklySrRepo = weeklySrRepo;
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

    dailySrRepo.deleteByTicker_TickerSymbol(ticker.getTickerSymbol());
    weeklySrRepo.deleteByTicker_TickerSymbol(ticker.getTickerSymbol());

    List<PriceBar> dailyBars = loadDailyBars(ticker);
    List<PriceBar> weeklyBars = loadWeeklyBars(ticker);

    List<ActiveLine> dailyActiveLines = compute(dailyBars, 10);
    List<DailySupportResistance> dailySr = new ArrayList<>();
    for (ActiveLine al : dailyActiveLines) {
      DailySupportResistance dsr = new DailySupportResistance();
      dsr.setTicker(ticker);
      dsr.setSlope(al.slope);
      dsr.setIntercept(al.intercept);
      dsr.setCurrentType(al.type);
      dsr.setImportance(al.importance);
      dsr.setTouchPoints(al.touchPoints);
      dsr.setBreakCount(al.breakCount);
      dsr.setFilterReason(al.filterReason);
      dailySr.add(dsr);
    }
    if (!dailySr.isEmpty()) {
      dailySrRepo.saveAll(dailySr);
    }

    List<ActiveLine> weeklyActiveLines = compute(weeklyBars, 10);
    List<WeeklySupportResistance> weeklySr = new ArrayList<>();
    for (ActiveLine al : weeklyActiveLines) {
      WeeklySupportResistance wsr = new WeeklySupportResistance();
      wsr.setTicker(ticker);
      wsr.setSlope(al.slope);
      wsr.setIntercept(al.intercept);
      wsr.setCurrentType(al.type);
      wsr.setImportance(al.importance);
      wsr.setTouchPoints(al.touchPoints);
      wsr.setBreakCount(al.breakCount);
      wsr.setFilterReason(al.filterReason);
      weeklySr.add(wsr);
    }
    if (!weeklySr.isEmpty()) {
      weeklySrRepo.saveAll(weeklySr);
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

    ActiveLine(
        List<SRTouchPoint> touchPoints,
        SRCurrentType type,
        BigDecimal slope,
        BigDecimal intercept,
        boolean isHorizontal) {
      this.touchPoints = touchPoints;
      this.type = type;
      this.breakCount = 0;
      this.importance = touchPoints.size();
      this.slope = slope;
      this.intercept = intercept;
      this.isHorizontal = isHorizontal;
    }
  }

  private List<ActiveLine> compute(List<PriceBar> bars, int window) {
    List<ActiveLine> activeLines = new ArrayList<>();
    if (bars.size() < window * 2) return activeLines;

    List<Pivot> pivotHighs = new ArrayList<>();
    List<Pivot> pivotLows = new ArrayList<>();
    BigDecimal tolerance = new BigDecimal("0.01"); // 1%

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
          if (line.filterReason == null && line.isHorizontal && line.type == SRCurrentType.RESISTANCE) {
            BigDecimal avgPrice = line.intercept;
            BigDecimal diff = currentHigh.subtract(avgPrice).abs();
            BigDecimal thresh = avgPrice.multiply(tolerance);
            if (diff.compareTo(thresh) <= 0) {
              line.touchPoints.add(new SRTouchPoint(currentPivot.date, avgPrice));
              line.importance = line.touchPoints.size();
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
              pts.add(new SRTouchPoint(past.date, avgPrice));
              pts.add(new SRTouchPoint(currentPivot.date, avgPrice));
              activeLines.add(
                  new ActiveLine(pts, SRCurrentType.RESISTANCE, BigDecimal.ZERO, avgPrice, true));
              break;
            }
          }
        }

        // 2. Angular logic
        pivotHighs.add(currentPivot);
        if (pivotHighs.size() >= 4) {
          List<Pivot> last4 = pivotHighs.subList(pivotHighs.size() - 4, pivotHighs.size());
          ActiveLine angularLine = createRegressionLine(last4, SRCurrentType.RESISTANCE);
          if (angularLine != null && angularLine.slope.compareTo(BigDecimal.ZERO) > 0) {
            activeLines.add(angularLine);
          }
        }
      }

      if (isLow) {
        Pivot currentPivot = new Pivot(i, currentLow, bars.get(i).date());

        // 1. Horizontal logic
        boolean addedToExisting = false;
        for (ActiveLine line : activeLines) {
          if (line.filterReason == null && line.isHorizontal && line.type == SRCurrentType.SUPPORT) {
            BigDecimal avgPrice = line.intercept;
            BigDecimal diff = currentLow.subtract(avgPrice).abs();
            BigDecimal thresh = avgPrice.multiply(tolerance);
            if (diff.compareTo(thresh) <= 0) {
              line.touchPoints.add(new SRTouchPoint(currentPivot.date, avgPrice));
              line.importance = line.touchPoints.size();
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
              pts.add(new SRTouchPoint(past.date, avgPrice));
              pts.add(new SRTouchPoint(currentPivot.date, avgPrice));
              activeLines.add(
                  new ActiveLine(pts, SRCurrentType.SUPPORT, BigDecimal.ZERO, avgPrice, true));
              break;
            }
          }
        }

        // 2. Angular logic
        pivotLows.add(currentPivot);
        if (pivotLows.size() >= 4) {
          List<Pivot> last4 = pivotLows.subList(pivotLows.size() - 4, pivotLows.size());
          ActiveLine angularLine = createRegressionLine(last4, SRCurrentType.SUPPORT);
          if (angularLine != null && angularLine.slope.compareTo(BigDecimal.ZERO) < 0) {
            activeLines.add(angularLine);
          }
        }
      }

      // Check breaks
      checkBreaks(i, bars.get(i), activeLines);
    }

    // Check breaks for the remaining bars that weren't checked for pivots
    for (int i = Math.max(startIndex, bars.size() - window); i < bars.size(); i++) {
      checkBreaks(i, bars.get(i), activeLines);
    }

    for (ActiveLine al : activeLines) {
      if (al.filterReason == null) {
        if (al.isHorizontal) {
          if (al.touchPoints.size() < 3) {
            al.filterReason = SRFilterReason.TOUCHES_LT_3;
          }
        } else {
          if (al.touchPoints.size() < 4) {
            al.filterReason = SRFilterReason.TOUCHES_LT_4;
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
    BigDecimal proximityThresh = new BigDecimal("0.01"); // 1%
    int currentIndex = bars.size() - 1;
    BigDecimal currentClose = bars.get(currentIndex).close();

    for (ActiveLine line : validForProximity) {
      BigDecimal lineCurrentExpected =
          line.slope.multiply(new BigDecimal(currentIndex)).add(line.intercept);

      // 1. Circuit Breaker
      BigDecimal upperLimit = currentClose.multiply(new BigDecimal("1.2"));
      BigDecimal lowerLimit = currentClose.multiply(new BigDecimal("0.8"));
      if (lineCurrentExpected.compareTo(upperLimit) > 0
          || lineCurrentExpected.compareTo(lowerLimit) < 0) {
        line.filterReason = SRFilterReason.CIRCUIT_BREAKER_20_PCT;
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
        line.filterReason = SRFilterReason.PROXIMITY_1_PCT;
      } else {
        merged.add(line);
      }
    }

    // Explicitly set polarity based on relation to current price
    for (ActiveLine line : activeLines) {
      BigDecimal lineCurrentExpected =
          line.slope.multiply(new BigDecimal(currentIndex)).add(line.intercept);
      if (lineCurrentExpected.compareTo(currentClose) > 0) {
        line.type = SRCurrentType.RESISTANCE;
      } else {
        line.type = SRCurrentType.SUPPORT;
      }
    }

    return activeLines;
  }

  private void checkBreaks(int index, PriceBar bar, List<ActiveLine> activeLines) {
    BigDecimal close = bar.close();
    for (ActiveLine line : activeLines) {
      // Check if the line was formed in the future relative to current bar
      boolean isFuture = false;
      for (SRTouchPoint tp : line.touchPoints) {
        if (tp.getDate().isAfter(bar.date()) || tp.getDate().isEqual(bar.date())) {
          isFuture = true;
          break;
        }
      }
      if (isFuture) continue;

      BigDecimal expectedPrice = line.slope.multiply(new BigDecimal(index)).add(line.intercept);

      if (line.type == SRCurrentType.RESISTANCE && close.compareTo(expectedPrice) > 0) {
        line.breakCount++;
        if (line.breakCount > 2) {
          line.filterReason = SRFilterReason.BREAKS_GT_2;
        } else {
          line.type = SRCurrentType.SUPPORT; // Flip polarity
        }
      } else if (line.type == SRCurrentType.SUPPORT && close.compareTo(expectedPrice) < 0) {
        line.breakCount++;
        if (line.breakCount > 2) {
          line.filterReason = SRFilterReason.BREAKS_GT_2;
        } else {
          line.type = SRCurrentType.RESISTANCE; // Flip polarity
        }
      }
    }
  }

  private ActiveLine createRegressionLine(List<Pivot> pivots, SRCurrentType type) {
    if (pivots.size() < 3) return null;
    double sumX = 0;
    double sumY = 0;
    for (Pivot p : pivots) {
      sumX += p.index;
      sumY += p.price.doubleValue();
    }
    double meanX = sumX / pivots.size();
    double meanY = sumY / pivots.size();

    double num = 0;
    double den = 0;
    for (Pivot p : pivots) {
      num += (p.index - meanX) * (p.price.doubleValue() - meanY);
      den += (p.index - meanX) * (p.index - meanX);
    }
    if (den == 0) return null;

    double m = num / den;
    double b = meanY - m * meanX;

    BigDecimal slope = new BigDecimal(String.valueOf(m)).setScale(4, RoundingMode.HALF_UP);
    BigDecimal intercept = new BigDecimal(String.valueOf(b)).setScale(4, RoundingMode.HALF_UP);

    List<SRTouchPoint> touchPoints = new ArrayList<>();
    for (Pivot p : pivots) {
      touchPoints.add(new SRTouchPoint(p.date, p.price));
    }

    return new ActiveLine(touchPoints, type, slope, intercept, false);
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
