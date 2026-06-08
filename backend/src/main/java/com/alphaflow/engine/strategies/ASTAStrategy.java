package com.alphaflow.engine.strategies;

import com.alphaflow.api.dtos.AnalysisResponseDTO;
import com.alphaflow.api.dtos.IndicatorPointDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.api.services.DailyPriceService;
import com.alphaflow.api.services.IndicatorService;
import com.alphaflow.persistence.entities.AnalysisResult;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Timeframe;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.AnalysisResultRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
public class ASTAStrategy {

  private final DailyPriceService dailyPriceService;
  private final IndicatorService indicatorService;
  private final TickerRepository tickerRepository;
  private final AnalysisResultRepository analysisResultRepository;

  @Autowired @Lazy private ASTAStrategy astaStrategy;

  public ASTAStrategy(
      DailyPriceService dailyPriceService,
      IndicatorService indicatorService,
      TickerRepository tickerRepository,
      AnalysisResultRepository analysisResultRepository) {
    this.dailyPriceService = dailyPriceService;
    this.indicatorService = indicatorService;
    this.tickerRepository = tickerRepository;
    this.analysisResultRepository = analysisResultRepository;
  }

  /** Scheduled update step. Run this after indicator calculation. */
  public void doTechnicalAnalysis() {
    log.info("Starting Technical Analysis computation for all active tickers...");
    List<Ticker> activeTickers =
        tickerRepository.findAll().stream().filter(Ticker::isActive).toList();

    ASTAStrategy proxy = (astaStrategy != null) ? astaStrategy : this;
    for (Ticker ticker : activeTickers) {
      try {
        proxy.computeAndPersist(ticker);
      } catch (Exception e) {
        log.error(
            "Failed to compute technical analysis for ticker: {}", ticker.getTickerSymbol(), e);
      }
    }
    log.info("Technical Analysis computation finished.");
  }

  @Transactional(readOnly = true)
  public AnalysisResponseDTO getAnalysis(String symbol) {
    Ticker ticker =
        tickerRepository
            .findByTickerSymbolIgnoreCase(symbol)
            .orElseThrow(() -> new ResourceNotFoundException("Ticker not found: " + symbol));

    List<OhlcvDTO> latestCandles = dailyPriceService.getDailyPriceByTickerName(symbol, 0, 1);
    if (latestCandles.isEmpty()) {
      throw new ResourceNotFoundException("No daily price data found for symbol: " + symbol);
    }
    LocalDate latestPriceDate = latestCandles.getFirst().priceDate();

    AnalysisResult res =
        analysisResultRepository
            .findByTicker(ticker)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Analysis result not found or stale for symbol: " + symbol));

    if (res.getPriceDate().isBefore(latestPriceDate)) {
      throw new ResourceNotFoundException("Analysis result is stale for symbol: " + symbol);
    }

    return toDTO(res, symbol);
  }

