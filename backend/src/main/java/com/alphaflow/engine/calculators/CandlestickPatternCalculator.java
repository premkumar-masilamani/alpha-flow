package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.dtos.PatternMatch;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import com.alphaflow.engine.pipeline.PipelineJob;
import com.alphaflow.persistence.entities.DailyCandlestickPattern;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyCandlestickPattern;
import com.alphaflow.persistence.enums.CandlestickPattern;
import com.alphaflow.persistence.repositories.DailyCandlestickPatternRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyCandlestickPatternRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class CandlestickPatternCalculator implements PipelineJob {

  public static final String JOB_NAME = "CALCULATE_CANDLESTICK_PATTERN";

  private static final MathContext MC = new MathContext(18, RoundingMode.HALF_UP);
  private static final BigDecimal THRESHOLD_LARGE = new BigDecimal("1.5");
  private static final BigDecimal RATIO_STAR_OUTER = new BigDecimal("0.7");
  private static final BigDecimal THRESHOLD_HALF = new BigDecimal("0.5");
  private static final BigDecimal THRESHOLD_SMALL = new BigDecimal("0.3");
  private static final BigDecimal THRESHOLD_VERY_SMALL = new BigDecimal("0.2");
  private static final BigDecimal THRESHOLD_MINIMAL = new BigDecimal("0.1");
  private static final BigDecimal THRESHOLD_DOJI = new BigDecimal("0.1");
  private static final BigDecimal THRESHOLD_TINY = new BigDecimal("0.05");
  private static final BigDecimal RATIO_HAMMER_SHADOW = new BigDecimal("2.0");
  private static final BigDecimal RATIO_HAMMER_UPPER = new BigDecimal("0.1");
  private static final BigDecimal DIVISOR_MIDPOINT = new BigDecimal("2");
  private static final int MIN_BARS = 5;
  private static final int LOOKBACK_BARS = MIN_BARS - 1;
  private static final int PERIOD_BODY_SMA = 14;

  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final DailyCandlestickPatternRepository dailyCandlestickPatternRepository;
  private final WeeklyCandlestickPatternRepository weeklyCandlestickPatternRepository;
  private final TickerRepository tickerRepository;

  @Autowired @Lazy private CandlestickPatternCalculator selfProxy;

  public CandlestickPatternCalculator(
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailyCandlestickPatternRepository dailyCandlestickPatternRepository,
      WeeklyCandlestickPatternRepository weeklyCandlestickPatternRepository,
      TickerRepository tickerRepository) {
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailyCandlestickPatternRepository = dailyCandlestickPatternRepository;
    this.weeklyCandlestickPatternRepository = weeklyCandlestickPatternRepository;
    this.tickerRepository = tickerRepository;
  }

  @Override
  public String getJobName() {
    return JOB_NAME;
  }

  @Override
  public boolean shouldRun(Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyCandlestickPatternRepository.hasPendingDailyCandlestickPatterns();
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyCandlestickPatternRepository.hasPendingWeeklyCandlestickPatterns();
    } else {
      log.error("Unsupported timeframe for candlestick patterns shouldRun: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  @Override
  public void execute(Timeframe timeframe) {
    List<Ticker> targetTickers;
    Map<Ticker, LocalDate> latestDates;
    if (timeframe == Timeframe.DAILY) {
      targetTickers =
          dailyCandlestickPatternRepository.findTickersWithPendingDailyCandlestickPatterns();
      latestDates = dailyCandlestickPatternRepository.findLatestPriceDatesForActiveTickers();
    } else if (timeframe == Timeframe.WEEKLY) {
      targetTickers = tickerRepository.findByIsActiveTrue();
      latestDates = weeklyCandlestickPatternRepository.findLatestPriceDatesForActiveTickers();
    } else {
      log.error("Unsupported timeframe for candlestick patterns computation: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }

    log.info("Computing {} candlestick patterns...", timeframe);
    log.info("Found {} tickers to process for {} patterns.", targetTickers.size(), timeframe);

    CandlestickPatternCalculator proxy = (selfProxy != null) ? selfProxy : this;

    for (Ticker ticker : targetTickers) {
      try {
        LocalDate latestCalculatedDate = latestDates.get(ticker);
        proxy.computeCandleStickPatternsForTicker(ticker, timeframe, latestCalculatedDate);
      } catch (Exception exception) {
        log.error(
            "Failed to compute {} candlestick patterns for ticker {}: {}",
            timeframe,
            ticker.getTickerSymbol(),
            exception.getMessage(),
            exception);
      }
    }
    log.info("{} candlestick patterns computed.", timeframe);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void computeCandleStickPatternsForTicker(
      Ticker ticker, Timeframe timeframe, LocalDate latestCalculatedDate) {
    if (timeframe == Timeframe.DAILY) {
      processDailyPatterns(ticker, latestCalculatedDate);
    } else if (timeframe == Timeframe.WEEKLY) {
      processWeeklyPatterns(ticker, latestCalculatedDate);
    } else {
      log.error("Unsupported timeframe for computing candlestick patterns: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  private void processDailyPatterns(Ticker ticker, LocalDate latestCalculatedDate) {
    List<PriceBar> bars =
        dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
            .map(PriceBar::from)
            .toList();
    if (bars.size() >= MIN_BARS) {
      int lastIndex = findDateIndex(bars, latestCalculatedDate);
      int startIndex = lastIndex != -1 ? Math.max(LOOKBACK_BARS, lastIndex + 1) : LOOKBACK_BARS;
      if (startIndex < bars.size()) {
        log.info(
            "Ticker {}: Timeframe {} - Calculating from index {}...",
            ticker.getTickerSymbol(),
            Timeframe.DAILY,
            startIndex);
        List<PatternMatch> matches = findMatches(bars, startIndex);
        saveDailyPatterns(ticker, matches);
      } else {
        log.debug(
            "Ticker {}: Daily candlestick patterns already up to date as of {}.",
            ticker.getTickerSymbol(),
            latestCalculatedDate);
      }
    } else {
      log.debug(
          "Ticker {}: Insufficient daily data points (found {}) to compute patterns.",
          ticker.getTickerSymbol(),
          bars.size());
    }
  }

  private void processWeeklyPatterns(Ticker ticker, LocalDate latestCalculatedDate) {
    List<PriceBar> bars =
        weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
            .map(PriceBar::from)
            .toList();
    if (bars.size() >= MIN_BARS) {
      int lastIndex = findDateIndex(bars, latestCalculatedDate);
      int startIndex = lastIndex != -1 ? Math.max(LOOKBACK_BARS, lastIndex) : LOOKBACK_BARS;
      LocalDate recomputeStartDate = lastIndex != -1 ? bars.get(startIndex).date() : null;
      log.info(
          "Ticker {}: Timeframe {} - Calculating from index {}...",
          ticker.getTickerSymbol(),
          Timeframe.WEEKLY,
          startIndex);
      List<PatternMatch> matches = findMatches(bars, startIndex);
      saveWeeklyPatterns(ticker, matches, recomputeStartDate);
    } else {
      log.debug(
          "Ticker {}: Insufficient weekly data points (found {}) to compute patterns.",
          ticker.getTickerSymbol(),
          bars.size());
    }
  }

  private int findDateIndex(List<PriceBar> bars, LocalDate date) {
    if (date != null) {
      for (int k = bars.size() - 1; k >= 0; k--) {
        if (bars.get(k).date().equals(date)) {
          return k;
        }
      }
    }
    return -1;
  }

  private void saveDailyPatterns(Ticker ticker, List<PatternMatch> matches) {
    if (!matches.isEmpty()) {
      List<DailyCandlestickPattern> newPatterns =
          matches.stream()
              .map(match ->
                  DailyCandlestickPattern.builder()
                      .ticker(ticker)
                      .priceDate(match.date())
                      .pattern(match.pattern())
                      .sentiment(match.pattern().getSentiment())
                      .build())
              .toList();
      dailyCandlestickPatternRepository.saveAll(newPatterns);
      log.info(
          "Ticker {}: Saved {} daily candlestick patterns.",
          ticker.getTickerSymbol(),
          matches.size());
    }
  }

  private void saveWeeklyPatterns(
      Ticker ticker, List<PatternMatch> matches, LocalDate recomputeStartDate) {
    if (recomputeStartDate != null) {
      weeklyCandlestickPatternRepository.deleteByTickerAndPriceDateGreaterThanEqual(
          ticker, recomputeStartDate);
      log.info(
          "Ticker {}: Cleaned up weekly patterns on or after {} for recomputation.",
          ticker.getTickerSymbol(),
          recomputeStartDate);
    }

    if (!matches.isEmpty()) {
      List<WeeklyCandlestickPattern> newPatterns =
          matches.stream()
              .map(match ->
                  WeeklyCandlestickPattern.builder()
                      .ticker(ticker)
                      .priceDate(match.date())
                      .pattern(match.pattern())
                      .sentiment(match.pattern().getSentiment())
                      .build())
              .toList();
      weeklyCandlestickPatternRepository.saveAll(newPatterns);
      log.info(
          "Ticker {}: Saved {} weekly candlestick patterns (including recomputed window).",
          ticker.getTickerSymbol(),
          matches.size());
    }
  }

  private List<PatternMatch> findMatches(List<PriceBar> bars, int startIndex) {
    List<PatternMatch> matches = new ArrayList<>();
    List<BigDecimal> avgBodies =
        IndicatorMath.computeSimpleMovingAverage(bars, this::body, PERIOD_BODY_SMA, startIndex);

    for (int i = startIndex; i < bars.size(); i++) {
      PriceBar bar = bars.get(i);
      LocalDate date = bar.date();
      BigDecimal avgBody = avgBodies.get(i - startIndex);

      for (CandlestickPattern pattern : CandlestickPattern.values()) {
        if (matchesPattern(bars, i, pattern, avgBody)) {
          matches.add(new PatternMatch(date, pattern));
        }
      }
    }
    return matches;
  }

  @SuppressWarnings("PMD.ExhaustiveSwitchHasDefault")
  private boolean matchesPattern(
      List<PriceBar> bars, int i, CandlestickPattern pattern, BigDecimal avgBody) {
    PriceBar cur = bars.get(i);
    BigDecimal curBody = body(cur);
    boolean curGreen = isGreen(cur);
    boolean curRed = isRed(cur);

    PriceBar prev = i >= 1 ? bars.get(i - 1) : null;
    PriceBar prev2 = i >= 2 ? bars.get(i - 2) : null;
    PriceBar prev3 = i >= 3 ? bars.get(i - 3) : null;
    PriceBar prev4 = i >= 4 ? bars.get(i - 4) : null;

    BigDecimal prevBody = prev != null ? body(prev) : BigDecimal.ZERO;
    BigDecimal prev2Body = prev2 != null ? body(prev2) : BigDecimal.ZERO;
    BigDecimal prev3Body = prev3 != null ? body(prev3) : BigDecimal.ZERO;
    BigDecimal prev4Body = prev4 != null ? body(prev4) : BigDecimal.ZERO;

    boolean prevGreen = prev != null && isGreen(prev);
    boolean prevRed = prev != null && isRed(prev);
    boolean prev2Green = prev2 != null && isGreen(prev2);
    boolean prev2Red = prev2 != null && isRed(prev2);
    boolean prev3Green = prev3 != null && isGreen(prev3);
    boolean prev3Red = prev3 != null && isRed(prev3);
    boolean prev4Green = prev4 != null && isGreen(prev4);
    boolean prev4Red = prev4 != null && isRed(prev4);

    switch (pattern) {
      // A. Bullish Reversals
      case LONG_WHITE_BODY:
        return curGreen && curBody.compareTo(avgBody.multiply(THRESHOLD_LARGE, MC)) >= 0;

      case HAMMER:
        return curBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) <= 0
            && lowerShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_SHADOW, MC)) >= 0
            && upperShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0;

      case INVERTED_HAMMER:
        return curBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) <= 0
            && upperShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_SHADOW, MC)) >= 0
            && lowerShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0;

      case BULLISH_BELT_HOLD:
        return curGreen
            && lowerShadow(cur).compareTo(curBody.multiply(THRESHOLD_TINY, MC)) <= 0
            && upperShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0
            && curBody.compareTo(avgBody) >= 0;

      case BULLISH_ENGULFING:
        return prev != null
            && prevRed
            && curGreen
            && cur.open().compareTo(prev.close()) <= 0
            && cur.close().compareTo(prev.open()) >= 0
            && (cur.open().compareTo(prev.close()) < 0 || cur.close().compareTo(prev.open()) > 0);

      case BULLISH_HARAMI:
        return prev != null
            && prevRed
            && prevBody.compareTo(avgBody) >= 0
            && curGreen
            && curBody.compareTo(prevBody) < 0
            && cur.open().compareTo(prev.close()) >= 0
            && cur.close().compareTo(prev.open()) <= 0;

      case BULLISH_HARAMI_CROSS:
        return prev != null
            && prevRed
            && prevBody.compareTo(avgBody) >= 0
            && curBody.compareTo(avgBody.multiply(THRESHOLD_DOJI, MC)) <= 0
            && cur.open().compareTo(prev.close()) >= 0
            && cur.close().compareTo(prev.open()) <= 0;

      case PIERCING_LINE:
        if (prev == null || !prevRed || !curGreen || prevBody.compareTo(avgBody) < 0) {
          return false;
        }
        BigDecimal midpointPiercingLine =
            prev.close().add(prev.open()).divide(DIVISOR_MIDPOINT, MC);
        return cur.open().compareTo(prev.low()) < 0
            && cur.close().compareTo(midpointPiercingLine) > 0
            && cur.close().compareTo(prev.open()) <= 0;

      case BULLISH_DOJI_STAR:
        return prev != null
            && prevRed
            && prevBody.compareTo(avgBody) >= 0
            && curBody.compareTo(avgBody.multiply(THRESHOLD_DOJI, MC)) <= 0
            && cur.open().compareTo(prev.close()) < 0
            && cur.close().compareTo(prev.close()) < 0;

      case BULLISH_MEETING_LINES:
        return prev != null
            && prevRed
            && curGreen
            && prevBody.compareTo(avgBody) >= 0
            && curBody.compareTo(avgBody) >= 0
            && cur.open().compareTo(prev.low()) < 0
            && cur.close()
                    .subtract(prev.close())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_TINY, MC))
                <= 0;

      case THREE_WHITE_SOLDIERS:
        return prev2 != null
            && curGreen
            && prevGreen
            && prev2Green
            && curBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) >= 0
            && prevBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) >= 0
            && prev2Body.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) >= 0
            && cur.open().compareTo(prev.open()) > 0
            && cur.open().compareTo(prev.close()) < 0
            && prev.open().compareTo(prev2.open()) > 0
            && prev.open().compareTo(prev2.close()) < 0
            && cur.close().compareTo(prev.close()) > 0
            && prev.close().compareTo(prev2.close()) > 0;

      case MORNING_STAR:
        if (prev2 == null
            || !prev2Red
            || prev2Body.compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) < 0) {
          return false;
        }
        if (prevBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) > 0) {
          return false;
        }
        BigDecimal morningStarMax = prev.open().max(prev.close());
        boolean gapDownMorningStar = morningStarMax.compareTo(prev2.close()) < 0;
        BigDecimal morningStarMidpoint =
            prev2.close().add(prev2.open()).divide(DIVISOR_MIDPOINT, MC);
        return gapDownMorningStar
            && curGreen
            && curBody.compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) >= 0
            && cur.close().compareTo(morningStarMidpoint) > 0;

      case MORNING_DOJI_STAR:
        if (prev2 == null
            || !prev2Red
            || prev2Body.compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) < 0) {
          return false;
        }
        if (prevBody.compareTo(avgBody.multiply(THRESHOLD_DOJI, MC)) > 0) {
          return false;
        }
        BigDecimal morningDojiStarMax = prev.open().max(prev.close());
        boolean gapDownMorningDojiStar = morningDojiStarMax.compareTo(prev2.close()) < 0;
        BigDecimal morningDojiStarMidpoint =
            prev2.close().add(prev2.open()).divide(DIVISOR_MIDPOINT, MC);
        return gapDownMorningDojiStar
            && curGreen
            && curBody.compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) >= 0
            && cur.close().compareTo(morningDojiStarMidpoint) > 0;

      case BULLISH_ABANDONED_BABY:
        return prev2 != null
            && prev2Red
            && curGreen
            && prevBody.compareTo(avgBody.multiply(THRESHOLD_DOJI, MC)) <= 0
            && prev.high().compareTo(prev2.low()) < 0
            && prev.high().compareTo(cur.low()) < 0;

      case BULLISH_TRI_STAR:
        return prev2 != null
            && curBody.compareTo(avgBody.multiply(THRESHOLD_DOJI, MC)) <= 0
            && prevBody.compareTo(avgBody.multiply(THRESHOLD_DOJI, MC)) <= 0
            && prev2Body.compareTo(avgBody.multiply(THRESHOLD_DOJI, MC)) <= 0
            && prev.open().compareTo(prev2.close()) < 0
            && prev.open().compareTo(cur.open()) < 0;

      case BULLISH_BREAKAWAY:
        return prev4 != null
            && prev4Red
            && prev4Body.compareTo(avgBody) >= 0
            && prev3Red
            && prev3.high().compareTo(prev4.low()) < 0
            && prev2Red
            && prevRed
            && prev2.close().compareTo(prev3.close()) < 0
            && prev.close().compareTo(prev2.close()) < 0
            && curGreen
            && curBody.compareTo(avgBody) >= 0
            && cur.close().compareTo(prev4.low()) < 0
            && cur.close().compareTo(prev3.open()) > 0;

      case THREE_INSIDE_UP:
        return prev2 != null
            && prev2Red
            && prev2Body.compareTo(avgBody) >= 0
            && isGreen(prev)
            && prevBody.compareTo(prev2Body) < 0
            && prev.open().compareTo(prev2.close()) >= 0
            && prev.close().compareTo(prev2.open()) <= 0
            && curGreen
            && cur.close().compareTo(prev.high()) > 0;

      case THREE_OUTSIDE_UP:
        return prev2 != null
            && prev2Red
            && isGreen(prev)
            && prev.open().compareTo(prev2.close()) <= 0
            && prev.close().compareTo(prev2.open()) >= 0
            && curGreen
            && cur.close().compareTo(prev.close()) > 0;

      case BULLISH_KICKING:
        return prev != null
            && prevRed
            && prevBody.compareTo(avgBody) >= 0
            && lowerShadow(prev).compareTo(prevBody.multiply(THRESHOLD_TINY, MC)) <= 0
            && upperShadow(prev).compareTo(prevBody.multiply(THRESHOLD_TINY, MC)) <= 0
            && curGreen
            && curBody.compareTo(avgBody) >= 0
            && lowerShadow(cur).compareTo(curBody.multiply(THRESHOLD_TINY, MC)) <= 0
            && upperShadow(cur).compareTo(curBody.multiply(THRESHOLD_TINY, MC)) <= 0
            && cur.open().compareTo(prev.open()) > 0;

      case UNIQUE_THREE_RIVERS_BOTTOM:
        return prev2 != null
            && prev2Red
            && prev2Body.compareTo(avgBody) >= 0
            && prevRed
            && prev.close().compareTo(prev2.close()) > 0
            && prev.low().compareTo(prev2.low()) < 0
            && lowerShadow(prev).compareTo(prevBody) >= 0
            && curGreen
            && curBody.compareTo(avgBody.multiply(THRESHOLD_HALF, MC)) <= 0
            && cur.low().compareTo(prev.low()) > 0
            && cur.close().compareTo(prev.open()) <= 0;

      case THREE_STARS_IN_SOUTH:
        return prev2 != null
            && prev2Red
            && prev2Body.compareTo(avgBody) >= 0
            && lowerShadow(prev2).compareTo(prev2Body.multiply(THRESHOLD_HALF, MC)) >= 0
            && prevRed
            && prevBody.compareTo(prev2Body) < 0
            && prev.low().compareTo(prev2.low()) > 0
            && lowerShadow(prev).compareTo(prevBody.multiply(THRESHOLD_HALF, MC)) >= 0
            && curRed
            && curBody.compareTo(avgBody.multiply(THRESHOLD_VERY_SMALL, MC)) <= 0
            && lowerShadow(cur).compareTo(curBody.multiply(THRESHOLD_MINIMAL, MC)) <= 0
            && upperShadow(cur).compareTo(curBody.multiply(THRESHOLD_MINIMAL, MC)) <= 0;

      case CONCEALING_SWALLOW:
        return prev3 != null
            && prev3Red
            && prev3Body.compareTo(avgBody) >= 0
            && prev2Red
            && prev2Body.compareTo(avgBody) >= 0
            && lowerShadow(prev3).compareTo(prev3Body.multiply(THRESHOLD_MINIMAL, MC)) <= 0
            && upperShadow(prev3).compareTo(prev3Body.multiply(THRESHOLD_MINIMAL, MC)) <= 0
            && prevRed
            && prev.open().compareTo(prev2.close()) < 0
            && upperShadow(prev).compareTo(prevBody.multiply(THRESHOLD_HALF, MC)) >= 0
            && curRed
            && cur.open().compareTo(prev.high()) > 0
            && cur.close().compareTo(prev.low()) < 0;

      case BULLISH_STICK_SANDWICH:
        return prev2 != null
            && prev2Red
            && prevGreen
            && curRed
            && cur.close()
                    .subtract(prev2.close())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_TINY, MC))
                <= 0;

      case HOMING_PIGEON:
        return prev != null
            && prevRed
            && curRed
            && prevBody.compareTo(avgBody) >= 0
            && cur.open().compareTo(prev.open()) < 0
            && cur.close().compareTo(prev.close()) > 0;

      case LADDER_BOTTOM:
        return prev4 != null
            && prev4Red
            && prev3Red
            && prev2Red
            && prev2.close().compareTo(prev3.close()) < 0
            && prev3.close().compareTo(prev4.close()) < 0
            && prevRed
            && upperShadow(prev).compareTo(prevBody) >= 0
            && curGreen
            && cur.open().compareTo(prev.open()) > 0;

      case MATCHING_LOW:
        return prev != null
            && prevRed
            && curRed
            && cur.close()
                    .subtract(prev.close())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_TINY, MC))
                <= 0;

      // B. Bearish Reversals
      case LONG_BLACK_BODY:
        return curRed && curBody.compareTo(avgBody.multiply(THRESHOLD_LARGE, MC)) >= 0;

      case HANGING_MAN:
        return prev != null
            && prevBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) <= 0
            && lowerShadow(prev).compareTo(prevBody.multiply(RATIO_HAMMER_SHADOW, MC)) >= 0
            && upperShadow(prev).compareTo(prevBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0
            && curRed;

      case SHOOTING_STAR:
        return curBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) <= 0
            && upperShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_SHADOW, MC)) >= 0
            && lowerShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0;

      case BEARISH_BELT_HOLD:
        return curRed
            && upperShadow(cur).compareTo(curBody.multiply(THRESHOLD_TINY, MC)) <= 0
            && lowerShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0
            && curBody.compareTo(avgBody) >= 0;

      case BEARISH_ENGULFING:
        return prev != null
            && prevGreen
            && curRed
            && cur.open().compareTo(prev.close()) >= 0
            && cur.close().compareTo(prev.open()) <= 0
            && (cur.open().compareTo(prev.close()) > 0 || cur.close().compareTo(prev.open()) < 0);

      case BEARISH_HARAMI:
        return prev != null
            && prevGreen
            && prevBody.compareTo(avgBody) >= 0
            && curRed
            && curBody.compareTo(prevBody) < 0
            && cur.open().compareTo(prev.close()) <= 0
            && cur.close().compareTo(prev.open()) >= 0;

      case BEARISH_HARAMI_CROSS:
        return prev != null
            && prevGreen
            && prevBody.compareTo(avgBody) >= 0
            && curBody.compareTo(avgBody.multiply(THRESHOLD_DOJI, MC)) <= 0
            && cur.open().compareTo(prev.close()) <= 0
            && cur.close().compareTo(prev.open()) >= 0;

      case DARK_CLOUD_COVER:
        if (prev == null || !prevGreen || !curRed || prevBody.compareTo(avgBody) < 0) {
          return false;
        }
        BigDecimal darkCloudCoverMidpoint =
            prev.close().add(prev.open()).divide(DIVISOR_MIDPOINT, MC);
        return cur.open().compareTo(prev.high()) > 0
            && cur.close().compareTo(darkCloudCoverMidpoint) < 0
            && cur.close().compareTo(prev.open()) >= 0;

      case BEARISH_DOJI_STAR:
        return prev != null
            && prevGreen
            && prevBody.compareTo(avgBody) >= 0
            && curBody.compareTo(avgBody.multiply(THRESHOLD_DOJI, MC)) <= 0
            && cur.open().compareTo(prev.close()) > 0
            && cur.close().compareTo(prev.close()) > 0;

      case BEARISH_MEETING_LINES:
        return prev != null
            && prevGreen
            && curRed
            && prevBody.compareTo(avgBody) >= 0
            && curBody.compareTo(avgBody) >= 0
            && cur.open().compareTo(prev.high()) > 0
            && cur.close()
                    .subtract(prev.close())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_TINY, MC))
                <= 0;

      case THREE_BLACK_CROWS:
        return prev2 != null
            && curRed
            && prevRed
            && prev2Red
            && curBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) >= 0
            && prevBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) >= 0
            && prev2Body.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) >= 0
            && cur.open().compareTo(prev.open()) < 0
            && cur.open().compareTo(prev.close()) > 0
            && prev.open().compareTo(prev2.open()) < 0
            && prev.open().compareTo(prev2.close()) > 0
            && cur.close().compareTo(prev.close()) < 0
            && prev.close().compareTo(prev2.close()) < 0;

      case EVENING_STAR:
        if (prev2 == null
            || !prev2Green
            || prev2Body.compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) < 0) {
          return false;
        }
        if (prevBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) > 0) {
          return false;
        }
        BigDecimal eveningStarMin = prev.open().min(prev.close());
        boolean gapUpEveningStar = eveningStarMin.compareTo(prev2.close()) > 0;
        BigDecimal eveningStarMidpoint =
            prev2.close().add(prev2.open()).divide(DIVISOR_MIDPOINT, MC);
        return gapUpEveningStar
            && curRed
            && curBody.compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) >= 0
            && cur.close().compareTo(eveningStarMidpoint) < 0;

      case EVENING_DOJI_STAR:
        if (prev2 == null
            || !prev2Green
            || prev2Body.compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) < 0) {
          return false;
        }
        if (prevBody.compareTo(avgBody.multiply(THRESHOLD_DOJI, MC)) > 0) {
          return false;
        }
        BigDecimal eveningDojiStarMin = prev.open().min(prev.close());
        boolean gapUpEveningDojiStar = eveningDojiStarMin.compareTo(prev2.close()) > 0;
        BigDecimal eveningDojiStarMidpoint =
            prev2.close().add(prev2.open()).divide(DIVISOR_MIDPOINT, MC);
        return gapUpEveningDojiStar
            && curRed
            && curBody.compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) >= 0
            && cur.close().compareTo(eveningDojiStarMidpoint) < 0;

      case BEARISH_ABANDONED_BABY:
        return prev2 != null
            && prev2Green
            && curRed
            && prevBody.compareTo(avgBody.multiply(THRESHOLD_DOJI, MC)) <= 0
            && prev.low().compareTo(prev2.high()) > 0
            && prev.low().compareTo(cur.high()) > 0;

      case BEARISH_TRI_STAR:
        return prev2 != null
            && curBody.compareTo(avgBody.multiply(THRESHOLD_DOJI, MC)) <= 0
            && prevBody.compareTo(avgBody.multiply(THRESHOLD_DOJI, MC)) <= 0
            && prev2Body.compareTo(avgBody.multiply(THRESHOLD_DOJI, MC)) <= 0
            && prev.open().compareTo(prev2.close()) > 0
            && prev.open().compareTo(cur.open()) > 0;

      case BEARISH_BREAKAWAY:
        return prev4 != null
            && prev4Green
            && prev4Body.compareTo(avgBody) >= 0
            && prev3Green
            && prev3.low().compareTo(prev4.high()) > 0
            && prev2Green
            && prevGreen
            && prev2.close().compareTo(prev3.close()) > 0
            && prev.close().compareTo(prev2.close()) > 0
            && curRed
            && curBody.compareTo(avgBody) >= 0
            && cur.close().compareTo(prev4.high()) > 0
            && cur.close().compareTo(prev3.open()) < 0;

      case THREE_INSIDE_DOWN:
        return prev2 != null
            && prev2Green
            && prev2Body.compareTo(avgBody) >= 0
            && isRed(prev)
            && prevBody.compareTo(prev2Body) < 0
            && prev.open().compareTo(prev2.close()) <= 0
            && prev.close().compareTo(prev2.open()) >= 0
            && curRed
            && cur.close().compareTo(prev.low()) < 0;

      case THREE_OUTSIDE_DOWN:
        return prev2 != null
            && prev2Green
            && isRed(prev)
            && prev.open().compareTo(prev2.close()) >= 0
            && prev.close().compareTo(prev2.open()) <= 0
            && curRed
            && cur.close().compareTo(prev.close()) < 0;

      case BEARISH_KICKING:
        return prev != null
            && prevGreen
            && prevBody.compareTo(avgBody) >= 0
            && lowerShadow(prev).compareTo(prevBody.multiply(THRESHOLD_TINY, MC)) <= 0
            && upperShadow(prev).compareTo(prevBody.multiply(THRESHOLD_TINY, MC)) <= 0
            && curRed
            && curBody.compareTo(avgBody) >= 0
            && lowerShadow(cur).compareTo(curBody.multiply(THRESHOLD_TINY, MC)) <= 0
            && upperShadow(cur).compareTo(curBody.multiply(THRESHOLD_TINY, MC)) <= 0
            && cur.open().compareTo(prev.open()) < 0;

      case LADDER_TOP:
        return prev4 != null
            && prev4Green
            && prev3Green
            && prev2Green
            && prev2.close().compareTo(prev3.close()) > 0
            && prev3.close().compareTo(prev4.close()) > 0
            && prevGreen
            && upperShadow(prev).compareTo(prevBody) >= 0
            && curRed
            && cur.open().compareTo(prev.open()) < 0;

      case MATCHING_HIGH:
        return prev != null
            && prevGreen
            && curGreen
            && cur.close()
                    .subtract(prev.close())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_TINY, MC))
                <= 0;

      case UPSIDE_GAP_TWO_CROWS:
        return prev2 != null
            && prev2Green
            && prev2Body.compareTo(avgBody) >= 0
            && prevRed
            && prev.open().min(prev.close()).compareTo(prev2.close()) > 0
            && curRed
            && cur.open().compareTo(prev.open()) > 0
            && cur.close().compareTo(prev.close()) < 0
            && cur.close().compareTo(prev2.close()) > 0;

      case IDENTICAL_THREE_CROWS:
        return prev2 != null
            && curRed
            && prevRed
            && prev2Red
            && cur.open()
                    .subtract(prev.close())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_MINIMAL, MC))
                <= 0
            && prev.open()
                    .subtract(prev2.close())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_MINIMAL, MC))
                <= 0;

      case DELIBERATION:
        return prev2 != null
            && prev2Green
            && prev2Body.compareTo(avgBody) >= 0
            && prevGreen
            && prevBody.compareTo(avgBody) >= 0
            && curGreen
            && curBody.compareTo(avgBody.multiply(THRESHOLD_HALF, MC)) <= 0
            && cur.open().compareTo(prev.close()) > 0;

      case ADVANCE_BLOCK:
        return prev2 != null
            && curGreen
            && prevGreen
            && prev2Green
            && curBody.compareTo(prevBody) < 0
            && prevBody.compareTo(prev2Body) < 0
            && upperShadow(cur).compareTo(upperShadow(prev)) > 0
            && upperShadow(prev).compareTo(upperShadow(prev2)) > 0;

      case TWO_CROWS:
        return prev2 != null
            && prev2Green
            && prev2Body.compareTo(avgBody) >= 0
            && isRed(prev)
            && prev.low().compareTo(prev2.high()) > 0
            && curRed
            && cur.open().compareTo(prev.close()) >= 0
            && cur.open().compareTo(prev.open()) <= 0
            && cur.close().compareTo(prev2.close()) < 0
            && cur.close().compareTo(prev2.open()) > 0;

      // C. Bullish Continuation
      case BULLISH_SEPARATING_LINES:
        return prev != null
            && prevRed
            && curGreen
            && cur.open()
                    .subtract(prev.open())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_TINY, MC))
                <= 0;

      case RISING_THREE_METHODS:
        return prev4 != null
            && prev4Green
            && prev4Body.compareTo(avgBody) >= 0
            && prev3Red
            && prev2Red
            && prevRed
            && curGreen
            && curBody.compareTo(avgBody) >= 0
            && prev3.high().compareTo(prev4.high()) < 0
            && prev3.low().compareTo(prev4.low()) > 0
            && prev2.high().compareTo(prev4.high()) < 0
            && prev2.low().compareTo(prev4.low()) > 0
            && prev.high().compareTo(prev4.high()) < 0
            && prev.low().compareTo(prev4.low()) > 0
            && cur.close().compareTo(prev4.high()) > 0;

      case UPSIDE_TASUKI_GAP:
        return prev2 != null
            && prev2Green
            && prevGreen
            && prev.open().compareTo(prev2.close()) > 0
            && curRed
            && cur.open().compareTo(prev.close()) < 0
            && cur.open().compareTo(prev.open()) > 0
            && cur.close().compareTo(prev2.close()) > 0
            && cur.close().compareTo(prev.open()) < 0;

      case BULLISH_SIDE_BY_SIDE_WHITE_LINES:
        return prev2 != null
            && prev2Green
            && prevGreen
            && curGreen
            && prev.open().compareTo(prev2.close()) > 0
            && cur.open().compareTo(prev2.close()) > 0
            && prev.open()
                    .subtract(cur.open())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_MINIMAL, MC))
                <= 0
            && prev.close()
                    .subtract(cur.close())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_MINIMAL, MC))
                <= 0;

      case BULLISH_THREE_LINE_STRIKE:
        return prev3 != null
            && prev3Green
            && prev2Green
            && prevGreen
            && prev2.close().compareTo(prev3.close()) > 0
            && prev.close().compareTo(prev2.close()) > 0
            && curRed
            && cur.open().compareTo(prev.close()) >= 0
            && cur.close().compareTo(prev3.open()) <= 0;

      case UPSIDE_GAP_THREE_METHODS:
        return prev2 != null
            && prev2Green
            && prevGreen
            && prev.open().compareTo(prev2.close()) > 0
            && curRed
            && cur.open().compareTo(prev.close()) < 0
            && cur.close().compareTo(prev2.close()) <= 0;

      case BULLISH_ON_NECK_LINE:
        return prev != null
            && prevRed
            && curGreen
            && cur.open().compareTo(prev.low()) < 0
            && cur.close()
                    .subtract(prev.low())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_TINY, MC))
                <= 0;

      case BULLISH_IN_NECK_LINE:
        return prev != null
            && prevRed
            && curGreen
            && cur.open().compareTo(prev.low()) < 0
            && cur.close()
                    .subtract(prev.close())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_TINY, MC))
                <= 0;

      // D. Bearish Continuation
      case BEARISH_SEPARATING_LINES:
        return prev != null
            && prevGreen
            && curRed
            && cur.open()
                    .subtract(prev.open())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_TINY, MC))
                <= 0;

      case FALLING_THREE_METHODS:
        return prev4 != null
            && prev4Red
            && prev4Body.compareTo(avgBody) >= 0
            && prev3Green
            && prev2Green
            && prevGreen
            && curRed
            && curBody.compareTo(avgBody) >= 0
            && prev3.high().compareTo(prev4.high()) < 0
            && prev3.low().compareTo(prev4.low()) > 0
            && prev2.high().compareTo(prev4.high()) < 0
            && prev2.low().compareTo(prev4.low()) > 0
            && prev.high().compareTo(prev4.high()) < 0
            && prev.low().compareTo(prev4.low()) > 0
            && cur.close().compareTo(prev4.low()) < 0;

      case DOWNSIDE_TASUKI_GAP:
        return prev2 != null
            && prev2Red
            && prevRed
            && prev.open().compareTo(prev2.close()) < 0
            && curGreen
            && cur.open().compareTo(prev.close()) > 0
            && cur.open().compareTo(prev.open()) < 0
            && cur.close().compareTo(prev2.close()) < 0
            && cur.close().compareTo(prev.open()) > 0;

      case BEARISH_SIDE_BY_SIDE_WHITE_LINES:
        return prev2 != null
            && prev2Red
            && prevGreen
            && curGreen
            && prev.close().compareTo(prev2.low()) < 0
            && cur.close().compareTo(prev2.low()) < 0
            && prev.open()
                    .subtract(cur.open())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_MINIMAL, MC))
                <= 0
            && prev.close()
                    .subtract(cur.close())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_MINIMAL, MC))
                <= 0;

      case BEARISH_THREE_LINE_STRIKE:
        return prev3 != null
            && prev3Red
            && prev2Red
            && prevRed
            && prev2.close().compareTo(prev3.close()) < 0
            && prev.close().compareTo(prev2.close()) < 0
            && curGreen
            && cur.open().compareTo(prev.close()) <= 0
            && cur.close().compareTo(prev3.open()) >= 0;

      case DOWNSIDE_GAP_THREE_METHODS:
        return prev2 != null
            && prev2Red
            && prevRed
            && prev.open().compareTo(prev2.close()) < 0
            && curGreen
            && cur.open().compareTo(prev.close()) > 0
            && cur.close().compareTo(prev2.close()) >= 0;

      case BEARISH_ON_NECK_LINE:
        return prev != null
            && prevRed
            && prevBody.compareTo(avgBody) >= 0
            && curGreen
            && cur.open().compareTo(prev.low()) < 0
            && cur.close()
                    .subtract(prev.low())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_TINY, MC))
                <= 0;

      case BEARISH_IN_NECK_LINE:
        return prev != null
            && prevRed
            && prevBody.compareTo(avgBody) >= 0
            && curGreen
            && cur.open().compareTo(prev.low()) < 0
            && cur.close().compareTo(prev.close()) >= 0
            && cur.close()
                    .subtract(prev.close())
                    .abs()
                    .compareTo(avgBody.multiply(THRESHOLD_TINY, MC))
                <= 0;

      default:
        return false;
    }
  }

  private BigDecimal body(PriceBar bar) {
    return bar.close().subtract(bar.open(), MC).abs();
  }

  private boolean isGreen(PriceBar bar) {
    return bar.close().compareTo(bar.open()) > 0;
  }

  private boolean isRed(PriceBar bar) {
    return bar.close().compareTo(bar.open()) < 0;
  }

  private BigDecimal upperShadow(PriceBar bar) {
    BigDecimal bodyMax = bar.open().max(bar.close());
    return bar.high().subtract(bodyMax, MC);
  }

  private BigDecimal lowerShadow(PriceBar bar) {
    BigDecimal bodyMin = bar.open().min(bar.close());
    return bodyMin.subtract(bar.low(), MC);
  }
}
