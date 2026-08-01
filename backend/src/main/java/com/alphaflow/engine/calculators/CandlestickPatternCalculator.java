package com.alphaflow.engine.calculators;

import com.alphaflow.engine.indicators.dtos.PriceBar;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes candlestick patterns for all active tickers. Follows the same transactional boundaries
 * and proxy pattern as other engine calculators.
 */
@Component
@Slf4j
public class CandlestickPatternCalculator {

  private static final MathContext MC = new MathContext(18, RoundingMode.HALF_UP);
  private static final BigDecimal THRESHOLD_LARGE = new BigDecimal("1.5");
  private static final BigDecimal THRESHOLD_SMALL = new BigDecimal("0.3");
  private static final BigDecimal RATIO_HAMMER_SHADOW = new BigDecimal("2.0");
  private static final BigDecimal RATIO_HAMMER_UPPER = new BigDecimal("0.1");

  private final TickerRepository tickerRepository;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final DailyCandlestickPatternRepository dailyCandlestickPatternRepository;
  private final WeeklyCandlestickPatternRepository weeklyCandlestickPatternRepository;

  @Autowired @Lazy private CandlestickPatternCalculator selfProxy;

  /**
   * Constructs a CandlestickPatternCalculator.
   *
   * @param tickerRepository the ticker repository
   * @param dailyPriceRepository the daily prices repository
   * @param weeklyPriceRepository the weekly prices repository
   * @param dailyCandlestickPatternRepository the daily candlestick patterns repository
   * @param weeklyCandlestickPatternRepository the weekly candlestick patterns repository
   */
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

  /** Main entry point to compute patterns across all active tickers. */
  public void computePatterns() {
    log.info("Starting candlestick pattern computation...");
    List<Ticker> tickers = tickerRepository.findByIsActiveTrue();
    log.info("Found {} active tickers to process for patterns.", tickers.size());

    CandlestickPatternCalculator proxy = (selfProxy != null) ? selfProxy : this;

    for (Ticker ticker : tickers) {
      try {
        proxy.computePatternsForTicker(ticker);
      } catch (Exception e) {
        log.error(
            "Failed to compute candlestick patterns for ticker {}: {}",
            ticker.getTickerSymbol(),
            e.getMessage(),
            e);
      }
    }
    log.info("Candlestick pattern computation completed.");
  }