  private AnalysisResponseDTO toDTO(AnalysisResult res, String symbol) {
    return AnalysisResponseDTO.builder()
        .symbol(symbol)
        .priceDate(res.getPriceDate())
        .emaSignal(res.getEmaSignal())
        .emaValue(res.getEmaValue())
        .macdSignal(res.getMacdSignal())
        .macdValue(res.getMacdValue())
        .stochasticSignal(res.getStochasticSignal())
        .stochasticValue(res.getStochasticValue())
        .rsiSignal(res.getRsiSignal())
        .rsiValue(res.getRsiValue())
        .volumeSignal(res.getVolumeSignal())
        .volumeValue(res.getVolumeValue())
        .overallSignal(res.getOverallSignal())
        .build();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AnalysisResult computeAndPersist(Ticker ticker) {
    String symbol = ticker.getTickerSymbol();

    // Fetch Daily candles (OhlcvDTOs are ordered oldest to newest)
    List<OhlcvDTO> dailyCandles = dailyPriceService.getDailyPriceByTickerName(symbol, 0, 50);

    // Fetch Indicators
    List<IndicatorSeriesDTO> dailyIndicators =
        indicatorService.getIndicatorSeries(symbol, Timeframe.DAILY, 0, 50);
    List<IndicatorSeriesDTO> weeklyIndicators =
        indicatorService.getIndicatorSeries(symbol, Timeframe.WEEKLY, 0, 10);

    // Date reference for this analysis
    LocalDate priceDate =
        dailyCandles.isEmpty() ? LocalDate.now() : dailyCandles.getLast().priceDate();

    // 1. MACD (12, 26) @ TIDE (Weekly)
    CalculatedSignal macd = evaluateWeeklyMacd(weeklyIndicators);

    // 2. Stochastic (14, 3, 3) @ WAVE (Daily)
    CalculatedSignal stoch = evaluateDailyStochastic(dailyIndicators);

    // 3. RSI (14) @ WAVE (Daily)
    CalculatedSignal rsi = evaluateDailyRsi(dailyIndicators);

    // 4. Volume (Daily)
    CalculatedSignal volume = evaluateVolume(dailyCandles, dailyIndicators);

    // 5. Moving Average EMA (Daily)
    CalculatedSignal ema = evaluateEma(dailyIndicators);

    // Overall consensus
    boolean doubleScreenBuy =
        "BUY".equals(macd.signal) && "BUY".equals(stoch.signal) && "BUY".equals(rsi.signal);
    boolean doubleScreenSell =
        "SELL".equals(macd.signal) && "SELL".equals(stoch.signal) && "SELL".equals(rsi.signal);

    boolean checklistBuy = "BUY".equals(volume.signal) && "BUY".equals(ema.signal);
    boolean checklistSell = "SELL".equals(volume.signal) && "SELL".equals(ema.signal);

    String overallSignal = "HOLD";
    if (doubleScreenBuy && checklistBuy) {
      overallSignal = "BUY";
    } else if (doubleScreenSell && checklistSell) {
      overallSignal = "SELL";
    }

    AnalysisResult result =
        analysisResultRepository
            .findByTicker(ticker)
            .orElseGet(() -> AnalysisResult.builder().ticker(ticker).build());

    result.setPriceDate(priceDate);
    result.setEmaSignal(ema.signal);
    result.setEmaValue(ema.value);
    result.setMacdSignal(macd.signal);
    result.setMacdValue(macd.value);
    result.setStochasticSignal(stoch.signal);
    result.setStochasticValue(stoch.value);
    result.setRsiSignal(rsi.signal);
    result.setRsiValue(rsi.value);
    result.setVolumeSignal(volume.signal);
    result.setVolumeValue(volume.value);
    result.setOverallSignal(overallSignal);

    return analysisResultRepository.save(result);
  }

  private CalculatedSignal evaluateWeeklyMacd(List<IndicatorSeriesDTO> weeklyIndicators) {
    Optional<IndicatorSeriesDTO> macdSeriesOpt =
        weeklyIndicators.stream().filter(s -> "MACD".equalsIgnoreCase(s.type())).findFirst();

    if (macdSeriesOpt.isEmpty() || macdSeriesOpt.get().points().size() < 2) {
      return new CalculatedSignal("HOLD", "Insufficient weekly MACD data");
    }

    List<IndicatorPointDTO> points = macdSeriesOpt.get().points();
    IndicatorPointDTO latest = points.getLast();
    IndicatorPointDTO prev = points.get(points.size() - 2);

    BigDecimal macd0 = latest.values().get("macd");
    BigDecimal sig0 = latest.values().get("signal");
    BigDecimal macd1 = prev.values().get("macd");
    BigDecimal sig1 = prev.values().get("signal");

    if (macd0 == null || sig0 == null || macd1 == null || sig1 == null) {
      return new CalculatedSignal("HOLD", "Missing MACD/Signal values");
    }

    boolean crossoverBuy = macd0.compareTo(sig0) > 0 && macd1.compareTo(sig1) <= 0;
    boolean crossoverSell = macd0.compareTo(sig0) < 0 && macd1.compareTo(sig1) >= 0;

    if (crossoverBuy) {
      return new CalculatedSignal("BUY", "Positive Crossover");
    } else if (crossoverSell) {
      return new CalculatedSignal("SELL", "Negative Crossover");
    } else if (macd0.compareTo(sig0) > 0) {
      BigDecimal hist0 = macd0.subtract(sig0);
      BigDecimal hist1 = macd1.subtract(sig1);
      String value = hist0.compareTo(hist1) > 0 ? "Uptick" : "Flat after down (rare)";
      return new CalculatedSignal("BUY", value);
    } else if (macd0.compareTo(sig0) < 0) {
      BigDecimal hist0 = macd0.subtract(sig0);
      BigDecimal hist1 = macd1.subtract(sig1);
      String value = hist0.compareTo(hist1) < 0 ? "Downtick" : "Flat after up (rare)";
      return new CalculatedSignal("SELL", value);
    } else {
      return new CalculatedSignal("HOLD", "MACD = Signal");
    }
  }

  private CalculatedSignal evaluateDailyStochastic(List<IndicatorSeriesDTO> dailyIndicators) {
    Optional<IndicatorSeriesDTO> stochSeriesOpt =
        dailyIndicators.stream().filter(s -> "STOCHASTIC".equalsIgnoreCase(s.type())).findFirst();

    if (stochSeriesOpt.isEmpty() || stochSeriesOpt.get().points().size() < 2) {
      return new CalculatedSignal("HOLD", "Insufficient stochastic data");
    }

    List<IndicatorPointDTO> points = stochSeriesOpt.get().points();
    IndicatorPointDTO latest = points.getLast();
    IndicatorPointDTO prev = points.get(points.size() - 2);

    BigDecimal k0 = latest.values().get("k");
    BigDecimal d0 = latest.values().get("d");
    BigDecimal k1 = prev.values().get("k");
    BigDecimal d1 = prev.values().get("d");

    if (k0 == null || d0 == null || k1 == null || d1 == null) {
      return new CalculatedSignal("HOLD", "Missing Stoch K/D values");
    }

    boolean crossoverBuy = k0.compareTo(d0) > 0 && k1.compareTo(d1) <= 0;
    boolean crossoverSell = k0.compareTo(d0) < 0 && k1.compareTo(d1) >= 0;

    if (crossoverBuy) {
      return new CalculatedSignal("BUY", "Positive Crossover");
    } else if (crossoverSell) {
      return new CalculatedSignal("SELL", "Negative Crossover");
    } else if (k0.compareTo(d0) > 0) {
      return new CalculatedSignal("BUY", "K > D");
    } else if (k0.compareTo(d0) < 0) {
      return new CalculatedSignal("SELL", "K < D");
    } else {
      return new CalculatedSignal("HOLD", "K = D");
    }
  }

  private CalculatedSignal evaluateDailyRsi(List<IndicatorSeriesDTO> dailyIndicators) {
    Optional<IndicatorSeriesDTO> rsiSeriesOpt =
        dailyIndicators.stream().filter(s -> "RSI".equalsIgnoreCase(s.type())).findFirst();

    if (rsiSeriesOpt.isEmpty() || rsiSeriesOpt.get().points().size() < 2) {
      return new CalculatedSignal("HOLD", "Insufficient RSI data");
    }

    List<IndicatorPointDTO> points = rsiSeriesOpt.get().points();
    IndicatorPointDTO latest = points.getLast();
    IndicatorPointDTO prev = points.get(points.size() - 2);

    BigDecimal rsi0 = latest.values().get("value");
    BigDecimal rsi1 = prev.values().get("value");

    if (rsi0 == null || rsi1 == null) {
      return new CalculatedSignal("HOLD", "Missing RSI values");
    }

    if (rsi0.compareTo(rsi1) > 0) {
      return new CalculatedSignal(
          "BUY", "Uptick (RSI: " + rsi0.setScale(1, RoundingMode.HALF_UP) + ")");
    } else if (rsi0.compareTo(rsi1) < 0) {
      return new CalculatedSignal(
          "SELL", "Downtick (RSI: " + rsi0.setScale(1, RoundingMode.HALF_UP) + ")");
    } else {
      return new CalculatedSignal(
          "HOLD", "Flat (RSI: " + rsi0.setScale(1, RoundingMode.HALF_UP) + ")");
    }
  }

  private CalculatedSignal evaluateVolume(
      List<OhlcvDTO> dailyCandles, List<IndicatorSeriesDTO> dailyIndicators) {
    if (dailyCandles.size() < 2) {
      return new CalculatedSignal("HOLD", "Insufficient price data");
    }

    OhlcvDTO latestCandle = dailyCandles.getLast();

    Optional<IndicatorSeriesDTO> volSmaOpt =
        dailyIndicators.stream()
            .filter(s -> "SMA".equalsIgnoreCase(s.type()) && "VOLUME".equalsIgnoreCase(s.source()))
            .findFirst();

    if (volSmaOpt.isEmpty() || volSmaOpt.get().points().isEmpty()) {
      return new CalculatedSignal("HOLD", "Insufficient volume SMA data");
    }

    List<IndicatorPointDTO> smaPoints = volSmaOpt.get().points();
    IndicatorPointDTO latestSma = smaPoints.getLast();
    BigDecimal volSmaValue = latestSma.values().get("value");

    if (volSmaValue == null) {
      return new CalculatedSignal("HOLD", "Missing volume SMA value");
    }

    BigDecimal volume = latestCandle.volume();
    boolean isHeavyVolume = volume.compareTo(volSmaValue) > 0;
    boolean isGreen = latestCandle.priceClose().compareTo(latestCandle.priceOpen()) > 0;
    boolean isRed = latestCandle.priceClose().compareTo(latestCandle.priceOpen()) < 0;

    if (isHeavyVolume) {
      if (isGreen) {
        return new CalculatedSignal("BUY", "Green Candle with Heavy Volume");
      } else if (isRed) {
        return new CalculatedSignal("SELL", "Red Candle with Heavy Volume");
      } else {
        return new CalculatedSignal("HOLD", "Doji with Heavy Volume");
      }
    } else {
      String val =
          isGreen
              ? "Green Candle with Normal Volume"
              : (isRed ? "Red Candle with Normal Volume" : "Doji with Normal Volume");
      return new CalculatedSignal("HOLD", val);
    }
  }

  private CalculatedSignal evaluateEma(List<IndicatorSeriesDTO> dailyIndicators) {
    Optional<IndicatorSeriesDTO> ema5Opt =
        dailyIndicators.stream()
            .filter(s -> "EMA".equalsIgnoreCase(s.type()) && s.params().contains("period=5"))
            .findFirst();
    Optional<IndicatorSeriesDTO> ema13Opt =
        dailyIndicators.stream()
            .filter(s -> "EMA".equalsIgnoreCase(s.type()) && s.params().contains("period=13"))
            .findFirst();
    Optional<IndicatorSeriesDTO> ema26Opt =
        dailyIndicators.stream()
            .filter(s -> "EMA".equalsIgnoreCase(s.type()) && s.params().contains("period=26"))
            .findFirst();

    if (ema5Opt.isEmpty()
        || ema13Opt.isEmpty()
        || ema26Opt.isEmpty()
        || ema5Opt.get().points().size() < 2
        || ema13Opt.get().points().size() < 2
        || ema26Opt.get().points().size() < 2) {
      return new CalculatedSignal("HOLD", "Insufficient EMA data");
    }

    List<IndicatorPointDTO> points5 = ema5Opt.get().points();
    List<IndicatorPointDTO> points13 = ema13Opt.get().points();
    List<IndicatorPointDTO> points26 = ema26Opt.get().points();

    BigDecimal e5_0 = points5.getLast().values().get("value");
    BigDecimal e5_1 = points5.get(points5.size() - 2).values().get("value");

    BigDecimal e13_0 = points13.getLast().values().get("value");
    BigDecimal e13_1 = points13.get(points13.size() - 2).values().get("value");

    BigDecimal e26_0 = points26.getLast().values().get("value");
    BigDecimal e26_1 = points26.get(points26.size() - 2).values().get("value");

    if (e5_0 == null
        || e5_1 == null
        || e13_0 == null
        || e13_1 == null
        || e26_0 == null
        || e26_1 == null) {
      return new CalculatedSignal("HOLD", "Missing EMA values");
    }

    boolean pco13 = e5_0.compareTo(e13_0) > 0 && e5_1.compareTo(e13_1) <= 0;
    boolean pco26 = e5_0.compareTo(e26_0) > 0 && e5_1.compareTo(e26_1) <= 0;
    boolean nco13 = e5_0.compareTo(e13_0) < 0 && e5_1.compareTo(e13_1) >= 0;
    boolean nco26 = e5_0.compareTo(e26_0) < 0 && e5_1.compareTo(e26_1) >= 0;

    if (pco13 || pco26) {
      String value =
          pco13 && pco26
              ? "5 EMA Positive Crossover with 13 & 26 EMA"
              : (pco13
                  ? "5 EMA Positive Crossover with 13 EMA"
                  : "5 EMA Positive Crossover with 26 EMA");
      return new CalculatedSignal("BUY", value);
    } else if (nco13 || nco26) {
      String value =
          nco13 && nco26
              ? "5 EMA Negative Crossover with 13 & 26 EMA"
              : (nco13
                  ? "5 EMA Negative Crossover with 13 EMA"
                  : "5 EMA Negative Crossover with 26 EMA");
      return new CalculatedSignal("SELL", value);
    } else if (e5_0.compareTo(e13_0) > 0 && e5_0.compareTo(e26_0) > 0) {
      return new CalculatedSignal("BUY", "EMA 5 > 13 & 26 (Bullish Alignment)");
    } else if (e5_0.compareTo(e13_0) < 0 && e5_0.compareTo(e26_0) < 0) {
      return new CalculatedSignal("SELL", "EMA 5 < 13 & 26 (Bearish Alignment)");
    } else {
      return new CalculatedSignal("HOLD", "Mixed EMAs");
    }
  }

  private record CalculatedSignal(String signal, String value) {}
}
