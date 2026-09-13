package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.dtos.ChartPatternMatch;
import com.alphaflow.engine.calculators.dtos.ExtremaPoint;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.persistence.entities.ChartPatternPivot;
import com.alphaflow.persistence.entities.DailyChartPattern;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyChartPattern;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.enums.ChartPatternStatus;
import com.alphaflow.persistence.enums.ChartPatternType;
import com.alphaflow.persistence.repositories.DailyChartPatternRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyChartPatternRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class ChartPatternCalculator {

  private static final MathContext MATH_CONTEXT = new MathContext(18, RoundingMode.HALF_UP);
  private static final int PRICE_SCALE = 4;
  private static final int MIN_BARS = 15;
  private static final int SWING_WINDOW = 2;
  private static final BigDecimal TWO = new BigDecimal("2");
  private static final BigDecimal THREE = new BigDecimal("3");
  private static final BigDecimal TOLERANCE_3_PERCENT = new BigDecimal("0.035");
  private static final BigDecimal TOLERANCE_4_PERCENT = new BigDecimal("0.040");
  private static final BigDecimal TOLERANCE_5_PERCENT = new BigDecimal("0.050");
  private static final BigDecimal MIN_DEPTH_RATIO = new BigDecimal("0.015");

  private final TickerRepository tickerRepository;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final DailyChartPatternRepository dailyChartPatternRepository;
  private final WeeklyChartPatternRepository weeklyChartPatternRepository;

  @Autowired @Lazy private ChartPatternCalculator selfProxy;

  public ChartPatternCalculator(
      TickerRepository tickerRepository,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailyChartPatternRepository dailyChartPatternRepository,
      WeeklyChartPatternRepository weeklyChartPatternRepository) {
    this.tickerRepository = tickerRepository;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailyChartPatternRepository = dailyChartPatternRepository;
    this.weeklyChartPatternRepository = weeklyChartPatternRepository;
  }

  public void computeChartPatterns() {
    log.info("Computing classical chart patterns...");
    List<Ticker> activeTickers = tickerRepository.findByIsActiveTrue();
    log.info("Found {} active tickers to process for chart patterns.", activeTickers.size());

    ChartPatternCalculator proxy = (selfProxy != null) ? selfProxy : this;

    for (Ticker activeTicker : activeTickers) {
      try {
        proxy.computeChartPatternsForTicker(activeTicker);
      } catch (Exception exception) {
        log.error(
            "Failed to compute chart patterns for ticker {}: {}",
            activeTicker.getTickerSymbol(),
            exception.getMessage(),
            exception);
      }
    }
    log.info("Chart patterns computation completed.");
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void computeChartPatternsForTicker(Ticker ticker) {
    List<PriceBar> dailyBars = loadBars(ticker, Timeframe.DAILY);
    List<PriceBar> weeklyBars = loadBars(ticker, Timeframe.WEEKLY);

    if (dailyBars.size() >= MIN_BARS) {
      log.info(
          "Ticker {}: Timeframe {} - Computing chart patterns...",
          ticker.getTickerSymbol(),
          Timeframe.DAILY);
      List<ChartPatternMatch> dailyMatches = detectAllPatterns(dailyBars);
      savePatterns(ticker, Timeframe.DAILY, dailyMatches);
    } else {
      log.debug(
          "Ticker {}: Insufficient daily bars ({}) for chart patterns.",
          ticker.getTickerSymbol(),
          dailyBars.size());
    }

    if (weeklyBars.size() >= MIN_BARS) {
      log.info(
          "Ticker {}: Timeframe {} - Computing chart patterns...",
          ticker.getTickerSymbol(),
          Timeframe.WEEKLY);
      List<ChartPatternMatch> weeklyMatches = detectAllPatterns(weeklyBars);
      savePatterns(ticker, Timeframe.WEEKLY, weeklyMatches);
    } else {
      log.debug(
          "Ticker {}: Insufficient weekly bars ({}) for chart patterns.",
          ticker.getTickerSymbol(),
          weeklyBars.size());
    }
  }

  private List<PriceBar> loadBars(Ticker ticker, Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
          .map(this::toPriceBar)
          .toList();
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
          .map(this::toPriceBar)
          .toList();
    } else {
      log.error("Unsupported timeframe for loading bars: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  private void savePatterns(
      Ticker ticker, Timeframe timeframe, List<ChartPatternMatch> patternMatches) {
    if (timeframe == Timeframe.DAILY) {
      for (ChartPatternMatch match : patternMatches) {
        Optional<DailyChartPattern> existingRecord =
            dailyChartPatternRepository.findByTickerAndPatternTypeAndStartDate(
                ticker, match.patternType(), match.startDate());
        if (existingRecord.isPresent()) {
          DailyChartPattern pattern = existingRecord.get();
          pattern.setStatus(match.status());
          pattern.setEndDate(match.endDate());
          pattern.setBreakoutDate(match.breakoutDate());
          pattern.setNecklinePrice(match.necklinePrice());
          pattern.setNecklineSlope(match.necklineSlope());
          pattern.setTargetPrice(match.targetPrice());
          pattern.setStopLossPrice(match.stopLossPrice());
          pattern.setInvalidationPrice(match.invalidationPrice());
          pattern.setPivotPoints(match.pivotPoints());
          dailyChartPatternRepository.save(pattern);
        } else {
          DailyChartPattern newPattern =
              DailyChartPattern.builder()
                  .ticker(ticker)
                  .patternType(match.patternType())
                  .sentiment(match.sentiment())
                  .status(match.status())
                  .startDate(match.startDate())
                  .endDate(match.endDate())
                  .breakoutDate(match.breakoutDate())
                  .necklineSlope(match.necklineSlope())
                  .necklinePrice(match.necklinePrice())
                  .targetPrice(match.targetPrice())
                  .stopLossPrice(match.stopLossPrice())
                  .invalidationPrice(match.invalidationPrice())
                  .pivotPoints(match.pivotPoints())
                  .build();
          dailyChartPatternRepository.save(newPattern);
        }
      }
    } else if (timeframe == Timeframe.WEEKLY) {
      for (ChartPatternMatch match : patternMatches) {
        Optional<WeeklyChartPattern> existingRecord =
            weeklyChartPatternRepository.findByTickerAndPatternTypeAndStartDate(
                ticker, match.patternType(), match.startDate());
        if (existingRecord.isPresent()) {
          WeeklyChartPattern pattern = existingRecord.get();
          pattern.setStatus(match.status());
          pattern.setEndDate(match.endDate());
          pattern.setBreakoutDate(match.breakoutDate());
          pattern.setNecklinePrice(match.necklinePrice());
          pattern.setNecklineSlope(match.necklineSlope());
          pattern.setTargetPrice(match.targetPrice());
          pattern.setStopLossPrice(match.stopLossPrice());
          pattern.setInvalidationPrice(match.invalidationPrice());
          pattern.setPivotPoints(match.pivotPoints());
          weeklyChartPatternRepository.save(pattern);
        } else {
          WeeklyChartPattern newPattern =
              WeeklyChartPattern.builder()
                  .ticker(ticker)
                  .patternType(match.patternType())
                  .sentiment(match.sentiment())
                  .status(match.status())
                  .startDate(match.startDate())
                  .endDate(match.endDate())
                  .breakoutDate(match.breakoutDate())
                  .necklineSlope(match.necklineSlope())
                  .necklinePrice(match.necklinePrice())
                  .targetPrice(match.targetPrice())
                  .stopLossPrice(match.stopLossPrice())
                  .invalidationPrice(match.invalidationPrice())
                  .pivotPoints(match.pivotPoints())
                  .build();
          weeklyChartPatternRepository.save(newPattern);
        }
      }
    } else {
      log.error("Unsupported timeframe for saving chart patterns: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  public List<ChartPatternMatch> detectAllPatterns(List<PriceBar> priceBars) {
    if (priceBars.size() < MIN_BARS) {
      return List.of();
    }
    List<ExtremaPoint> rawExtrema = identifySwingPoints(priceBars);
    List<ExtremaPoint> alternatingPivots = filterAlternatingExtrema(rawExtrema);
    if (alternatingPivots.size() < 3) {
      return List.of();
    }

    List<ChartPatternMatch> matches = new ArrayList<>();
    detectDoubleFormations(priceBars, alternatingPivots, matches);
    detectTripleFormations(priceBars, alternatingPivots, matches);
    detectHeadAndShoulders(priceBars, alternatingPivots, matches);
    detectTriangles(priceBars, alternatingPivots, matches);
    detectWedges(priceBars, alternatingPivots, matches);
    detectCupAndHandle(priceBars, alternatingPivots, matches);
    return matches;
  }

  public List<ExtremaPoint> identifySwingPoints(List<PriceBar> priceBars) {
    List<ExtremaPoint> extremaPoints = new ArrayList<>();
    for (int index = SWING_WINDOW; index < priceBars.size() - SWING_WINDOW; index++) {
      PriceBar currentBar = priceBars.get(index);
      boolean isSwingHigh = true;
      boolean isSwingLow = true;

      for (int step = 1; step <= SWING_WINDOW; step++) {
        PriceBar leftBar = priceBars.get(index - step);
        PriceBar rightBar = priceBars.get(index + step);
        if (currentBar.high().compareTo(leftBar.high()) < 0
            || currentBar.high().compareTo(rightBar.high()) < 0) {
          isSwingHigh = false;
        }
        if (currentBar.low().compareTo(leftBar.low()) > 0
            || currentBar.low().compareTo(rightBar.low()) > 0) {
          isSwingLow = false;
        }
      }

      if (isSwingHigh && !isSwingLow) {
        extremaPoints.add(new ExtremaPoint(index, currentBar.date(), currentBar.high(), true));
      } else if (isSwingLow && !isSwingHigh) {
        extremaPoints.add(new ExtremaPoint(index, currentBar.date(), currentBar.low(), false));
      }
    }
    return extremaPoints;
  }

  public List<ExtremaPoint> filterAlternatingExtrema(List<ExtremaPoint> rawExtrema) {
    if (rawExtrema.isEmpty()) {
      return List.of();
    }
    List<ExtremaPoint> filtered = new ArrayList<>();
    ExtremaPoint currentExtrema = rawExtrema.getFirst();

    for (int index = 1; index < rawExtrema.size(); index++) {
      ExtremaPoint nextExtrema = rawExtrema.get(index);
      if (currentExtrema.isHigh() == nextExtrema.isHigh()) {
        if (currentExtrema.isHigh()) {
          if (nextExtrema.price().compareTo(currentExtrema.price()) > 0) {
            currentExtrema = nextExtrema;
          }
        } else {
          if (nextExtrema.price().compareTo(currentExtrema.price()) < 0) {
            currentExtrema = nextExtrema;
          }
        }
      } else {
        filtered.add(currentExtrema);
        currentExtrema = nextExtrema;
      }
    }
    filtered.add(currentExtrema);
    return filtered;
  }

  private void detectDoubleFormations(
      List<PriceBar> priceBars, List<ExtremaPoint> pivots, List<ChartPatternMatch> matches) {
    for (int index = 0; index <= pivots.size() - 3; index++) {
      ExtremaPoint pivot1 = pivots.get(index);
      ExtremaPoint pivot2 = pivots.get(index + 1);
      ExtremaPoint pivot3 = pivots.get(index + 2);

      // Double Top: High, Low, High
      if (pivot1.isHigh() && !pivot2.isHigh() && pivot3.isHigh()) {
        BigDecimal diff = pivot1.price().subtract(pivot3.price()).abs();
        BigDecimal maxHigh = pivot1.price().max(pivot3.price());
        BigDecimal ratio = diff.divide(maxHigh, MATH_CONTEXT);

        BigDecimal depth = maxHigh.subtract(pivot2.price());
        BigDecimal depthRatio = depth.divide(maxHigh, MATH_CONTEXT);

        if (ratio.compareTo(TOLERANCE_3_PERCENT) <= 0
            && depthRatio.compareTo(MIN_DEPTH_RATIO) >= 0) {
          BigDecimal neckline = pivot2.price();
          BigDecimal target = neckline.subtract(depth).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal invalidation = maxHigh.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal stopLoss = pivot3.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);

          ChartPatternStatus status =
              evaluateStatus(priceBars, pivot3.index(), neckline, target, invalidation, false);

          LocalDate breakoutDate = findBreakoutDate(priceBars, pivot3.index(), neckline, false);

          List<ChartPatternPivot> patternPivots =
              List.of(
                  new ChartPatternPivot(pivot1.date(), pivot1.price(), "HIGH", "PEAK_1"),
                  new ChartPatternPivot(pivot2.date(), pivot2.price(), "LOW", "NECKLINE"),
                  new ChartPatternPivot(pivot3.date(), pivot3.price(), "HIGH", "PEAK_2"));

          matches.add(
              ChartPatternMatch.builder()
                  .patternType(ChartPatternType.DOUBLE_TOP)
                  .sentiment(ChartPatternType.DOUBLE_TOP.getSentiment())
                  .status(status)
                  .startDate(pivot1.date())
                  .endDate(pivot3.date())
                  .breakoutDate(breakoutDate)
                  .necklinePrice(neckline.setScale(PRICE_SCALE, RoundingMode.HALF_UP))
                  .targetPrice(target)
                  .stopLossPrice(stopLoss)
                  .invalidationPrice(invalidation)
                  .pivotPoints(patternPivots)
                  .build());
        }
      }

      // Double Bottom: Low, High, Low
      if (!pivot1.isHigh() && pivot2.isHigh() && !pivot3.isHigh()) {
        BigDecimal diff = pivot1.price().subtract(pivot3.price()).abs();
        BigDecimal maxLow = pivot1.price().max(pivot3.price());
        BigDecimal ratio = diff.divide(maxLow, MATH_CONTEXT);

        BigDecimal height = pivot2.price().subtract(pivot1.price().min(pivot3.price()));
        BigDecimal heightRatio = height.divide(pivot2.price(), MATH_CONTEXT);

        if (ratio.compareTo(TOLERANCE_3_PERCENT) <= 0
            && heightRatio.compareTo(MIN_DEPTH_RATIO) >= 0) {
          BigDecimal neckline = pivot2.price();
          BigDecimal target = neckline.add(height).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal invalidation =
              pivot1.price().min(pivot3.price()).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal stopLoss = pivot3.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);

          ChartPatternStatus status =
              evaluateStatus(priceBars, pivot3.index(), neckline, target, invalidation, true);

          LocalDate breakoutDate = findBreakoutDate(priceBars, pivot3.index(), neckline, true);

          List<ChartPatternPivot> patternPivots =
              List.of(
                  new ChartPatternPivot(pivot1.date(), pivot1.price(), "LOW", "TROUGH_1"),
                  new ChartPatternPivot(pivot2.date(), pivot2.price(), "HIGH", "NECKLINE"),
                  new ChartPatternPivot(pivot3.date(), pivot3.price(), "LOW", "TROUGH_2"));

          matches.add(
              ChartPatternMatch.builder()
                  .patternType(ChartPatternType.DOUBLE_BOTTOM)
                  .sentiment(ChartPatternType.DOUBLE_BOTTOM.getSentiment())
                  .status(status)
                  .startDate(pivot1.date())
                  .endDate(pivot3.date())
                  .breakoutDate(breakoutDate)
                  .necklinePrice(neckline.setScale(PRICE_SCALE, RoundingMode.HALF_UP))
                  .targetPrice(target)
                  .stopLossPrice(stopLoss)
                  .invalidationPrice(invalidation)
                  .pivotPoints(patternPivots)
                  .build());
        }
      }
    }
  }

  private void detectTripleFormations(
      List<PriceBar> priceBars, List<ExtremaPoint> pivots, List<ChartPatternMatch> matches) {
    for (int index = 0; index <= pivots.size() - 5; index++) {
      ExtremaPoint p1 = pivots.get(index);
      ExtremaPoint p2 = pivots.get(index + 1);
      ExtremaPoint p3 = pivots.get(index + 2);
      ExtremaPoint p4 = pivots.get(index + 3);
      ExtremaPoint p5 = pivots.get(index + 4);

      // Triple Top: High, Low, High, Low, High
      if (p1.isHigh() && !p2.isHigh() && p3.isHigh() && !p4.isHigh() && p5.isHigh()) {
        BigDecimal maxHigh = p1.price().max(p3.price()).max(p5.price());
        BigDecimal minHigh = p1.price().min(p3.price()).min(p5.price());
        BigDecimal spread = maxHigh.subtract(minHigh).divide(maxHigh, MATH_CONTEXT);

        BigDecimal neckline = p2.price().min(p4.price());
        BigDecimal height = maxHigh.subtract(neckline);

        if (spread.compareTo(TOLERANCE_4_PERCENT) <= 0
            && height.divide(maxHigh, MATH_CONTEXT).compareTo(MIN_DEPTH_RATIO) >= 0) {
          BigDecimal target = neckline.subtract(height).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal invalidation = maxHigh.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal stopLoss = p5.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);

          ChartPatternStatus status =
              evaluateStatus(priceBars, p5.index(), neckline, target, invalidation, false);
          LocalDate breakoutDate = findBreakoutDate(priceBars, p5.index(), neckline, false);

          List<ChartPatternPivot> patternPivots =
              List.of(
                  new ChartPatternPivot(p1.date(), p1.price(), "HIGH", "PEAK_1"),
                  new ChartPatternPivot(p2.date(), p2.price(), "LOW", "TROUGH_1"),
                  new ChartPatternPivot(p3.date(), p3.price(), "HIGH", "PEAK_2"),
                  new ChartPatternPivot(p4.date(), p4.price(), "LOW", "TROUGH_2"),
                  new ChartPatternPivot(p5.date(), p5.price(), "HIGH", "PEAK_3"));

          matches.add(
              ChartPatternMatch.builder()
                  .patternType(ChartPatternType.TRIPLE_TOP)
                  .sentiment(ChartPatternType.TRIPLE_TOP.getSentiment())
                  .status(status)
                  .startDate(p1.date())
                  .endDate(p5.date())
                  .breakoutDate(breakoutDate)
                  .necklinePrice(neckline.setScale(PRICE_SCALE, RoundingMode.HALF_UP))
                  .targetPrice(target)
                  .stopLossPrice(stopLoss)
                  .invalidationPrice(invalidation)
                  .pivotPoints(patternPivots)
                  .build());
        }
      }

      // Triple Bottom: Low, High, Low, High, Low
      if (!p1.isHigh() && p2.isHigh() && !p3.isHigh() && p4.isHigh() && !p5.isHigh()) {
        BigDecimal maxLow = p1.price().max(p3.price()).max(p5.price());
        BigDecimal minLow = p1.price().min(p3.price()).min(p5.price());
        BigDecimal spread = maxLow.subtract(minLow).divide(maxLow, MATH_CONTEXT);

        BigDecimal neckline = p2.price().max(p4.price());
        BigDecimal height = neckline.subtract(minLow);

        if (spread.compareTo(TOLERANCE_4_PERCENT) <= 0
            && height.divide(neckline, MATH_CONTEXT).compareTo(MIN_DEPTH_RATIO) >= 0) {
          BigDecimal target = neckline.add(height).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal invalidation = minLow.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal stopLoss = p5.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);

          ChartPatternStatus status =
              evaluateStatus(priceBars, p5.index(), neckline, target, invalidation, true);
          LocalDate breakoutDate = findBreakoutDate(priceBars, p5.index(), neckline, true);

          List<ChartPatternPivot> patternPivots =
              List.of(
                  new ChartPatternPivot(p1.date(), p1.price(), "LOW", "TROUGH_1"),
                  new ChartPatternPivot(p2.date(), p2.price(), "HIGH", "PEAK_1"),
                  new ChartPatternPivot(p3.date(), p3.price(), "LOW", "TROUGH_2"),
                  new ChartPatternPivot(p4.date(), p4.price(), "HIGH", "PEAK_2"),
                  new ChartPatternPivot(p5.date(), p5.price(), "LOW", "TROUGH_3"));

          matches.add(
              ChartPatternMatch.builder()
                  .patternType(ChartPatternType.TRIPLE_BOTTOM)
                  .sentiment(ChartPatternType.TRIPLE_BOTTOM.getSentiment())
                  .status(status)
                  .startDate(p1.date())
                  .endDate(p5.date())
                  .breakoutDate(breakoutDate)
                  .necklinePrice(neckline.setScale(PRICE_SCALE, RoundingMode.HALF_UP))
                  .targetPrice(target)
                  .stopLossPrice(stopLoss)
                  .invalidationPrice(invalidation)
                  .pivotPoints(patternPivots)
                  .build());
        }
      }
    }
  }

  private void detectHeadAndShoulders(
      List<PriceBar> priceBars, List<ExtremaPoint> pivots, List<ChartPatternMatch> matches) {
    for (int index = 0; index <= pivots.size() - 5; index++) {
      ExtremaPoint p1 = pivots.get(index);
      ExtremaPoint p2 = pivots.get(index + 1);
      ExtremaPoint p3 = pivots.get(index + 2);
      ExtremaPoint p4 = pivots.get(index + 3);
      ExtremaPoint p5 = pivots.get(index + 4);

      // Standard Head and Shoulders: High (LS), Low (T1), High (Head), Low (T2), High (RS)
      if (p1.isHigh() && !p2.isHigh() && p3.isHigh() && !p4.isHigh() && p5.isHigh()) {
        boolean headHigher =
            p3.price().compareTo(p1.price()) > 0 && p3.price().compareTo(p5.price()) > 0;
        BigDecimal shoulderDiff = p1.price().subtract(p5.price()).abs();
        BigDecimal shoulderRatio = shoulderDiff.divide(p1.price().max(p5.price()), MATH_CONTEXT);

        if (headHigher && shoulderRatio.compareTo(TOLERANCE_5_PERCENT) <= 0) {
          BigDecimal neckline = p2.price().min(p4.price());
          BigDecimal height = p3.price().subtract(neckline);
          BigDecimal target = neckline.subtract(height).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal invalidation = p3.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal stopLoss = p5.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);

          ChartPatternStatus status =
              evaluateStatus(priceBars, p5.index(), neckline, target, invalidation, false);
          LocalDate breakoutDate = findBreakoutDate(priceBars, p5.index(), neckline, false);

          List<ChartPatternPivot> patternPivots =
              List.of(
                  new ChartPatternPivot(p1.date(), p1.price(), "HIGH", "LEFT_SHOULDER"),
                  new ChartPatternPivot(p2.date(), p2.price(), "LOW", "TROUGH_1"),
                  new ChartPatternPivot(p3.date(), p3.price(), "HIGH", "HEAD"),
                  new ChartPatternPivot(p4.date(), p4.price(), "LOW", "TROUGH_2"),
                  new ChartPatternPivot(p5.date(), p5.price(), "HIGH", "RIGHT_SHOULDER"));

          matches.add(
              ChartPatternMatch.builder()
                  .patternType(ChartPatternType.HEAD_AND_SHOULDERS)
                  .sentiment(ChartPatternType.HEAD_AND_SHOULDERS.getSentiment())
                  .status(status)
                  .startDate(p1.date())
                  .endDate(p5.date())
                  .breakoutDate(breakoutDate)
                  .necklinePrice(neckline.setScale(PRICE_SCALE, RoundingMode.HALF_UP))
                  .targetPrice(target)
                  .stopLossPrice(stopLoss)
                  .invalidationPrice(invalidation)
                  .pivotPoints(patternPivots)
                  .build());
        }
      }

      // Inverse Head and Shoulders: Low (LS), High (P1), Low (Head), High (P2), Low (RS)
      if (!p1.isHigh() && p2.isHigh() && !p3.isHigh() && p4.isHigh() && !p5.isHigh()) {
        boolean headLower =
            p3.price().compareTo(p1.price()) < 0 && p3.price().compareTo(p5.price()) < 0;
        BigDecimal shoulderDiff = p1.price().subtract(p5.price()).abs();
        BigDecimal shoulderRatio = shoulderDiff.divide(p1.price().max(p5.price()), MATH_CONTEXT);

        if (headLower && shoulderRatio.compareTo(TOLERANCE_5_PERCENT) <= 0) {
          BigDecimal neckline = p2.price().max(p4.price());
          BigDecimal height = neckline.subtract(p3.price());
          BigDecimal target = neckline.add(height).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal invalidation = p3.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal stopLoss = p5.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);

          ChartPatternStatus status =
              evaluateStatus(priceBars, p5.index(), neckline, target, invalidation, true);
          LocalDate breakoutDate = findBreakoutDate(priceBars, p5.index(), neckline, true);

          List<ChartPatternPivot> patternPivots =
              List.of(
                  new ChartPatternPivot(p1.date(), p1.price(), "LOW", "LEFT_SHOULDER"),
                  new ChartPatternPivot(p2.date(), p2.price(), "HIGH", "PEAK_1"),
                  new ChartPatternPivot(p3.date(), p3.price(), "LOW", "HEAD"),
                  new ChartPatternPivot(p4.date(), p4.price(), "HIGH", "PEAK_2"),
                  new ChartPatternPivot(p5.date(), p5.price(), "LOW", "RIGHT_SHOULDER"));

          matches.add(
              ChartPatternMatch.builder()
                  .patternType(ChartPatternType.INVERSE_HEAD_AND_SHOULDERS)
                  .sentiment(ChartPatternType.INVERSE_HEAD_AND_SHOULDERS.getSentiment())
                  .status(status)
                  .startDate(p1.date())
                  .endDate(p5.date())
                  .breakoutDate(breakoutDate)
                  .necklinePrice(neckline.setScale(PRICE_SCALE, RoundingMode.HALF_UP))
                  .targetPrice(target)
                  .stopLossPrice(stopLoss)
                  .invalidationPrice(invalidation)
                  .pivotPoints(patternPivots)
                  .build());
        }
      }
    }
  }

  private void detectTriangles(
      List<PriceBar> priceBars, List<ExtremaPoint> pivots, List<ChartPatternMatch> matches) {
    for (int index = 0; index <= pivots.size() - 4; index++) {
      ExtremaPoint p1 = pivots.get(index);
      ExtremaPoint p2 = pivots.get(index + 1);
      ExtremaPoint p3 = pivots.get(index + 2);
      ExtremaPoint p4 = pivots.get(index + 3);

      // Starting with High: p1=H1, p2=L1, p3=H2, p4=L2
      if (p1.isHigh() && !p2.isHigh() && p3.isHigh() && !p4.isHigh()) {
        BigDecimal highDiff = p1.price().subtract(p3.price()).abs();
        BigDecimal highSpread = highDiff.divide(p1.price().max(p3.price()), MATH_CONTEXT);

        // Ascending Triangle: H1 ~ H2 (flat resistance), L2 > L1 (higher low)
        if (highSpread.compareTo(TOLERANCE_3_PERCENT) <= 0
            && p4.price().compareTo(p2.price()) > 0) {
          BigDecimal resistance = p1.price().max(p3.price());
          BigDecimal height = resistance.subtract(p2.price());
          BigDecimal target = resistance.add(height).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal invalidation = p2.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal stopLoss = p4.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);

          ChartPatternStatus status =
              evaluateStatus(priceBars, p4.index(), resistance, target, invalidation, true);
          LocalDate breakoutDate = findBreakoutDate(priceBars, p4.index(), resistance, true);

          List<ChartPatternPivot> patternPivots =
              List.of(
                  new ChartPatternPivot(p1.date(), p1.price(), "HIGH", "RESISTANCE_1"),
                  new ChartPatternPivot(p2.date(), p2.price(), "LOW", "SUPPORT_1"),
                  new ChartPatternPivot(p3.date(), p3.price(), "HIGH", "RESISTANCE_2"),
                  new ChartPatternPivot(p4.date(), p4.price(), "LOW", "SUPPORT_2"));

          matches.add(
              ChartPatternMatch.builder()
                  .patternType(ChartPatternType.ASCENDING_TRIANGLE)
                  .sentiment(ChartPatternType.ASCENDING_TRIANGLE.getSentiment())
                  .status(status)
                  .startDate(p1.date())
                  .endDate(p4.date())
                  .breakoutDate(breakoutDate)
                  .necklinePrice(resistance.setScale(PRICE_SCALE, RoundingMode.HALF_UP))
                  .targetPrice(target)
                  .stopLossPrice(stopLoss)
                  .invalidationPrice(invalidation)
                  .pivotPoints(patternPivots)
                  .build());
        }

        // Symmetrical Triangle: H2 < H1 (lower high), L2 > L1 (higher low)
        if (p3.price().compareTo(p1.price()) < 0 && p4.price().compareTo(p2.price()) > 0) {
          BigDecimal height = p1.price().subtract(p2.price());
          BigDecimal trigger = p3.price();
          BigDecimal target = trigger.add(height).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal invalidation = p2.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal stopLoss = p4.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);

          ChartPatternStatus status =
              evaluateStatus(priceBars, p4.index(), trigger, target, invalidation, true);
          LocalDate breakoutDate = findBreakoutDate(priceBars, p4.index(), trigger, true);

          List<ChartPatternPivot> patternPivots =
              List.of(
                  new ChartPatternPivot(p1.date(), p1.price(), "HIGH", "PEAK_1"),
                  new ChartPatternPivot(p2.date(), p2.price(), "LOW", "TROUGH_1"),
                  new ChartPatternPivot(p3.date(), p3.price(), "HIGH", "PEAK_2"),
                  new ChartPatternPivot(p4.date(), p4.price(), "LOW", "TROUGH_2"));

          matches.add(
              ChartPatternMatch.builder()
                  .patternType(ChartPatternType.SYMMETRICAL_TRIANGLE)
                  .sentiment(ChartPatternType.SYMMETRICAL_TRIANGLE.getSentiment())
                  .status(status)
                  .startDate(p1.date())
                  .endDate(p4.date())
                  .breakoutDate(breakoutDate)
                  .necklinePrice(trigger.setScale(PRICE_SCALE, RoundingMode.HALF_UP))
                  .targetPrice(target)
                  .stopLossPrice(stopLoss)
                  .invalidationPrice(invalidation)
                  .pivotPoints(patternPivots)
                  .build());
        }
      }

      // Starting with Low: p1=L1, p2=H1, p3=L2, p4=H2
      if (!p1.isHigh() && p2.isHigh() && !p3.isHigh() && p4.isHigh()) {
        BigDecimal lowDiff = p1.price().subtract(p3.price()).abs();
        BigDecimal lowSpread = lowDiff.divide(p1.price().max(p3.price()), MATH_CONTEXT);

        // Descending Triangle: L1 ~ L2 (flat support), H2 < H1 (lower high)
        if (lowSpread.compareTo(TOLERANCE_3_PERCENT) <= 0 && p4.price().compareTo(p2.price()) < 0) {
          BigDecimal support = p1.price().min(p3.price());
          BigDecimal height = p2.price().subtract(support);
          BigDecimal target = support.subtract(height).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal invalidation = p2.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal stopLoss = p4.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);

          ChartPatternStatus status =
              evaluateStatus(priceBars, p4.index(), support, target, invalidation, false);
          LocalDate breakoutDate = findBreakoutDate(priceBars, p4.index(), support, false);

          List<ChartPatternPivot> patternPivots =
              List.of(
                  new ChartPatternPivot(p1.date(), p1.price(), "LOW", "SUPPORT_1"),
                  new ChartPatternPivot(p2.date(), p2.price(), "HIGH", "RESISTANCE_1"),
                  new ChartPatternPivot(p3.date(), p3.price(), "LOW", "SUPPORT_2"),
                  new ChartPatternPivot(p4.date(), p4.price(), "HIGH", "RESISTANCE_2"));

          matches.add(
              ChartPatternMatch.builder()
                  .patternType(ChartPatternType.DESCENDING_TRIANGLE)
                  .sentiment(ChartPatternType.DESCENDING_TRIANGLE.getSentiment())
                  .status(status)
                  .startDate(p1.date())
                  .endDate(p4.date())
                  .breakoutDate(breakoutDate)
                  .necklinePrice(support.setScale(PRICE_SCALE, RoundingMode.HALF_UP))
                  .targetPrice(target)
                  .stopLossPrice(stopLoss)
                  .invalidationPrice(invalidation)
                  .pivotPoints(patternPivots)
                  .build());
        }
      }
    }
  }

  private void detectWedges(
      List<PriceBar> priceBars, List<ExtremaPoint> pivots, List<ChartPatternMatch> matches) {
    for (int index = 0; index <= pivots.size() - 4; index++) {
      ExtremaPoint p1 = pivots.get(index);
      ExtremaPoint p2 = pivots.get(index + 1);
      ExtremaPoint p3 = pivots.get(index + 2);
      ExtremaPoint p4 = pivots.get(index + 3);

      // Rising Wedge: Low1, High1, Low2, High2 with High2 > High1 and Low2 > Low1 (converging
      // upward)
      if (!p1.isHigh() && p2.isHigh() && !p3.isHigh() && p4.isHigh()) {
        if (p4.price().compareTo(p2.price()) > 0 && p3.price().compareTo(p1.price()) > 0) {
          BigDecimal highDiff = p4.price().subtract(p2.price());
          BigDecimal lowDiff = p3.price().subtract(p1.price());

          // Slopes converging (lows rising faster than highs)
          if (lowDiff.compareTo(highDiff) > 0) {
            BigDecimal trigger = p3.price();
            BigDecimal target = p1.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            BigDecimal invalidation = p4.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            BigDecimal stopLoss = p4.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);

            ChartPatternStatus status =
                evaluateStatus(priceBars, p4.index(), trigger, target, invalidation, false);
            LocalDate breakoutDate = findBreakoutDate(priceBars, p4.index(), trigger, false);

            List<ChartPatternPivot> patternPivots =
                List.of(
                    new ChartPatternPivot(p1.date(), p1.price(), "LOW", "SUPPORT_1"),
                    new ChartPatternPivot(p2.date(), p2.price(), "HIGH", "RESISTANCE_1"),
                    new ChartPatternPivot(p3.date(), p3.price(), "LOW", "SUPPORT_2"),
                    new ChartPatternPivot(p4.date(), p4.price(), "HIGH", "RESISTANCE_2"));

            matches.add(
                ChartPatternMatch.builder()
                    .patternType(ChartPatternType.RISING_WEDGE)
                    .sentiment(ChartPatternType.RISING_WEDGE.getSentiment())
                    .status(status)
                    .startDate(p1.date())
                    .endDate(p4.date())
                    .breakoutDate(breakoutDate)
                    .necklinePrice(trigger.setScale(PRICE_SCALE, RoundingMode.HALF_UP))
                    .targetPrice(target)
                    .stopLossPrice(stopLoss)
                    .invalidationPrice(invalidation)
                    .pivotPoints(patternPivots)
                    .build());
          }
        }
      }

      // Falling Wedge: High1, Low1, High2, Low2 with High2 < High1 and Low2 < Low1 (converging
      // downward)
      if (p1.isHigh() && !p2.isHigh() && p3.isHigh() && !p4.isHigh()) {
        if (p3.price().compareTo(p1.price()) < 0 && p4.price().compareTo(p2.price()) < 0) {
          BigDecimal highDiff = p1.price().subtract(p3.price());
          BigDecimal lowDiff = p2.price().subtract(p4.price());

          // Highs falling faster than lows
          if (highDiff.compareTo(lowDiff) > 0) {
            BigDecimal trigger = p3.price();
            BigDecimal target = p1.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            BigDecimal invalidation = p4.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            BigDecimal stopLoss = p4.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);

            ChartPatternStatus status =
                evaluateStatus(priceBars, p4.index(), trigger, target, invalidation, true);
            LocalDate breakoutDate = findBreakoutDate(priceBars, p4.index(), trigger, true);

            List<ChartPatternPivot> patternPivots =
                List.of(
                    new ChartPatternPivot(p1.date(), p1.price(), "HIGH", "RESISTANCE_1"),
                    new ChartPatternPivot(p2.date(), p2.price(), "LOW", "SUPPORT_1"),
                    new ChartPatternPivot(p3.date(), p3.price(), "HIGH", "RESISTANCE_2"),
                    new ChartPatternPivot(p4.date(), p4.price(), "LOW", "SUPPORT_2"));

            matches.add(
                ChartPatternMatch.builder()
                    .patternType(ChartPatternType.FALLING_WEDGE)
                    .sentiment(ChartPatternType.FALLING_WEDGE.getSentiment())
                    .status(status)
                    .startDate(p1.date())
                    .endDate(p4.date())
                    .breakoutDate(breakoutDate)
                    .necklinePrice(trigger.setScale(PRICE_SCALE, RoundingMode.HALF_UP))
                    .targetPrice(target)
                    .stopLossPrice(stopLoss)
                    .invalidationPrice(invalidation)
                    .pivotPoints(patternPivots)
                    .build());
          }
        }
      }
    }
  }

  private void detectCupAndHandle(
      List<PriceBar> priceBars, List<ExtremaPoint> pivots, List<ChartPatternMatch> matches) {
    for (int index = 0; index <= pivots.size() - 4; index++) {
      ExtremaPoint p1 = pivots.get(index);
      ExtremaPoint p2 = pivots.get(index + 1);
      ExtremaPoint p3 = pivots.get(index + 2);
      ExtremaPoint p4 = pivots.get(index + 3);

      // p1=High (Left Rim), p2=Low (Cup Bottom), p3=High (Right Rim), p4=Low (Handle Trough)
      if (p1.isHigh() && !p2.isHigh() && p3.isHigh() && !p4.isHigh()) {
        BigDecimal rimDiff = p1.price().subtract(p3.price()).abs();
        BigDecimal rimRatio = rimDiff.divide(p1.price().max(p3.price()), MATH_CONTEXT);

        BigDecimal cupDepth = p1.price().max(p3.price()).subtract(p2.price());
        BigDecimal handlePullback = p3.price().subtract(p4.price());

        // Rim tops close, cup has depth, handle is shallow (less than half cup depth)
        if (rimRatio.compareTo(TOLERANCE_4_PERCENT) <= 0
            && cupDepth.divide(p1.price(), MATH_CONTEXT).compareTo(new BigDecimal("0.04")) >= 0
            && p4.price().compareTo(p2.price()) > 0
            && handlePullback.compareTo(cupDepth.divide(TWO, MATH_CONTEXT)) <= 0) {
          BigDecimal resistance = p3.price();
          BigDecimal target = resistance.add(cupDepth).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal invalidation = p4.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
          BigDecimal stopLoss = p4.price().setScale(PRICE_SCALE, RoundingMode.HALF_UP);

          ChartPatternStatus status =
              evaluateStatus(priceBars, p4.index(), resistance, target, invalidation, true);
          LocalDate breakoutDate = findBreakoutDate(priceBars, p4.index(), resistance, true);

          List<ChartPatternPivot> patternPivots =
              List.of(
                  new ChartPatternPivot(p1.date(), p1.price(), "HIGH", "LEFT_RIM"),
                  new ChartPatternPivot(p2.date(), p2.price(), "LOW", "CUP_BOTTOM"),
                  new ChartPatternPivot(p3.date(), p3.price(), "HIGH", "RIGHT_RIM"),
                  new ChartPatternPivot(p4.date(), p4.price(), "LOW", "HANDLE_TROUGH"));

          matches.add(
              ChartPatternMatch.builder()
                  .patternType(ChartPatternType.CUP_AND_HANDLE)
                  .sentiment(ChartPatternType.CUP_AND_HANDLE.getSentiment())
                  .status(status)
                  .startDate(p1.date())
                  .endDate(p4.date())
                  .breakoutDate(breakoutDate)
                  .necklinePrice(resistance.setScale(PRICE_SCALE, RoundingMode.HALF_UP))
                  .targetPrice(target)
                  .stopLossPrice(stopLoss)
                  .invalidationPrice(invalidation)
                  .pivotPoints(patternPivots)
                  .build());
        }
      }
    }
  }

  private ChartPatternStatus evaluateStatus(
      List<PriceBar> priceBars,
      int lastPivotIndex,
      BigDecimal triggerPrice,
      BigDecimal targetPrice,
      BigDecimal invalidationPrice,
      boolean isBullish) {
    if (lastPivotIndex >= priceBars.size() - 1) {
      return ChartPatternStatus.IN_PROGRESS;
    }

    boolean brokenOut = false;
    for (int index = lastPivotIndex + 1; index < priceBars.size(); index++) {
      PriceBar bar = priceBars.get(index);
      if (isBullish) {
        if (!brokenOut) {
          if (bar.close().compareTo(triggerPrice) > 0) {
            brokenOut = true;
          } else if (bar.close().compareTo(invalidationPrice) < 0) {
            return ChartPatternStatus.INVALIDATED;
          }
        }
        if (brokenOut) {
          if (bar.high().compareTo(targetPrice) >= 0) {
            return ChartPatternStatus.TARGET_REACHED;
          }
          if (bar.close().compareTo(invalidationPrice) < 0) {
            return ChartPatternStatus.INVALIDATED;
          }
        }
      } else {
        if (!brokenOut) {
          if (bar.close().compareTo(triggerPrice) < 0) {
            brokenOut = true;
          } else if (bar.close().compareTo(invalidationPrice) > 0) {
            return ChartPatternStatus.INVALIDATED;
          }
        }
        if (brokenOut) {
          if (bar.low().compareTo(targetPrice) <= 0) {
            return ChartPatternStatus.TARGET_REACHED;
          }
          if (bar.close().compareTo(invalidationPrice) > 0) {
            return ChartPatternStatus.INVALIDATED;
          }
        }
      }
    }

    if (brokenOut) {
      return ChartPatternStatus.COMPLETED;
    } else {
      return ChartPatternStatus.IN_PROGRESS;
    }
  }

  private LocalDate findBreakoutDate(
      List<PriceBar> priceBars, int lastPivotIndex, BigDecimal triggerPrice, boolean isBullish) {
    for (int index = lastPivotIndex + 1; index < priceBars.size(); index++) {
      PriceBar bar = priceBars.get(index);
      if (isBullish && bar.close().compareTo(triggerPrice) > 0) {
        return bar.date();
      } else if (!isBullish && bar.close().compareTo(triggerPrice) < 0) {
        return bar.date();
      }
    }
    return null;
  }

  private PriceBar toPriceBar(DailyPrice dailyPrice) {
    return new PriceBar(
        dailyPrice.getPriceDate(),
        dailyPrice.getPriceOpen(),
        dailyPrice.getPriceHigh(),
        dailyPrice.getPriceLow(),
        dailyPrice.getPriceClose(),
        dailyPrice.getVolume());
  }

  private PriceBar toPriceBar(WeeklyPrice weeklyPrice) {
    return new PriceBar(
        weeklyPrice.getPriceDate(),
        weeklyPrice.getPriceOpen(),
        weeklyPrice.getPriceHigh(),
        weeklyPrice.getPriceLow(),
        weeklyPrice.getPriceClose(),
        weeklyPrice.getVolume());
  }
}
