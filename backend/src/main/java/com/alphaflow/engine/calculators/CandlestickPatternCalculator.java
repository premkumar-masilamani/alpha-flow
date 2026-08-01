package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.dtos.PatternMatch;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
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
  private static final int MIN_BARS = 3;
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
    log.info("Ticker {}: Timeframe {} - Calculating...", ticker.getTickerSymbol(), Timeframe.DAILY);
    computeDailyPatternsForTicker(ticker);
    log.info(
        "Ticker {}: Timeframe {} - Calculating...", ticker.getTickerSymbol(), Timeframe.WEEKLY);
    computeWeeklyPatternsForTicker(ticker);
  }

  private void computeDailyPatternsForTicker(Ticker ticker) {
    List<PriceBar> bars = loadDailyBars(ticker);
    if (bars.size() < MIN_BARS) {
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
    if (bars.size() < MIN_BARS) {
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
    List<BigDecimal> avgBodies = computeMovingAverages(bodies, PERIOD_BODY_MA);

    for (int i = 2; i < bars.size(); i++) {
      PriceBar bar = bars.get(i);
      LocalDate date = bar.date();

      if (lastComputedDate != null && !date.isAfter(lastComputedDate)) {
        continue;
      }

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

    switch (pattern) {
      // Bullish Marubozu: A long green body with little to no upper and lower shadows, showing
      // strong buying pressure.
      case BULLISH_MARUBOZU:
        return curGreen
            && curBody.compareTo(avgBody.multiply(THRESHOLD_LARGE, MC)) >= 0
            && lowerShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0
            && upperShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0;

      // Bearish Marubozu: A long red body with little to no upper and lower shadows, showing strong
      // selling pressure.
      case BEARISH_MARUBOZU:
        return curRed
            && curBody.compareTo(avgBody.multiply(THRESHOLD_LARGE, MC)) >= 0
            && lowerShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0
            && upperShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0;

      // Bullish Engulfing: A two-candle pattern where a small red candle is fully engulfed by a
      // subsequent larger green candle.
      case BULLISH_ENGULFING:
        PriceBar prevBullish = bars.get(i - 1);
        return isRed(prevBullish)
            && curGreen
            && cur.open().compareTo(prevBullish.close()) <= 0
            && cur.close().compareTo(prevBullish.open()) >= 0
            && (cur.open().compareTo(prevBullish.close()) < 0
                || cur.close().compareTo(prevBullish.open()) > 0);

      // Bearish Engulfing: A two-candle pattern where a small green candle is fully engulfed by a
      // subsequent larger red candle.
      case BEARISH_ENGULFING:
        PriceBar prevBearish = bars.get(i - 1);
        return isGreen(prevBearish)
            && curRed
            && cur.open().compareTo(prevBearish.close()) >= 0
            && cur.close().compareTo(prevBearish.open()) <= 0
            && (cur.open().compareTo(prevBearish.close()) > 0
                || cur.close().compareTo(prevBearish.open()) < 0);

      // Bullish Piercing (Piercing Line): A two-candle reversal pattern where a green candle opens
      // below the previous red candle's close and closes more than halfway up its body.
      case BULLISH_PIERCING:
        PriceBar prevPiercingBullish = bars.get(i - 1);
        if (!isRed(prevPiercingBullish)
            || !curGreen
            || body(prevPiercingBullish).compareTo(avgBody) < 0) {
          return false;
        }
        BigDecimal midpointBullish =
            prevPiercingBullish
                .close()
                .add(prevPiercingBullish.open())
                .divide(DIVISOR_MIDPOINT, MC);
        return cur.open().compareTo(prevPiercingBullish.close()) < 0
            && cur.close().compareTo(midpointBullish) > 0
            && cur.close().compareTo(prevPiercingBullish.open()) <= 0;

      // Bearish Piercing (Dark Cloud Cover): A two-candle reversal pattern where a red candle opens
      // above the previous green candle's close and closes more than halfway down its body.
      case BEARISH_PIERCING:
        PriceBar prevPiercingBearish = bars.get(i - 1);
        if (!isGreen(prevPiercingBearish)
            || !curRed
            || body(prevPiercingBearish).compareTo(avgBody) < 0) {
          return false;
        }
        BigDecimal midpointBearish =
            prevPiercingBearish
                .close()
                .add(prevPiercingBearish.open())
                .divide(DIVISOR_MIDPOINT, MC);
        return cur.open().compareTo(prevPiercingBearish.close()) > 0
            && cur.close().compareTo(midpointBearish) < 0
            && cur.close().compareTo(prevPiercingBearish.open()) >= 0;

      // Hammer: A single-candle bullish reversal pattern with a small body and a long lower shadow
      // (>= 2x body).
      case HAMMER:
        return curBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) <= 0
            && lowerShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_SHADOW, MC)) >= 0
            && upperShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0;

      // Inverted Hammer: A single-candle bullish reversal pattern with a small body and a long
      // upper shadow (>= 2x body).
      case INVERTED_HAMMER:
        return curBody.compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) <= 0
            && upperShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_SHADOW, MC)) >= 0
            && lowerShadow(cur).compareTo(curBody.multiply(RATIO_HAMMER_UPPER, MC)) <= 0;

      // Hanging Man: A bearish reversal pattern featuring a hammer-like candle followed by a
      // confirmation red candle.
      case HANGING_MAN:
        PriceBar prevHanging = bars.get(i - 1);
        boolean prevHangingMan =
            body(prevHanging).compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) <= 0
                && lowerShadow(prevHanging)
                        .compareTo(body(prevHanging).multiply(RATIO_HAMMER_SHADOW, MC))
                    >= 0
                && upperShadow(prevHanging)
                        .compareTo(body(prevHanging).multiply(RATIO_HAMMER_UPPER, MC))
                    <= 0;
        return prevHangingMan && curRed;

      // Morning Star: A three-candle bullish reversal pattern consisting of a long red candle, a
      // gapping down star (small body), and a green candle closing more than halfway up the first
      // candle's body.
      case MORNING_STAR:
        PriceBar firstMorning = bars.get(i - 2);
        PriceBar starMorning = bars.get(i - 1);
        if (!isRed(firstMorning)
            || body(firstMorning).compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) < 0) {
          return false;
        }
        if (body(starMorning).compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) > 0) {
          return false;
        }
        BigDecimal starMax = starMorning.open().max(starMorning.close());
        boolean gapDown = starMax.compareTo(firstMorning.close()) < 0;

        BigDecimal firstMidpoint =
            firstMorning.close().add(firstMorning.open()).divide(DIVISOR_MIDPOINT, MC);
        return gapDown
            && curGreen
            && body(cur).compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) >= 0
            && cur.close().compareTo(firstMidpoint) > 0;

      // Evening Star: A three-candle bearish reversal pattern consisting of a long green candle, a
      // gapping up star (small body), and a red candle closing more than halfway down the first
      // candle's body.
      case EVENING_STAR:
        PriceBar firstEvening = bars.get(i - 2);
        PriceBar starEvening = bars.get(i - 1);
        if (!isGreen(firstEvening)
            || body(firstEvening).compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) < 0) {
          return false;
        }
        if (body(starEvening).compareTo(avgBody.multiply(THRESHOLD_SMALL, MC)) > 0) {
          return false;
        }
        BigDecimal starMin = starEvening.open().min(starEvening.close());
        boolean gapUp = starMin.compareTo(firstEvening.close()) > 0;

        BigDecimal firstMidpointEvening =
            firstEvening.close().add(firstEvening.open()).divide(DIVISOR_MIDPOINT, MC);
        return gapUp
            && curRed
            && body(cur).compareTo(avgBody.multiply(RATIO_STAR_OUTER, MC)) >= 0
            && cur.close().compareTo(firstMidpointEvening) < 0;
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