  /**
   * Computes daily and weekly patterns for a specific ticker in separate transaction boundaries.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void computePatternsForTicker(Ticker ticker) {
    log.info("Ticker {}: Starting pattern calculation...", ticker.getTickerSymbol());
    computeDailyPatternsForTicker(ticker);
    computeWeeklyPatternsForTicker(ticker);
  }

  private record PatternMatch(LocalDate date, CandlestickPattern pattern) {}

  private void computeDailyPatternsForTicker(Ticker ticker) {
    List<PriceBar> bars = loadDailyBars(ticker);
    if (bars.size() < 3) {
      log.debug(
          "Ticker {}: Insufficient daily data points (found {}) to compute patterns.",
          ticker.getTickerSymbol(),
          bars.size());
      return;
    }

    Optional<DailyCandlestickPattern> lastPattern =
        dailyCandlestickPatternRepository.findFirstByTickerOrderByPriceDateDesc(ticker);
    LocalDate lastComputedDate =
        lastPattern.map(DailyCandlestickPattern::getPriceDate).orElse(null);

    List<PatternMatch> matches = findMatches(bars, lastComputedDate);

    if (!matches.isEmpty()) {
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
      log.info(
          "Ticker {}: Saved {} new daily candlestick patterns.",
          ticker.getTickerSymbol(),
          newPatterns.size());
    }
  }

  private void computeWeeklyPatternsForTicker(Ticker ticker) {
    List<PriceBar> bars = loadWeeklyBars(ticker);
    if (bars.size() < 3) {
      log.debug(
          "Ticker {}: Insufficient weekly data points (found {}) to compute patterns.",
          ticker.getTickerSymbol(),
          bars.size());
      return;
    }

    Optional<WeeklyCandlestickPattern> lastPattern =
        weeklyCandlestickPatternRepository.findFirstByTickerOrderByPriceDateDesc(ticker);
    LocalDate lastComputedDate =
        lastPattern.map(WeeklyCandlestickPattern::getPriceDate).orElse(null);

    List<PatternMatch> matches = findMatches(bars, lastComputedDate);

    if (!matches.isEmpty()) {
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
      log.info(
          "Ticker {}: Saved {} new weekly candlestick patterns.",
          ticker.getTickerSymbol(),
          newPatterns.size());
    }
  }

  private List<PatternMatch> findMatches(List<PriceBar> bars, LocalDate lastComputedDate) {
    List<PatternMatch> matches = new ArrayList<>();
    List<BigDecimal> bodies = computeAbsoluteBodies(bars);
    List<BigDecimal> avgBodies = computeMovingAverages(bodies, 14);
    List<BigDecimal> sma20List =
        computeMovingAverages(bars.stream().map(PriceBar::close).toList(), 20);

    for (int i = 2; i < bars.size(); i++) {
      PriceBar bar = bars.get(i);
      LocalDate date = bar.date();

      if (lastComputedDate != null && !date.isAfter(lastComputedDate)) {
        continue;
      }

      BigDecimal avgBody = avgBodies.get(i);
      BigDecimal sma20 = sma20List.get(i);

      for (CandlestickPattern pattern : CandlestickPattern.values()) {
        if (matchesPattern(bars, i, pattern, avgBody, sma20)) {
          matches.add(new PatternMatch(date, pattern));
        }
      }
    }
    return matches;
  }

  /**
   * Matches a candlestick pattern at a specific bar index.
   *
   * @param bars the list of price bars
   * @param i the current bar index
   * @param pattern the pattern to match
   * @param avgBody the moving average of body size
   * @param sma20 the 20-period moving average of close prices
   * @return true if the pattern matches at the index
   */
  private boolean matchesPattern(
      List<PriceBar> bars,
      int i,
      CandlestickPattern pattern,
      BigDecimal avgBody,
      BigDecimal sma20) {
    PriceBar cur = bars.get(i);
    BigDecimal curBody = body(cur);
    boolean curGreen = isGreen(cur);
    boolean curRed = isRed(cur);

    switch (pattern) {
      case BULLISH_MARUBOZU:
        return curGreen
            && curBody.compareTo(avgBody.multiply(THRESHOLD_LARGE, MC)) >= 0
            && lowerShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0
            && upperShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0;

      case BEARISH_MARUBOZU:
        return curRed
            && curBody.compareTo(avgBody.multiply(THRESHOLD_LARGE, MC)) >= 0
            && lowerShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0
            && upperShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0;

      case BULLISH_ENGULFING:
        {
          PriceBar prev = bars.get(i - 1);
          return isRed(prev)
              && curGreen
              && cur.open().compareTo(prev.close()) <= 0
              && cur.close().compareTo(prev.open()) >= 0
              && (cur.open().compareTo(prev.close()) < 0 || cur.close().compareTo(prev.open()) > 0);
        }

      case BEARISH_ENGULFING:
        {
          PriceBar prev = bars.get(i - 1);
          return isGreen(prev)
              && curRed
              && cur.open().compareTo(prev.close()) >= 0
              && cur.close().compareTo(prev.open()) <= 0
              && (cur.open().compareTo(prev.close()) > 0 || cur.close().compareTo(prev.open()) < 0);
        }

      case BULLISH_PIERCING:
        {
          PriceBar prev = bars.get(i - 1);
          if (!isRed(prev) || !curGreen || body(prev).compareTo(avgBody) < 0) {
            return false;
          }
          BigDecimal prevMidpoint = prev.close().add(prev.open()).divide(new BigDecimal("2"), MC);
          return cur.open().compareTo(prev.close()) < 0
              && cur.close().compareTo(prevMidpoint) > 0
              && cur.close().compareTo(prev.open()) <= 0;
        }

      case BEARISH_PIERCING:
        {
          PriceBar prev = bars.get(i - 1);
          if (!isGreen(prev) || !curRed || body(prev).compareTo(avgBody) < 0) {
            return false;
          }
          BigDecimal prevMidpoint = prev.close().add(prev.open()).divide(new BigDecimal("2"), MC);
          return cur.open().compareTo(prev.close()) > 0
              && cur.close().compareTo(prevMidpoint) < 0
              && cur.close().compareTo(prev.open()) >= 0;
        }

      case HAMMER:
        return curBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) <= 0
            && lowerShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_SHADOW, MC)) >= 0
            && upperShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0
            && cur.close().compareTo(sma20) < 0;

      case INVERTED_HAMMER:
        return curBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) <= 0
            && upperShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_SHADOW, MC)) >= 0
            && lowerShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0
            && cur.close().compareTo(sma20) < 0;

      case HANGING_MAN:
        {
          PriceBar prev = bars.get(i - 1);
          boolean prevHangingMan =
              body(prev).compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) <= 0
                  && lowerShadow(prev).compareTo(body(prev).multiply(RATIO_HAMMER_SHADOW, MC)) >= 0
                  && upperShadow(prev).compareTo(body(prev).multiply(RATIO_HAMMER_UPPER, MC)) <= 0
                  && prev.close().compareTo(sma20) > 0;
          return prevHangingMan && curRed;
        }

      case MORNING_STAR:
        {
          PriceBar first = bars.get(i - 2);
          PriceBar star = bars.get(i - 1);
          if (!isRed(first)
              || body(first).compareTo(avgBody.multiply(new BigDecimal("0.7"), MC)) < 0) {
            return false;
          }
          if (body(star).compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) > 0) {
            return false;
          }
          BigDecimal starMax = star.open().max(star.close());
          boolean gapDown = starMax.compareTo(first.close()) < 0;

          BigDecimal firstMidpoint =
              first.close().add(first.open()).divide(new BigDecimal("2"), MC);
          return gapDown
              && curGreen
              && body(cur).compareTo(avgBody.multiply(new BigDecimal("0.7"), MC)) >= 0
              && cur.close().compareTo(firstMidpoint) > 0
              && first.close().compareTo(sma20) < 0;
        }

      case EVENING_STAR:
        {
          PriceBar first = bars.get(i - 2);
          PriceBar star = bars.get(i - 1);
          if (!isGreen(first)
              || body(first).compareTo(avgBody.multiply(new BigDecimal("0.7"), MC)) < 0) {
            return false;
          }
          if (body(star).compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) > 0) {
            return false;
          }
          BigDecimal starMin = star.open().min(star.close());
          boolean gapUp = starMin.compareTo(first.close()) > 0;

          BigDecimal firstMidpoint =
              first.close().add(first.open()).divide(new BigDecimal("2"), MC);
          return gapUp
              && curRed
              && body(cur).compareTo(avgBody.multiply(new BigDecimal("0.7"), MC)) >= 0
              && cur.close().compareTo(firstMidpoint) < 0
              && first.close().compareTo(sma20) > 0;
        }
    }
    return false;
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
      ma.set(i, sum.divide(BigDecimal.valueOf(count), MC));
    }
    return ma;
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
