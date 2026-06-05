package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.alphaflow.api.dtos.AnalysisResponseDTO;
import com.alphaflow.api.dtos.IndicatorPointDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.persistence.entities.AnalysisResult;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Timeframe;
import com.alphaflow.persistence.repositories.AnalysisResultRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnalysisServiceTest {

  private static final String SYMBOL = "TEST";
  private static final LocalDate TODAY = LocalDate.of(2026, 5, 30);
  private static final LocalDate YESTERDAY = LocalDate.of(2026, 5, 29);
  private static final LocalDate TWO_DAYS_AGO = LocalDate.of(2026, 5, 28);
  private DailyPriceService dailyPriceService;
  private IndicatorService indicatorService;
  private TickerRepository tickerRepository;
  private AnalysisResultRepository analysisResultRepository;
  private AnalysisService analysisService;

  @BeforeEach
  void setUp() {
    dailyPriceService = mock(DailyPriceService.class);
    indicatorService = mock(IndicatorService.class);
    tickerRepository = mock(TickerRepository.class);
    analysisResultRepository = mock(AnalysisResultRepository.class);
    analysisService =
        new AnalysisService(
            dailyPriceService, indicatorService, tickerRepository, analysisResultRepository);
  }

  private OhlcvDTO candle(
      LocalDate date, double open, double high, double low, double close, double volume) {
    return OhlcvDTO.builder()
        .priceDate(date)
        .priceOpen(BigDecimal.valueOf(open))
        .priceHigh(BigDecimal.valueOf(high))
        .priceLow(BigDecimal.valueOf(low))
        .priceClose(BigDecimal.valueOf(close))
        .volume(BigDecimal.valueOf(volume))
        .build();
  }

  private IndicatorSeriesDTO series(
      String type, String source, String params, List<IndicatorPointDTO> points) {
    return IndicatorSeriesDTO.builder()
        .type(type)
        .source(source)
        .params(params)
        .label(type)
        .points(points)
        .build();
  }

  private IndicatorPointDTO point(LocalDate date, Map<String, BigDecimal> values) {
    return IndicatorPointDTO.builder().date(date).values(values).build();
  }

  @Test
  void testGetAnalysisPullsPersistedResult() {
    Ticker ticker = Ticker.builder().tickerSymbol(SYMBOL).build();
    when(tickerRepository.findByTickerSymbolIgnoreCase(SYMBOL)).thenReturn(Optional.of(ticker));

    AnalysisResult result =
        AnalysisResult.builder()
            .ticker(ticker)
            .priceDate(TODAY)
            .emaSignal("BUY")
            .emaValue("EMA 5 > 13 & 26 (Bullish Alignment)")
            .macdSignal("BUY")
            .macdValue("Positive Crossover")
            .stochasticSignal("BUY")
            .stochasticValue("Positive Crossover")
            .rsiSignal("BUY")
            .rsiValue("Uptick (RSI: 55.0)")
            .volumeSignal("BUY")
            .volumeValue("Green Candle with Heavy Volume")
            .overallSignal("BUY")
            .build();
    when(analysisResultRepository.findByTicker(ticker)).thenReturn(Optional.of(result));

    AnalysisResponseDTO response = analysisService.getAnalysis(SYMBOL);

    assertNotNull(response);
    assertEquals("BUY", response.overallSignal());
    assertEquals("Positive Crossover", response.macdValue());
    verify(analysisResultRepository, never()).save(any());
  }

  @Test
  void testComputeAndPersistCalculatesSignalsCorrectly() {
    Ticker ticker = Ticker.builder().tickerSymbol(SYMBOL).build();
    when(tickerRepository.findByTickerSymbolIgnoreCase(SYMBOL)).thenReturn(Optional.of(ticker));
    when(analysisResultRepository.findByTicker(ticker)).thenReturn(Optional.empty());

    // Setup daily candles
    List<OhlcvDTO> candles =
        List.of(
            candle(TWO_DAYS_AGO, 100, 105, 95, 102, 1000),
            candle(YESTERDAY, 102, 106, 101, 105, 1200),
            candle(TODAY, 105, 110, 104, 109, 1500));
    when(dailyPriceService.getDailyPriceByTickerName(SYMBOL, 0, 50)).thenReturn(candles);

    // Setup weekly MACD (Positive Crossover)
    List<IndicatorPointDTO> macdPoints =
        List.of(
            point(
                YESTERDAY,
                Map.of("macd", BigDecimal.valueOf(1.0), "signal", BigDecimal.valueOf(1.2))),
            point(
                TODAY, Map.of("macd", BigDecimal.valueOf(1.5), "signal", BigDecimal.valueOf(1.3))));
    IndicatorSeriesDTO macdSeries = series("MACD", "CLOSE", "fast=12,slow=26,signal=9", macdPoints);
    when(indicatorService.getIndicatorSeries(SYMBOL, Timeframe.WEEKLY, 0, 10))
        .thenReturn(List.of(macdSeries));

    // Setup daily Stochastic (K > D)
    List<IndicatorPointDTO> stochPoints =
        List.of(
            point(YESTERDAY, Map.of("k", BigDecimal.valueOf(70), "d", BigDecimal.valueOf(75))),
            point(TODAY, Map.of("k", BigDecimal.valueOf(82), "d", BigDecimal.valueOf(80))));
    IndicatorSeriesDTO stochSeries =
        series("STOCHASTIC", "CLOSE", "k=14,kSmooth=3,dSmooth=3", stochPoints);

    // Setup daily RSI (Uptick)
    List<IndicatorPointDTO> rsiPoints =
        List.of(
            point(YESTERDAY, Map.of("value", BigDecimal.valueOf(45.0))),
            point(TODAY, Map.of("value", BigDecimal.valueOf(52.0))));
    IndicatorSeriesDTO rsiSeries = series("RSI", "CLOSE", "period=14", rsiPoints);

    // Setup Volume SMA(20)
    List<IndicatorPointDTO> volSmaPoints =
        List.of(
            point(YESTERDAY, Map.of("value", BigDecimal.valueOf(1100.0))),
            point(TODAY, Map.of("value", BigDecimal.valueOf(1100.0))));
    IndicatorSeriesDTO volSmaSeries = series("SMA", "VOLUME", "period=20", volSmaPoints);

    // Setup EMAs (Bullish Alignment)
    IndicatorSeriesDTO ema5 =
        series(
            "EMA",
            "CLOSE",
            "period=5",
            List.of(
                point(YESTERDAY, Map.of("value", BigDecimal.valueOf(101.0))),
                point(TODAY, Map.of("value", BigDecimal.valueOf(104.0)))));
    IndicatorSeriesDTO ema13 =
        series(
            "EMA",
            "CLOSE",
            "period=13",
            List.of(
                point(YESTERDAY, Map.of("value", BigDecimal.valueOf(100.0))),
                point(TODAY, Map.of("value", BigDecimal.valueOf(102.0)))));
    IndicatorSeriesDTO ema26 =
        series(
            "EMA",
            "CLOSE",
            "period=26",
            List.of(
                point(YESTERDAY, Map.of("value", BigDecimal.valueOf(99.0))),
                point(TODAY, Map.of("value", BigDecimal.valueOf(101.0)))));

    when(indicatorService.getIndicatorSeries(SYMBOL, Timeframe.DAILY, 0, 50))
        .thenReturn(List.of(stochSeries, rsiSeries, volSmaSeries, ema5, ema13, ema26));

    when(analysisResultRepository.save(any(AnalysisResult.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Run
    AnalysisResponseDTO response = analysisService.getAnalysis(SYMBOL);

    assertNotNull(response);
    assertEquals("BUY", response.overallSignal());
    assertEquals("Positive Crossover", response.macdValue());
    assertEquals("BUY", response.macdSignal());
    assertEquals("Positive Crossover", response.stochasticValue());
    assertEquals("BUY", response.stochasticSignal());
    assertEquals("BUY", response.rsiSignal());
    assertEquals("BUY", response.volumeSignal());
    assertEquals("BUY", response.emaSignal());

    verify(analysisResultRepository, times(1)).save(any(AnalysisResult.class));
  }

  @Test
  void testComputeAnalysisContinuesOnTickerFailure() {
    Ticker ticker1 = Ticker.builder().tickerSymbol("T1").isActive(true).build();
    Ticker ticker2 = Ticker.builder().tickerSymbol("T2").isActive(true).build();
    when(tickerRepository.findAll()).thenReturn(List.of(ticker1, ticker2));

    // Let T1 throw an exception during daily price fetch
    when(dailyPriceService.getDailyPriceByTickerName("T1", 0, 50))
        .thenThrow(new RuntimeException("Injected data fetch error"));

    // Setup successful mocks for T2
    List<OhlcvDTO> candles2 = List.of(candle(TODAY, 100, 105, 95, 100, 1000));
    when(dailyPriceService.getDailyPriceByTickerName("T2", 0, 50)).thenReturn(candles2);
    when(indicatorService.getIndicatorSeries("T2", Timeframe.WEEKLY, 0, 10)).thenReturn(List.of());
    when(indicatorService.getIndicatorSeries("T2", Timeframe.DAILY, 0, 50)).thenReturn(List.of());
    when(analysisResultRepository.findByTicker(ticker2)).thenReturn(Optional.empty());
    when(analysisResultRepository.save(any(AnalysisResult.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Run
    analysisService.computeAnalysis();

    // Verify that only T2 was successfully saved
    verify(analysisResultRepository, times(1)).save(any(AnalysisResult.class));
    verify(analysisResultRepository)
        .save(argThat(res -> "T2".equals(res.getTicker().getTickerSymbol())));
  }

  @Test
  void testEvaluateDailyStochasticFlatReturnsHold() {
    Ticker ticker = Ticker.builder().tickerSymbol(SYMBOL).build();
    when(tickerRepository.findByTickerSymbolIgnoreCase(SYMBOL)).thenReturn(Optional.of(ticker));
    when(analysisResultRepository.findByTicker(ticker)).thenReturn(Optional.empty());

    // Setup daily candles
    List<OhlcvDTO> candles =
        List.of(
            candle(YESTERDAY, 102, 106, 101, 105, 1200), candle(TODAY, 105, 110, 104, 109, 1500));
    when(dailyPriceService.getDailyPriceByTickerName(SYMBOL, 0, 50)).thenReturn(candles);

    // Setup weekly indicators empty (defaults MACD to HOLD)
    when(indicatorService.getIndicatorSeries(SYMBOL, Timeframe.WEEKLY, 0, 10))
        .thenReturn(List.of());

    // Setup daily Stochastic where latest %K == %D (both 80)
    List<IndicatorPointDTO> stochPoints =
        List.of(
            point(YESTERDAY, Map.of("k", BigDecimal.valueOf(70), "d", BigDecimal.valueOf(75))),
            point(TODAY, Map.of("k", BigDecimal.valueOf(80), "d", BigDecimal.valueOf(80))));
    IndicatorSeriesDTO stochSeries =
        series("STOCHASTIC", "CLOSE", "k=14,kSmooth=3,dSmooth=3", stochPoints);

    when(indicatorService.getIndicatorSeries(SYMBOL, Timeframe.DAILY, 0, 50))
        .thenReturn(List.of(stochSeries));
    when(analysisResultRepository.save(any(AnalysisResult.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Run
    AnalysisResponseDTO response = analysisService.getAnalysis(SYMBOL);

    // Verify stochastic signal is HOLD and value is "K = D"
    assertNotNull(response);
    assertEquals("HOLD", response.stochasticSignal());
    assertEquals("K = D", response.stochasticValue());
  }

  @Test
  void testEvaluateWeeklyMacdFlatReturnsHold() {
    Ticker ticker = Ticker.builder().tickerSymbol(SYMBOL).build();
    when(tickerRepository.findByTickerSymbolIgnoreCase(SYMBOL)).thenReturn(Optional.of(ticker));
    when(analysisResultRepository.findByTicker(ticker)).thenReturn(Optional.empty());

    // Setup daily candles
    List<OhlcvDTO> candles =
        List.of(
            candle(YESTERDAY, 102, 106, 101, 105, 1200), candle(TODAY, 105, 110, 104, 109, 1500));
    when(dailyPriceService.getDailyPriceByTickerName(SYMBOL, 0, 50)).thenReturn(candles);

    // Setup weekly MACD where latest macd == signal (both 1.5)
    List<IndicatorPointDTO> macdPoints =
        List.of(
            point(
                YESTERDAY,
                Map.of("macd", BigDecimal.valueOf(1.0), "signal", BigDecimal.valueOf(1.2))),
            point(
                TODAY, Map.of("macd", BigDecimal.valueOf(1.5), "signal", BigDecimal.valueOf(1.5))));
    IndicatorSeriesDTO macdSeries = series("MACD", "CLOSE", "fast=12,slow=26,signal=9", macdPoints);
    when(indicatorService.getIndicatorSeries(SYMBOL, Timeframe.WEEKLY, 0, 10))
        .thenReturn(List.of(macdSeries));

    // Setup empty daily indicators
    when(indicatorService.getIndicatorSeries(SYMBOL, Timeframe.DAILY, 0, 50)).thenReturn(List.of());
    when(analysisResultRepository.save(any(AnalysisResult.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Run
    AnalysisResponseDTO response = analysisService.getAnalysis(SYMBOL);

    // Verify MACD signal is HOLD and value is "MACD = Signal"
    assertNotNull(response);
    assertEquals("HOLD", response.macdSignal());
    assertEquals("MACD = Signal", response.macdValue());
  }

  @Test
  void testGetAnalysisHandlesConcurrentInsertRaceCondition() {
    Ticker ticker = Ticker.builder().tickerSymbol(SYMBOL).build();
    when(tickerRepository.findByTickerSymbolIgnoreCase(SYMBOL)).thenReturn(Optional.of(ticker));

    // First call returns empty, second call returns the concurrently saved result
    when(analysisResultRepository.findByTicker(ticker))
        .thenReturn(Optional.empty())
        .thenReturn(
            Optional.of(AnalysisResult.builder().ticker(ticker).overallSignal("BUY").build()));

    // Simulate database integrity error (unique key constraint violation) during computeAndPersist
    when(dailyPriceService.getDailyPriceByTickerName(SYMBOL, 0, 50))
        .thenThrow(
            new org.springframework.dao.DataIntegrityViolationException("Duplicate key violation"));

    // Run
    AnalysisResponseDTO response = analysisService.getAnalysis(SYMBOL);

    // Verify that it successfully recovered by fetching the concurrently saved row
    assertNotNull(response);
    assertEquals("BUY", response.overallSignal());

    // Verify findByTicker was called twice
    verify(analysisResultRepository, times(2)).findByTicker(ticker);
  }
}
