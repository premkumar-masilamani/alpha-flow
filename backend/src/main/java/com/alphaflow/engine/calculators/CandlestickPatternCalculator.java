package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.dtos.PatternMatch;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import com.alphaflow.persistence.entities.DailyCandlestickPattern;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyCandlestickPattern;
import com.alphaflow.persistence.entities.WeeklyPrice;
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
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class CandlestickPatternCalculator {

  private static final MathContext MC = new MathContext(18, RoundingMode.HALF_UP);
  private static final BigDecimal THRESHOLD_LARGE = new BigDecimal("1.5");
  private static final BigDecimal THRESHOLD_SMALL = new BigDecimal("0.3");
  private static final BigDecimal RATIO_HAMMER_SHADOW = new BigDecimal("2.0");
  private static final BigDecimal RATIO_HAMMER_UPPER = new BigDecimal("0.1");
  private static final BigDecimal RATIO_STAR_OUTER = new BigDecimal("0.7");
  private static final BigDecimal DIVISOR_MIDPOINT = new BigDecimal("2");
  private static final int MIN_BARS = 5;
  private static final int LOOKBACK_BARS = MIN_BARS - 1;
  private static final int PERIOD_BODY_MA = 14;

  private final TickerRepository tickerRepository;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final DailyCandlestickPatternRepository dailyCandlestickPatternRepository;
  private final WeeklyCandlestickPatternRepository weeklyCandlestickPatternRepository;

  @Autowired @Lazy private CandlestickPatternCalculator selfProxy;

  public CandlestickPatternCalculator(
      TickerRepository tickerRepository,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailyCandlestickPatternRepository dailyCandlestickPatternRepository,
      WeeklyCandlestickPatternRepository weeklyCandlestickPatternRepository) {
    this.tickerRepository = tickerRepository;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailyCandlestickPatternRepository = dailyCandlestickPatternRepository;
    this.weeklyCandlestickPatternRepository = weeklyCandlestickPatternRepository;
  }

  public void computeCandleStickPatterns() {
    log.info("Computing candlestick patterns...");
    List<Ticker> tickers = tickerRepository.findByIsActiveTrue();
    log.info("Found {} active tickers to process for patterns.", tickers.size());

    CandlestickPatternCalculator proxy = (selfProxy != null) ? selfProxy : this;

    for (Ticker ticker : tickers) {
      try {
        proxy.computeCandleStickPatternsForTicker(ticker);
      } catch (Exception e) {
        log.error(
            "Failed to compute candlestick patterns for ticker {}: {}",
            ticker.getTickerSymbol(),
            e.getMessage(),
            e);
      }
    }
    log.info("Candlestick pattern computed.");
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void computeCandleStickPatternsForTicker(Ticker ticker) {
    // Step 1: Load the bars (daily and weekly)
    List<PriceBar> dailyBars = loadBars(ticker, Timeframe.DAILY);
    List<PriceBar> weeklyBars = loadBars(ticker, Timeframe.WEEKLY);

    // Step 2: Compute the patterns (common)
    LocalDate dailyRecomputeStartDate = null;
    List<PatternMatch> dailyMatches = List.of();
    if (dailyBars.size() < MIN_BARS) {
      log.debug(
          "Ticker {}: Insufficient daily data points (found {}) to compute patterns.",
          ticker.getTickerSymbol(),
          dailyBars.size());
    } else {
      log.info(
          "Ticker {}: Timeframe {} - Calculating...", ticker.getTickerSymbol(), Timeframe.DAILY);
      LocalDate lastDailyDate = getLastComputedDate(ticker, Timeframe.DAILY);
      dailyRecomputeStartDate = calculateRecomputeStartDate(dailyBars, lastDailyDate);
      dailyMatches = computePatterns(dailyBars, lastDailyDate);
    }

    LocalDate weeklyRecomputeStartDate = null;
    List<PatternMatch> weeklyMatches = List.of();
    if (weeklyBars.size() < MIN_BARS) {
      log.debug(
          "Ticker {}: Insufficient weekly data points (found {}) to compute patterns.",
          ticker.getTickerSymbol(),
          weeklyBars.size());
    } else {
      log.info(
          "Ticker {}: Timeframe {} - Calculating...", ticker.getTickerSymbol(), Timeframe.WEEKLY);
      LocalDate lastWeeklyDate = getLastComputedDate(ticker, Timeframe.WEEKLY);
      weeklyRecomputeStartDate = calculateRecomputeStartDate(weeklyBars, lastWeeklyDate);
      weeklyMatches = computePatterns(weeklyBars, lastWeeklyDate);
    }

    // Step 3: Save the computed patterns (daily and weekly)
    if (dailyBars.size() >= MIN_BARS) {
      savePatterns(ticker, Timeframe.DAILY, dailyMatches, dailyRecomputeStartDate);
    }
    if (weeklyBars.size() >= MIN_BARS) {
      savePatterns(ticker, Timeframe.WEEKLY, weeklyMatches, weeklyRecomputeStartDate);
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
    }
    log.error("Unsupported timeframe for loading bars: {}", timeframe);
    throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
  }

  private LocalDate getLastComputedDate(Ticker ticker, Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyCandlestickPatternRepository
          .findFirstByTickerOrderByPriceDateDesc(ticker)
          .map(DailyCandlestickPattern::getPriceDate)
          .orElse(null);
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyCandlestickPatternRepository
          .findFirstByTickerOrderByPriceDateDesc(ticker)
          .map(WeeklyCandlestickPattern::getPriceDate)
          .orElse(null);
    }
    log.error("Unsupported timeframe for fetching last computed date: {}", timeframe);
    throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
  }

  private List<PatternMatch> computePatterns(List<PriceBar> bars, LocalDate lastComputedDate) {
    if (bars.size() < MIN_BARS) {
      return Collections.emptyList();
    }
    int startIndex = calculateStartIndex(bars, lastComputedDate);
    return findMatches(bars, startIndex);
  }

  private int calculateStartIndex(List<PriceBar> bars, LocalDate lastComputedDate) {
    int lastIndex = findDateIndex(bars, lastComputedDate);
    return lastIndex != -1 ? Math.max(LOOKBACK_BARS, lastIndex - LOOKBACK_BARS) : LOOKBACK_BARS;
  }

  private LocalDate calculateRecomputeStartDate(List<PriceBar> bars, LocalDate lastComputedDate) {
    int lastIndex = findDateIndex(bars, lastComputedDate);
    return lastIndex != -1
        ? bars.get(Math.max(LOOKBACK_BARS, lastIndex - LOOKBACK_BARS)).date()
        : null;
  }

  private int findDateIndex(List<PriceBar> bars, LocalDate date) {
    if (date == null) {
      return -1;
    }
    for (int k = 0; k < bars.size(); k++) {
      if (bars.get(k).date().equals(date)) {
        return k;
      }
    }
    return -1;
  }

  private void savePatterns(
      Ticker ticker,
      Timeframe timeframe,
      List<PatternMatch> matches,
      LocalDate recomputeStartDate) {
    if (recomputeStartDate != null) {
      if (timeframe == Timeframe.DAILY) {
        dailyCandlestickPatternRepository.deleteByTickerAndPriceDateGreaterThanEqual(
            ticker, recomputeStartDate);
        dailyCandlestickPatternRepository.flush();
      } else if (timeframe == Timeframe.WEEKLY) {
        weeklyCandlestickPatternRepository.deleteByTickerAndPriceDateGreaterThanEqual(
            ticker, recomputeStartDate);
        weeklyCandlestickPatternRepository.flush();
      } else {
        log.error("Unsupported timeframe for cleaning up patterns: {}", timeframe);
        throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
      }
      log.info(
          "Ticker {}: Cleaned up {} patterns on or after {} for recomputation.",
          ticker.getTickerSymbol(),
          timeframe.name().toLowerCase(),
          recomputeStartDate);
    }

    if (!matches.isEmpty()) {
      if (timeframe == Timeframe.DAILY) {
        List<DailyCandlestickPattern> newPatterns =
            matches.stream()
                .map(
                    m ->
                        DailyCandlestickPattern.builder()
                            .ticker(ticker)
                            .priceDate(m.date())
                            .pattern(m.pattern())
                            .sentiment(m.pattern().getSentiment())
                            .build())
                .toList();
        dailyCandlestickPatternRepository.saveAll(newPatterns);
      } else if (timeframe == Timeframe.WEEKLY) {
        List<WeeklyCandlestickPattern> newPatterns =
            matches.stream()
                .map(
                    m ->
                        WeeklyCandlestickPattern.builder()
                            .ticker(ticker)
                            .priceDate(m.date())
                            .pattern(m.pattern())
                            .sentiment(m.pattern().getSentiment())
                            .build())
                .toList();
        weeklyCandlestickPatternRepository.saveAll(newPatterns);
      } else {
        log.error("Unsupported timeframe for saving patterns: {}", timeframe);
        throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
      }
      log.info(
          "Ticker {}: Saved {} {} candlestick patterns (including recomputed window).",
          ticker.getTickerSymbol(),
          matches.size(),
          timeframe.name().toLowerCase());
    }
  }

  private List<PatternMatch> findMatches(List<PriceBar> bars, int startIndex) {
    List<PatternMatch> matches = new ArrayList<>();
    List<BigDecimal> bodies = computeAbsoluteBodies(bars);
    List<BigDecimal> avgBodies = computeMovingAverages(bodies, PERIOD_BODY_MA);

    for (int i = startIndex; i < bars.size(); i++) {
      PriceBar bar = bars.get(i);
      LocalDate date = bar.date();
      BigDecimal avgBody = avgBodies.get(i);

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
            && lowerShadow(cur).compareTo(curBody.multiply(new BigDecimal("0.05"), MC)) <= 0
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
            && curBody.compareTo(prevBody) < 0
            && cur.open().compareTo(prev.close()) >= 0
            && cur.close().compareTo(prev.open()) <= 0;

      case BULLISH_HARAMI_CROSS:
        return prev != null
            && prevRed
            && curBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) <= 0
            && cur.open().compareTo(prev.close()) >= 0
            && cur.close().compareTo(prev.open()) <= 0;

      case PIERCING_LINE:
        if (prev == null || !prevRed || !curGreen || prevBody.compareTo(avgBody) < 0) {
          return false;
        }
        BigDecimal midpointPL = prev.close().add(prev.open()).divide(DIVISOR_MIDPOINT, MC);
        return cur.open().compareTo(prev.close()) < 0
            && cur.close().compareTo(midpointPL) > 0
            && cur.close().compareTo(prev.open()) <= 0;

      case BULLISH_DOJI_STAR:
        return prev != null
            && prevRed
            && prevBody.compareTo(avgBody) >= 0
            && curBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) <= 0
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
                    .compareTo(avgBody.multiply(new BigDecimal("0.1"), MC))
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
        BigDecimal starMaxMS = prev.open().max(prev.close());
        boolean gapDownMS = starMaxMS.compareTo(prev2.close()) < 0;
        BigDecimal midpointMS = prev2.close().add(prev2.open()).divide(DIVISOR_MIDPOINT, MC);
        return gapDownMS
            && curGreen
            && curBody.compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) >= 0
            && cur.close().compareTo(midpointMS) > 0;

      case MORNING_DOJI_STAR:
        if (prev2 == null
            || !prev2Red
            || prev2Body.compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) < 0) {
          return false;
        }
        if (prevBody.compareTo(avgBody.multiply(new BigDecimal("0.1"), MC)) > 0) {
          return false;
        }
        BigDecimal starMaxMDS = prev.open().max(prev.close());
        boolean gapDownMDS = starMaxMDS.compareTo(prev2.close()) < 0;
        BigDecimal midpointMDS = prev2.close().add(prev2.open()).divide(DIVISOR_MIDPOINT, MC);
        return gapDownMDS
            && curGreen
            && curBody.compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) >= 0
            && cur.close().compareTo(midpointMDS) > 0;

      case BULLISH_ABANDONED_BABY:
        return prev2 != null
            && prev2Red
            && curGreen
            && prevBody.compareTo(avgBody.multiply(new BigDecimal("0.1"), MC)) <= 0
            && prev.high().compareTo(prev2.low()) < 0
            && prev.high().compareTo(cur.low()) < 0;

      case BULLISH_TRI_STAR:
        return prev2 != null
            && curBody.compareTo(avgBody.multiply(new BigDecimal("0.1"), MC)) <= 0
            && prevBody.compareTo(avgBody.multiply(new BigDecimal("0.1"), MC)) <= 0
            && prev2Body.compareTo(avgBody.multiply(new BigDecimal("0.1"), MC)) <= 0
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
            && cur.close().compareTo(prev2.open()) > 0;

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
            && curGreen
            && curBody.compareTo(avgBody) >= 0
            && cur.open().compareTo(prev.open()) > 0;

      case UNIQUE_THREE_RIVERS_BOTTOM:
        return prev2 != null
            && prev2Red
            && prev2Body.compareTo(avgBody) >= 0
            && prevRed
            && prev.close().compareTo(prev2.close()) > 0
            && prev.low().compareTo(prev2.low()) < 0
            && curBody.compareTo(avgBody) < 0
            && lowerShadow(cur).compareTo(curBody.multiply(new BigDecimal("0.1"), MC)) <= 0
            && cur.close().compareTo(prev.low()) > 0;

      case THREE_STARS_IN_SOUTH:
        return prev2 != null
            && prev2Red
            && prev2Body.compareTo(avgBody) >= 0
            && lowerShadow(prev2).compareTo(prev2Body.multiply(new BigDecimal("0.5"), MC)) >= 0
            && prevRed
            && prevBody.compareTo(prev2Body) < 0
            && prev.low().compareTo(prev2.low()) > 0
            && lowerShadow(prev).compareTo(prevBody.multiply(new BigDecimal("0.5"), MC)) >= 0
            && curRed
            && curBody.compareTo(avgBody.multiply(new BigDecimal("0.2"), MC)) <= 0
            && lowerShadow(cur).compareTo(curBody.multiply(new BigDecimal("0.1"), MC)) <= 0
            && upperShadow(cur).compareTo(curBody.multiply(new BigDecimal("0.1"), MC)) <= 0;

      case CONCEALING_SWALLOW:
        return prev3 != null
            && prev3Red
            && prev3Body.compareTo(avgBody) >= 0
            && prev2Red
            && prev2Body.compareTo(avgBody) >= 0
            && lowerShadow(prev3).compareTo(prev3Body.multiply(new BigDecimal("0.1"), MC)) <= 0
            && upperShadow(prev3).compareTo(prev3Body.multiply(new BigDecimal("0.1"), MC)) <= 0
            && prevRed
            && prev.open().compareTo(prev2.close()) < 0
            && upperShadow(prev).compareTo(prevBody.multiply(new BigDecimal("0.5"), MC)) >= 0
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
                    .compareTo(avgBody.multiply(new BigDecimal("0.1"), MC))
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
                    .compareTo(avgBody.multiply(new BigDecimal("0.05"), MC))
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
            && upperShadow(cur).compareTo(curBody.multiply(new BigDecimal("0.05"), MC)) <= 0
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
            && curBody.compareTo(prevBody) < 0
            && cur.open().compareTo(prev.close()) <= 0
            && cur.close().compareTo(prev.open()) >= 0;

      case BEARISH_HARAMI_CROSS:
        return prev != null
            && prevGreen
            && curBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) <= 0
            && cur.open().compareTo(prev.close()) <= 0
            && cur.close().compareTo(prev.open()) >= 0;

      case DARK_CLOUD_COVER:
        if (prev == null || !prevGreen || !curRed || prevBody.compareTo(avgBody) < 0) {
          return false;
        }
        BigDecimal midpointDCC = prev.close().add(prev.open()).divide(DIVISOR_MIDPOINT, MC);
        return cur.open().compareTo(prev.close()) > 0
            && cur.close().compareTo(midpointDCC) < 0
            && cur.close().compareTo(prev.open()) >= 0;

      case BEARISH_DOJI_STAR:
        return prev != null
            && prevGreen
            && prevBody.compareTo(avgBody) >= 0
            && curBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) <= 0
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
                    .compareTo(avgBody.multiply(new BigDecimal("0.1"), MC))
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
        BigDecimal starMinES = prev.open().min(prev.close());
        boolean gapUpES = starMinES.compareTo(prev2.close()) > 0;
        BigDecimal midpointES = prev2.close().add(prev2.open()).divide(DIVISOR_MIDPOINT, MC);
        return gapUpES
            && curRed
            && curBody.compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) >= 0
            && cur.close().compareTo(midpointES) < 0;

      case EVENING_DOJI_STAR:
        if (prev2 == null
            || !prev2Green
            || prev2Body.compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) < 0) {
          return false;
        }
        if (prevBody.compareTo(avgBody.multiply(new BigDecimal("0.1"), MC)) > 0) {
          return false;
        }
        BigDecimal starMinEDS = prev.open().min(prev.close());
        boolean gapUpEDS = starMinEDS.compareTo(prev2.close()) > 0;
        BigDecimal midpointEDS = prev2.close().add(prev2.open()).divide(DIVISOR_MIDPOINT, MC);
        return gapUpEDS
            && curRed
            && curBody.compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) >= 0
            && cur.close().compareTo(midpointEDS) < 0;

      case BEARISH_ABANDONED_BABY:
        return prev2 != null
            && prev2Green
            && curRed
            && prevBody.compareTo(avgBody.multiply(new BigDecimal("0.1"), MC)) <= 0
            && prev.low().compareTo(prev2.high()) > 0
            && prev.low().compareTo(cur.high()) > 0;

      case BEARISH_TRI_STAR:
        return prev2 != null
            && curBody.compareTo(avgBody.multiply(new BigDecimal("0.1"), MC)) <= 0
            && prevBody.compareTo(avgBody.multiply(new BigDecimal("0.1"), MC)) <= 0
            && prev2Body.compareTo(avgBody.multiply(new BigDecimal("0.1"), MC)) <= 0
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
            && cur.close().compareTo(prev2.open()) < 0;

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
            && curRed
            && curBody.compareTo(avgBody) >= 0
            && cur.open().compareTo(prev.open()) < 0;

      case LATTER_TOP:
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
                    .compareTo(avgBody.multiply(new BigDecimal("0.05"), MC))
                <= 0;

      case UPSIDE_GAP_TWO_CROWS:
        return prev2 != null
            && prev2Green
            && prev2Body.compareTo(avgBody) >= 0
            && prevRed
            && prev.low().compareTo(prev2.high()) > 0
            && curRed
            && cur.open().compareTo(prev.open()) > 0
            && cur.close().compareTo(prev2.close()) < 0
            && cur.close().compareTo(prev2.open()) > 0;

      case IDENTICAL_THREE_CROWS:
        return prev2 != null
            && curRed
            && prevRed
            && prev2Red
            && cur.open()
                    .subtract(prev.close())
                    .abs()
                    .compareTo(avgBody.multiply(new BigDecimal("0.1"), MC))
                <= 0
            && prev.open()
                    .subtract(prev2.close())
                    .abs()
                    .compareTo(avgBody.multiply(new BigDecimal("0.1"), MC))
                <= 0;

      case DELIBERATION:
        return prev2 != null
            && prev2Green
            && prev2Body.compareTo(avgBody) >= 0
            && prevGreen
            && prevBody.compareTo(avgBody) >= 0
            && curGreen
            && curBody.compareTo(avgBody.multiply(new BigDecimal("0.5"), MC)) <= 0
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
            && isRed(prev)
            && prev.low().compareTo(prev2.high()) > 0
            && curRed
            && cur.close().compareTo(prev2.close()) < 0
            && cur.close().compareTo(prev2.open()) > 0
            && cur.open().compareTo(prev.close()) < 0;

      // C. Bullish Continuation
      case BULLISH_SEPARATING_LINES:
        return prev != null
            && prevRed
            && curGreen
            && cur.open()
                    .subtract(prev.open())
                    .abs()
                    .compareTo(avgBody.multiply(new BigDecimal("0.05"), MC))
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
            && cur.close().compareTo(prev4.close()) > 0;

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
                    .compareTo(avgBody.multiply(new BigDecimal("0.1"), MC))
                <= 0
            && prev.close()
                    .subtract(cur.close())
                    .abs()
                    .compareTo(avgBody.multiply(new BigDecimal("0.1"), MC))
                <= 0;

      case BULLISH_THREE_LINE_STRIKE:
        return prev3 != null
            && prev3Green
            && prev2Green
            && prevGreen
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
                    .compareTo(avgBody.multiply(new BigDecimal("0.05"), MC))
                <= 0;

      case BULLISH_IN_NECK_LINE:
        return prev != null
            && prevRed
            && curGreen
            && cur.open().compareTo(prev.low()) < 0
            && cur.close()
                    .subtract(prev.close())
                    .abs()
                    .compareTo(avgBody.multiply(new BigDecimal("0.05"), MC))
                <= 0;

      // D. Bearish Continuation
      case BEARISH_SEPARATING_LINES:
        return prev != null
            && prevGreen
            && curRed
            && cur.open()
                    .subtract(prev.open())
                    .abs()
                    .compareTo(avgBody.multiply(new BigDecimal("0.05"), MC))
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
            && cur.close().compareTo(prev4.close()) < 0;

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
                    .compareTo(avgBody.multiply(new BigDecimal("0.1"), MC))
                <= 0
            && prev.close()
                    .subtract(cur.close())
                    .abs()
                    .compareTo(avgBody.multiply(new BigDecimal("0.1"), MC))
                <= 0;

      case BEARISH_THREE_LINE_STRIKE:
        return prev3 != null
            && prev3Red
            && prev2Red
            && prevRed
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
            && prevGreen
            && curRed
            && cur.open().compareTo(prev.high()) > 0
            && cur.close()
                    .subtract(prev.high())
                    .abs()
                    .compareTo(avgBody.multiply(new BigDecimal("0.05"), MC))
                <= 0;

      case BEARISH_IN_NECK_LINE:
        return prev != null
            && prevGreen
            && curRed
            && cur.open().compareTo(prev.high()) > 0
            && cur.close()
                    .subtract(prev.close())
                    .abs()
                    .compareTo(avgBody.multiply(new BigDecimal("0.05"), MC))
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

  private List<BigDecimal> computeAbsoluteBodies(List<PriceBar> bars) {
    return bars.stream().map(this::body).toList();
  }

  private List<BigDecimal> computeMovingAverages(List<BigDecimal> values, int period) {
    List<BigDecimal> ma = new ArrayList<>(Collections.nCopies(values.size(), BigDecimal.ZERO));
    BigDecimal sum = BigDecimal.ZERO;

    for (int i = 0; i < values.size(); i++) {
      sum = sum.add(values.get(i));
      if (i >= period) {
        sum = sum.subtract(values.get(i - period));
      }
      int count = Math.min(i + 1, period);
      ma.set(i, IndicatorMath.divide(sum, BigDecimal.valueOf(count)));
    }
    return ma;
  }

  private PriceBar toPriceBar(DailyPrice d) {
    return new PriceBar(
        d.getPriceDate(),
        d.getPriceOpen(),
        d.getPriceHigh(),
        d.getPriceLow(),
        d.getPriceClose(),
        d.getVolume());
  }

  private PriceBar toPriceBar(WeeklyPrice w) {
    return new PriceBar(
        w.getPriceDate(),
        w.getPriceOpen(),
        w.getPriceHigh(),
        w.getPriceLow(),
        w.getPriceClose(),
        w.getVolume());
  }
}
