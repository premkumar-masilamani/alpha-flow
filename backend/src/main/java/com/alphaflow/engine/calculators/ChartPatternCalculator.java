package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.dtos.ChartPatternMatch;
import com.alphaflow.engine.configs.ChartPatternConfig;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import com.alphaflow.engine.pipeline.PipelineJob;
import com.alphaflow.engine.structure.DowAtrSwingDetector;
import com.alphaflow.engine.structure.DowTrendClassifier;
import com.alphaflow.engine.structure.dtos.SwingPoint;
import com.alphaflow.engine.structure.enums.DowTrend;
import com.alphaflow.engine.structure.enums.SwingType;
import com.alphaflow.persistence.entities.ChartPatternPivot;
import com.alphaflow.persistence.entities.DailyChartPattern;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyChartPattern;
import com.alphaflow.persistence.enums.ChartPatternStatus;
import com.alphaflow.persistence.enums.ChartPatternType;
import com.alphaflow.persistence.repositories.DailyChartPatternRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyChartPatternRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes and persists classical chart pattern snapshots across daily and weekly timeframes using
 * confirmed Dow-ATR swing pivots and Dow Theory prior-trend classification.
 */
@Component
@Slf4j
public class ChartPatternCalculator implements PipelineJob {

  public static final String JOB_NAME = "CALCULATE_CHART_PATTERN";

  private static final int MIN_PIVOTS_FOR_PATTERNS = 3;

  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final DailyChartPatternRepository dailyChartPatternRepository;
  private final WeeklyChartPatternRepository weeklyChartPatternRepository;
  private final TickerRepository tickerRepository;
  private final DowAtrSwingDetector dowAtrSwingDetector;
  private final DowTrendClassifier dowTrendClassifier;

  @Autowired @Lazy private ChartPatternCalculator selfProxy;

  public ChartPatternCalculator(
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailyChartPatternRepository dailyChartPatternRepository,
      WeeklyChartPatternRepository weeklyChartPatternRepository,
      TickerRepository tickerRepository,
      DowAtrSwingDetector dowAtrSwingDetector,
      DowTrendClassifier dowTrendClassifier) {
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailyChartPatternRepository = dailyChartPatternRepository;
    this.weeklyChartPatternRepository = weeklyChartPatternRepository;
    this.tickerRepository = tickerRepository;
    this.dowAtrSwingDetector = dowAtrSwingDetector;
    this.dowTrendClassifier = dowTrendClassifier;
  }

  @Override
  public String getJobName() {
    return JOB_NAME;
  }

  @Override
  public boolean shouldRun(Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyChartPatternRepository.hasPendingDailyChartPatterns();
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyChartPatternRepository.hasPendingWeeklyChartPatterns();
    } else {
      log.error("Unsupported timeframe for chart pattern shouldRun: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  @Override
  public void execute(Timeframe timeframe) {
    if (timeframe != Timeframe.DAILY && timeframe != Timeframe.WEEKLY) {
      log.error("Unsupported timeframe for chart pattern computation: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
    log.info("Computing {} classical chart patterns...", timeframe);
    List<Ticker> activeTickers = tickerRepository.findByIsActiveTrue();
    log.info(
        "Found {} active tickers to process for {} chart patterns.",
        activeTickers.size(),
        timeframe);

    Map<Ticker, LocalDate> latestWeeklyDates =
        (timeframe == Timeframe.WEEKLY)
            ? weeklyChartPatternRepository.findLatestPriceDatesForActiveTickers()
            : Collections.emptyMap();

    ChartPatternCalculator proxy = (selfProxy != null) ? selfProxy : this;

    for (Ticker ticker : activeTickers) {
      try {
        LocalDate latestCalculatedDate = latestWeeklyDates.get(ticker);
        proxy.computeChartPatternsForTicker(ticker, timeframe, latestCalculatedDate);
      } catch (Exception exception) {
        log.error(
            "Failed to compute {} chart patterns for ticker {}: {}",
            timeframe,
            ticker.getTickerSymbol(),
            exception.getMessage(),
            exception);
      }
    }
    log.info("{} chart patterns computation completed.", timeframe);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void computeChartPatternsForTicker(
      Ticker ticker, Timeframe timeframe, LocalDate latestCalculatedDate) {
    List<PriceBar> bars = loadBars(ticker, timeframe);
    if (bars.size() >= ChartPatternConfig.MIN_BARS) {
      LocalDate priceDate = bars.getLast().date();

      if (timeframe == Timeframe.DAILY
          && dailyChartPatternRepository.existsByTickerAndPriceDate(ticker, priceDate)) {
        log.debug(
            "Ticker {}: DAILY chart patterns already computed for {}; skipping.",
            ticker.getTickerSymbol(),
            priceDate);
      } else {
        LocalDate cutoffDate =
            priceDate.minusDays(ChartPatternConfig.getRetentionDaysForTimeframe(timeframe));
        deleteOlderPatterns(ticker, cutoffDate, timeframe);

        if (timeframe == Timeframe.WEEKLY
            && latestCalculatedDate != null
            && !latestCalculatedDate.isBefore(priceDate)) {
          log.debug(
              "Refreshing in-progress weekly chart patterns for ticker {} on {}.",
              ticker.getTickerSymbol(),
              priceDate);
          weeklyChartPatternRepository.deleteByTickerAndPriceDate(ticker, priceDate);
        }

        List<ChartPatternMatch> matches = detectAllPatterns(bars);
        if (!matches.isEmpty()) {
          savePatterns(ticker, priceDate, timeframe, matches);
          log.info(
              "Saved {} {} chart patterns for ticker {} (priceDate={}).",
              matches.size(),
              timeframe,
              ticker.getTickerSymbol(),
              priceDate);
        } else {
          log.debug(
              "No {} chart patterns detected for ticker {} (priceDate={}).",
              timeframe,
              ticker.getTickerSymbol(),
              priceDate);
        }
      }
    } else {
      log.debug(
          "Ticker {}: Insufficient {} bars ({}) for chart patterns.",
          ticker.getTickerSymbol(),
          timeframe,
          bars.size());
    }
  }

  private List<PriceBar> loadBars(Ticker ticker, Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
          .map(PriceBar::from)
          .toList();
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
          .map(PriceBar::from)
          .toList();
    } else {
      log.error("Unsupported timeframe for loading bars: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  private void deleteOlderPatterns(Ticker ticker, LocalDate cutoffDate, Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      dailyChartPatternRepository.deleteByTickerAndPriceDateLessThan(ticker, cutoffDate);
    } else {
      weeklyChartPatternRepository.deleteByTickerAndPriceDateLessThan(ticker, cutoffDate);
    }
  }

  private void savePatterns(
      Ticker ticker,
      LocalDate priceDate,
      Timeframe timeframe,
      List<ChartPatternMatch> patternMatches) {
    if (timeframe == Timeframe.DAILY) {
      List<DailyChartPattern> entities =
          patternMatches.stream()
              .map(match ->
                  DailyChartPattern.builder()
                      .ticker(ticker)
                      .priceDate(priceDate)
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
                      .build())
              .toList();
      dailyChartPatternRepository.saveAll(entities);
    } else {
      List<WeeklyChartPattern> entities =
          patternMatches.stream()
              .map(match ->
                  WeeklyChartPattern.builder()
                      .ticker(ticker)
                      .priceDate(priceDate)
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
                      .build())
              .toList();
      weeklyChartPatternRepository.saveAll(entities);
    }
  }

  public List<ChartPatternMatch> detectAllPatterns(List<PriceBar> priceBars) {
    if (priceBars.size() >= ChartPatternConfig.MIN_BARS) {
      List<BigDecimal> atrSeries =
          IndicatorMath.calculateWilderAtrSeries(priceBars, ChartPatternConfig.ATR_PERIOD);
      List<SwingPoint> swings =
          dowAtrSwingDetector.detectSwings(
              priceBars,
              atrSeries,
              ChartPatternConfig.ATR_PERIOD,
              ChartPatternConfig.SWING_ATR_MULTIPLIER);
      return detectPatternsFromSwings(priceBars, swings);
    } else {
      return List.of();
    }
  }

  public List<ChartPatternMatch> detectPatternsFromSwings(
      List<PriceBar> priceBars, List<SwingPoint> swings) {
    if (swings.size() >= MIN_PIVOTS_FOR_PATTERNS) {
      List<ChartPatternMatch> matches = new ArrayList<>();
      detectDoubleFormations(priceBars, swings, matches);
      detectTripleFormations(priceBars, swings, matches);
      detectHeadAndShoulders(priceBars, swings, matches);
      detectTriangles(priceBars, swings, matches);
      detectWedges(priceBars, swings, matches);
      detectCupAndHandle(priceBars, swings, matches);
      return matches;
    } else {
      return List.of();
    }
  }

  private void detectDoubleFormations(
      List<PriceBar> priceBars, List<SwingPoint> swings, List<ChartPatternMatch> matches) {
    for (int i = 0; i <= swings.size() - 3; i++) {
      SwingPoint pivot1 = swings.get(i);
      SwingPoint pivot2 = swings.get(i + 1);
      SwingPoint pivot3 = swings.get(i + 2);
      DowTrend priorTrend = dowTrendClassifier.classifyPriorTrend(swings, i);

      if (pivot1.type() == SwingType.HIGH) {
        if (priorTrend == DowTrend.UPTREND) {
          BigDecimal diff = pivot1.price().subtract(pivot3.price()).abs();
          BigDecimal maxHigh = pivot1.price().max(pivot3.price());
          BigDecimal ratio = diff.divide(maxHigh, ChartPatternConfig.MATH_CONTEXT);
          BigDecimal depth = maxHigh.subtract(pivot2.price());
          BigDecimal depthRatio = depth.divide(maxHigh, ChartPatternConfig.MATH_CONTEXT);

          if (ratio.compareTo(ChartPatternConfig.TOLERANCE_3_PERCENT) <= 0
              && depthRatio.compareTo(ChartPatternConfig.MIN_DEPTH_RATIO) >= 0) {
            BigDecimal neckline = pivot2.price();
            BigDecimal target = scalePrice(neckline.subtract(depth));
            BigDecimal invalidation = scalePrice(maxHigh);
            BigDecimal stopLoss = scalePrice(pivot3.price());
            List<ChartPatternPivot> patternPivots =
                List.of(
                    toPivot(pivot1, "PEAK_1"),
                    toPivot(pivot2, "NECKLINE"),
                    toPivot(pivot3, "PEAK_2"));
            matches.add(
                buildMatch(
                    ChartPatternType.DOUBLE_TOP,
                    priceBars,
                    pivot1.date(),
                    pivot3,
                    neckline,
                    target,
                    stopLoss,
                    invalidation,
                    false,
                    patternPivots));
          }
        }
      } else if (priorTrend == DowTrend.DOWNTREND) {
        BigDecimal diff = pivot1.price().subtract(pivot3.price()).abs();
        BigDecimal maxLow = pivot1.price().max(pivot3.price());
        BigDecimal ratio = diff.divide(maxLow, ChartPatternConfig.MATH_CONTEXT);
        BigDecimal minLow = pivot1.price().min(pivot3.price());
        BigDecimal height = pivot2.price().subtract(minLow);
        BigDecimal heightRatio = height.divide(pivot2.price(), ChartPatternConfig.MATH_CONTEXT);

        if (ratio.compareTo(ChartPatternConfig.TOLERANCE_3_PERCENT) <= 0
            && heightRatio.compareTo(ChartPatternConfig.MIN_DEPTH_RATIO) >= 0) {
          BigDecimal neckline = pivot2.price();
          BigDecimal target = scalePrice(neckline.add(height));
          BigDecimal invalidation = scalePrice(minLow);
          BigDecimal stopLoss = scalePrice(pivot3.price());
          List<ChartPatternPivot> patternPivots =
              List.of(
                  toPivot(pivot1, "TROUGH_1"),
                  toPivot(pivot2, "NECKLINE"),
                  toPivot(pivot3, "TROUGH_2"));
          matches.add(
              buildMatch(
                  ChartPatternType.DOUBLE_BOTTOM,
                  priceBars,
                  pivot1.date(),
                  pivot3,
                  neckline,
                  target,
                  stopLoss,
                  invalidation,
                  true,
                  patternPivots));
        }
      }
    }
  }

  private void detectTripleFormations(
      List<PriceBar> priceBars, List<SwingPoint> swings, List<ChartPatternMatch> matches) {
    for (int i = 0; i <= swings.size() - 5; i++) {
      SwingPoint pivot1 = swings.get(i);
      SwingPoint pivot2 = swings.get(i + 1);
      SwingPoint pivot3 = swings.get(i + 2);
      SwingPoint pivot4 = swings.get(i + 3);
      SwingPoint pivot5 = swings.get(i + 4);
      DowTrend priorTrend = dowTrendClassifier.classifyPriorTrend(swings, i);

      if (pivot1.type() == SwingType.HIGH) {
        if (priorTrend == DowTrend.UPTREND) {
          BigDecimal maxHigh = pivot1.price().max(pivot3.price()).max(pivot5.price());
          BigDecimal minHigh = pivot1.price().min(pivot3.price()).min(pivot5.price());
          BigDecimal spread =
              maxHigh.subtract(minHigh).divide(maxHigh, ChartPatternConfig.MATH_CONTEXT);
          BigDecimal neckline = pivot2.price().min(pivot4.price());
          BigDecimal height = maxHigh.subtract(neckline);

          if (spread.compareTo(ChartPatternConfig.TOLERANCE_4_PERCENT) <= 0
              && height
                      .divide(maxHigh, ChartPatternConfig.MATH_CONTEXT)
                      .compareTo(ChartPatternConfig.MIN_DEPTH_RATIO)
                  >= 0) {
            BigDecimal target = scalePrice(neckline.subtract(height));
            BigDecimal invalidation = scalePrice(maxHigh);
            BigDecimal stopLoss = scalePrice(pivot5.price());
            List<ChartPatternPivot> patternPivots =
                List.of(
                    toPivot(pivot1, "PEAK_1"),
                    toPivot(pivot2, "TROUGH_1"),
                    toPivot(pivot3, "PEAK_2"),
                    toPivot(pivot4, "TROUGH_2"),
                    toPivot(pivot5, "PEAK_3"));
            matches.add(
                buildMatch(
                    ChartPatternType.TRIPLE_TOP,
                    priceBars,
                    pivot1.date(),
                    pivot5,
                    neckline,
                    target,
                    stopLoss,
                    invalidation,
                    false,
                    patternPivots));
          }
        }
      } else if (priorTrend == DowTrend.DOWNTREND) {
        BigDecimal maxLow = pivot1.price().max(pivot3.price()).max(pivot5.price());
        BigDecimal minLow = pivot1.price().min(pivot3.price()).min(pivot5.price());
        BigDecimal spread = maxLow.subtract(minLow).divide(maxLow, ChartPatternConfig.MATH_CONTEXT);
        BigDecimal neckline = pivot2.price().max(pivot4.price());
        BigDecimal height = neckline.subtract(minLow);

        if (spread.compareTo(ChartPatternConfig.TOLERANCE_4_PERCENT) <= 0
            && height
                    .divide(neckline, ChartPatternConfig.MATH_CONTEXT)
                    .compareTo(ChartPatternConfig.MIN_DEPTH_RATIO)
                >= 0) {
          BigDecimal target = scalePrice(neckline.add(height));
          BigDecimal invalidation = scalePrice(minLow);
          BigDecimal stopLoss = scalePrice(pivot5.price());
          List<ChartPatternPivot> patternPivots =
              List.of(
                  toPivot(pivot1, "TROUGH_1"),
                  toPivot(pivot2, "PEAK_1"),
                  toPivot(pivot3, "TROUGH_2"),
                  toPivot(pivot4, "PEAK_2"),
                  toPivot(pivot5, "TROUGH_3"));
          matches.add(
              buildMatch(
                  ChartPatternType.TRIPLE_BOTTOM,
                  priceBars,
                  pivot1.date(),
                  pivot5,
                  neckline,
                  target,
                  stopLoss,
                  invalidation,
                  true,
                  patternPivots));
        }
      }
    }
  }

  private void detectHeadAndShoulders(
      List<PriceBar> priceBars, List<SwingPoint> swings, List<ChartPatternMatch> matches) {
    for (int i = 0; i <= swings.size() - 5; i++) {
      SwingPoint pivot1 = swings.get(i);
      SwingPoint pivot2 = swings.get(i + 1);
      SwingPoint pivot3 = swings.get(i + 2);
      SwingPoint pivot4 = swings.get(i + 3);
      SwingPoint pivot5 = swings.get(i + 4);
      DowTrend priorTrend = dowTrendClassifier.classifyPriorTrend(swings, i);

      if (pivot1.type() == SwingType.HIGH) {
        if (priorTrend == DowTrend.UPTREND) {
          boolean headHigher =
              pivot3.price().compareTo(pivot1.price()) > 0
                  && pivot3.price().compareTo(pivot5.price()) > 0;
          BigDecimal shoulderDiff = pivot1.price().subtract(pivot5.price()).abs();
          BigDecimal shoulderRatio =
              shoulderDiff.divide(
                  pivot1.price().max(pivot5.price()), ChartPatternConfig.MATH_CONTEXT);

          if (headHigher && shoulderRatio.compareTo(ChartPatternConfig.TOLERANCE_5_PERCENT) <= 0) {
            BigDecimal neckline = pivot2.price().min(pivot4.price());
            BigDecimal height = pivot3.price().subtract(neckline);
            BigDecimal target = scalePrice(neckline.subtract(height));
            BigDecimal invalidation = scalePrice(pivot3.price());
            BigDecimal stopLoss = scalePrice(pivot5.price());
            List<ChartPatternPivot> patternPivots =
                List.of(
                    toPivot(pivot1, "LEFT_SHOULDER"),
                    toPivot(pivot2, "TROUGH_1"),
                    toPivot(pivot3, "HEAD"),
                    toPivot(pivot4, "TROUGH_2"),
                    toPivot(pivot5, "RIGHT_SHOULDER"));
            matches.add(
                buildMatch(
                    ChartPatternType.HEAD_AND_SHOULDERS,
                    priceBars,
                    pivot1.date(),
                    pivot5,
                    neckline,
                    target,
                    stopLoss,
                    invalidation,
                    false,
                    patternPivots));
          }
        }
      } else if (priorTrend == DowTrend.DOWNTREND) {
        boolean headLower =
            pivot3.price().compareTo(pivot1.price()) < 0
                && pivot3.price().compareTo(pivot5.price()) < 0;
        BigDecimal shoulderDiff = pivot1.price().subtract(pivot5.price()).abs();
        BigDecimal shoulderRatio =
            shoulderDiff.divide(
                pivot1.price().max(pivot5.price()), ChartPatternConfig.MATH_CONTEXT);

        if (headLower && shoulderRatio.compareTo(ChartPatternConfig.TOLERANCE_5_PERCENT) <= 0) {
          BigDecimal neckline = pivot2.price().max(pivot4.price());
          BigDecimal height = neckline.subtract(pivot3.price());
          BigDecimal target = scalePrice(neckline.add(height));
          BigDecimal invalidation = scalePrice(pivot3.price());
          BigDecimal stopLoss = scalePrice(pivot5.price());
          List<ChartPatternPivot> patternPivots =
              List.of(
                  toPivot(pivot1, "LEFT_SHOULDER"),
                  toPivot(pivot2, "PEAK_1"),
                  toPivot(pivot3, "HEAD"),
                  toPivot(pivot4, "PEAK_2"),
                  toPivot(pivot5, "RIGHT_SHOULDER"));
          matches.add(
              buildMatch(
                  ChartPatternType.INVERSE_HEAD_AND_SHOULDERS,
                  priceBars,
                  pivot1.date(),
                  pivot5,
                  neckline,
                  target,
                  stopLoss,
                  invalidation,
                  true,
                  patternPivots));
        }
      }
    }
  }

  private void detectTriangles(
      List<PriceBar> priceBars, List<SwingPoint> swings, List<ChartPatternMatch> matches) {
    for (int i = 0; i <= swings.size() - 4; i++) {
      SwingPoint pivot1 = swings.get(i);
      SwingPoint pivot2 = swings.get(i + 1);
      SwingPoint pivot3 = swings.get(i + 2);
      SwingPoint pivot4 = swings.get(i + 3);

      if (pivot1.type() == SwingType.HIGH) {
        BigDecimal highDiff = pivot1.price().subtract(pivot3.price()).abs();
        BigDecimal highSpread =
            highDiff.divide(pivot1.price().max(pivot3.price()), ChartPatternConfig.MATH_CONTEXT);

        if (highSpread.compareTo(ChartPatternConfig.TOLERANCE_3_PERCENT) <= 0
            && pivot4.price().compareTo(pivot2.price()) > 0) {
          BigDecimal resistance = pivot1.price().max(pivot3.price());
          BigDecimal height = resistance.subtract(pivot2.price());
          List<ChartPatternPivot> patternPivots =
              List.of(
                  toPivot(pivot1, "RESISTANCE_1"),
                  toPivot(pivot2, "SUPPORT_1"),
                  toPivot(pivot3, "RESISTANCE_2"),
                  toPivot(pivot4, "SUPPORT_2"));
          matches.add(
              buildMatch(
                  ChartPatternType.ASCENDING_TRIANGLE,
                  priceBars,
                  pivot1.date(),
                  pivot4,
                  resistance,
                  scalePrice(resistance.add(height)),
                  scalePrice(pivot4.price()),
                  scalePrice(pivot2.price()),
                  true,
                  patternPivots));
        }

        if (pivot3.price().compareTo(pivot1.price()) < 0
            && pivot4.price().compareTo(pivot2.price()) > 0) {
          BigDecimal height = pivot1.price().subtract(pivot2.price());
          BigDecimal trigger = pivot3.price();
          List<ChartPatternPivot> patternPivots =
              List.of(
                  toPivot(pivot1, "PEAK_1"),
                  toPivot(pivot2, "TROUGH_1"),
                  toPivot(pivot3, "PEAK_2"),
                  toPivot(pivot4, "TROUGH_2"));
          matches.add(
              buildMatch(
                  ChartPatternType.SYMMETRICAL_TRIANGLE,
                  priceBars,
                  pivot1.date(),
                  pivot4,
                  trigger,
                  scalePrice(trigger.add(height)),
                  scalePrice(pivot4.price()),
                  scalePrice(pivot2.price()),
                  true,
                  patternPivots));
        }
      } else {
        BigDecimal lowDiff = pivot1.price().subtract(pivot3.price()).abs();
        BigDecimal lowSpread =
            lowDiff.divide(pivot1.price().max(pivot3.price()), ChartPatternConfig.MATH_CONTEXT);

        if (lowSpread.compareTo(ChartPatternConfig.TOLERANCE_3_PERCENT) <= 0
            && pivot4.price().compareTo(pivot2.price()) < 0) {
          BigDecimal support = pivot1.price().min(pivot3.price());
          BigDecimal height = pivot2.price().subtract(support);
          List<ChartPatternPivot> patternPivots =
              List.of(
                  toPivot(pivot1, "SUPPORT_1"),
                  toPivot(pivot2, "RESISTANCE_1"),
                  toPivot(pivot3, "SUPPORT_2"),
                  toPivot(pivot4, "RESISTANCE_2"));
          matches.add(
              buildMatch(
                  ChartPatternType.DESCENDING_TRIANGLE,
                  priceBars,
                  pivot1.date(),
                  pivot4,
                  support,
                  scalePrice(support.subtract(height)),
                  scalePrice(pivot4.price()),
                  scalePrice(pivot2.price()),
                  false,
                  patternPivots));
        }
      }
    }
  }

  private void detectWedges(
      List<PriceBar> priceBars, List<SwingPoint> swings, List<ChartPatternMatch> matches) {
    for (int i = 0; i <= swings.size() - 4; i++) {
      SwingPoint pivot1 = swings.get(i);
      SwingPoint pivot2 = swings.get(i + 1);
      SwingPoint pivot3 = swings.get(i + 2);
      SwingPoint pivot4 = swings.get(i + 3);

      if (pivot1.type() == SwingType.LOW) {
        if (pivot4.price().compareTo(pivot2.price()) > 0
            && pivot3.price().compareTo(pivot1.price()) > 0) {
          BigDecimal highDiff = pivot4.price().subtract(pivot2.price());
          BigDecimal lowDiff = pivot3.price().subtract(pivot1.price());

          if (lowDiff.compareTo(highDiff) > 0) {
            BigDecimal trigger = pivot3.price();
            List<ChartPatternPivot> patternPivots =
                List.of(
                    toPivot(pivot1, "SUPPORT_1"),
                    toPivot(pivot2, "RESISTANCE_1"),
                    toPivot(pivot3, "SUPPORT_2"),
                    toPivot(pivot4, "RESISTANCE_2"));
            matches.add(
                buildMatch(
                    ChartPatternType.RISING_WEDGE,
                    priceBars,
                    pivot1.date(),
                    pivot4,
                    trigger,
                    scalePrice(pivot1.price()),
                    scalePrice(pivot4.price()),
                    scalePrice(pivot4.price()),
                    false,
                    patternPivots));
          }
        }
      } else {
        if (pivot3.price().compareTo(pivot1.price()) < 0
            && pivot4.price().compareTo(pivot2.price()) < 0) {
          BigDecimal highDiff = pivot1.price().subtract(pivot3.price());
          BigDecimal lowDiff = pivot2.price().subtract(pivot4.price());

          if (highDiff.compareTo(lowDiff) > 0) {
            BigDecimal trigger = pivot3.price();
            List<ChartPatternPivot> patternPivots =
                List.of(
                    toPivot(pivot1, "RESISTANCE_1"),
                    toPivot(pivot2, "SUPPORT_1"),
                    toPivot(pivot3, "RESISTANCE_2"),
                    toPivot(pivot4, "SUPPORT_2"));
            matches.add(
                buildMatch(
                    ChartPatternType.FALLING_WEDGE,
                    priceBars,
                    pivot1.date(),
                    pivot4,
                    trigger,
                    scalePrice(pivot1.price()),
                    scalePrice(pivot4.price()),
                    scalePrice(pivot4.price()),
                    true,
                    patternPivots));
          }
        }
      }
    }
  }

  private void detectCupAndHandle(
      List<PriceBar> priceBars, List<SwingPoint> swings, List<ChartPatternMatch> matches) {
    for (int i = 0; i <= swings.size() - 4; i++) {
      SwingPoint pivot1 = swings.get(i);
      SwingPoint pivot2 = swings.get(i + 1);
      SwingPoint pivot3 = swings.get(i + 2);
      SwingPoint pivot4 = swings.get(i + 3);

      if (pivot1.type() == SwingType.HIGH) {
        BigDecimal rimDiff = pivot1.price().subtract(pivot3.price()).abs();
        BigDecimal rimRatio =
            rimDiff.divide(pivot1.price().max(pivot3.price()), ChartPatternConfig.MATH_CONTEXT);
        BigDecimal cupDepth = pivot1.price().max(pivot3.price()).subtract(pivot2.price());
        BigDecimal handlePullback = pivot3.price().subtract(pivot4.price());

        if (rimRatio.compareTo(ChartPatternConfig.TOLERANCE_4_PERCENT) <= 0
            && cupDepth
                    .divide(pivot1.price(), ChartPatternConfig.MATH_CONTEXT)
                    .compareTo(ChartPatternConfig.CUP_MIN_DEPTH_RATIO)
                >= 0
            && pivot4.price().compareTo(pivot2.price()) > 0
            && handlePullback.compareTo(
                    cupDepth.divide(ChartPatternConfig.TWO, ChartPatternConfig.MATH_CONTEXT))
                <= 0) {
          BigDecimal resistance = pivot3.price();
          List<ChartPatternPivot> patternPivots =
              List.of(
                  toPivot(pivot1, "LEFT_RIM"),
                  toPivot(pivot2, "CUP_BOTTOM"),
                  toPivot(pivot3, "RIGHT_RIM"),
                  toPivot(pivot4, "HANDLE_TROUGH"));
          matches.add(
              buildMatch(
                  ChartPatternType.CUP_AND_HANDLE,
                  priceBars,
                  pivot1.date(),
                  pivot4,
                  resistance,
                  scalePrice(resistance.add(cupDepth)),
                  scalePrice(pivot4.price()),
                  scalePrice(pivot4.price()),
                  true,
                  patternPivots));
        }
      }
    }
  }

  private ChartPatternMatch buildMatch(
      ChartPatternType patternType,
      List<PriceBar> priceBars,
      LocalDate startDate,
      SwingPoint lastPivot,
      BigDecimal triggerPrice,
      BigDecimal targetPrice,
      BigDecimal stopLossPrice,
      BigDecimal invalidationPrice,
      boolean isBullish,
      List<ChartPatternPivot> patternPivots) {
    ChartPatternStatus status =
        evaluateStatus(
            priceBars,
            lastPivot.barIndex(),
            triggerPrice,
            targetPrice,
            invalidationPrice,
            isBullish);
    LocalDate breakoutDate =
        findBreakoutDate(priceBars, lastPivot.barIndex(), triggerPrice, isBullish);

    return ChartPatternMatch.builder()
        .patternType(patternType)
        .sentiment(patternType.getSentiment())
        .status(status)
        .startDate(startDate)
        .endDate(lastPivot.date())
        .breakoutDate(breakoutDate)
        .necklinePrice(scalePrice(triggerPrice))
        .targetPrice(targetPrice)
        .stopLossPrice(stopLossPrice)
        .invalidationPrice(invalidationPrice)
        .pivotPoints(patternPivots)
        .build();
  }

  private ChartPatternPivot toPivot(SwingPoint swingPoint, String role) {
    return new ChartPatternPivot(
        swingPoint.date(), swingPoint.price(), swingPoint.type().name(), role);
  }

  private BigDecimal scalePrice(BigDecimal price) {
    return price.setScale(ChartPatternConfig.PRICE_SCALE, RoundingMode.HALF_UP);
  }

  private ChartPatternStatus evaluateStatus(
      List<PriceBar> priceBars,
      int lastPivotIndex,
      BigDecimal triggerPrice,
      BigDecimal targetPrice,
      BigDecimal invalidationPrice,
      boolean isBullish) {
    if (lastPivotIndex < priceBars.size() - 1) {
      boolean brokenOut = false;
      for (int i = lastPivotIndex + 1; i < priceBars.size(); i++) {
        PriceBar bar = priceBars.get(i);
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
    } else {
      return ChartPatternStatus.IN_PROGRESS;
    }
  }

  private LocalDate findBreakoutDate(
      List<PriceBar> priceBars, int lastPivotIndex, BigDecimal triggerPrice, boolean isBullish) {
    for (int i = lastPivotIndex + 1; i < priceBars.size(); i++) {
      PriceBar bar = priceBars.get(i);
      if (isBullish && bar.close().compareTo(triggerPrice) > 0) {
        return bar.date();
      } else if (!isBullish && bar.close().compareTo(triggerPrice) < 0) {
        return bar.date();
      }
    }
    return null;
  }
}
