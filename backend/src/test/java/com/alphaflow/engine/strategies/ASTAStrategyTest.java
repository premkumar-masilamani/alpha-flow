package com.alphaflow.engine.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.alphaflow.engine.configs.IndicatorConfig;
import com.alphaflow.engine.strategies.evaluators.DailyEmaEvaluator;
import com.alphaflow.engine.strategies.evaluators.DailyRsiEvaluator;
import com.alphaflow.engine.strategies.evaluators.DailyStochasticEvaluator;
import com.alphaflow.engine.strategies.evaluators.DailyVolumeEvaluator;
import com.alphaflow.engine.strategies.evaluators.WeeklyMacdEvaluator;
import com.alphaflow.persistence.entities.*;
import com.alphaflow.persistence.enums.*;
import com.alphaflow.persistence.repositories.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ASTAStrategyTest {

  private static final String SYMBOL = "TEST";
  private static final int HISTORY_WINDOW = 5;
  private static final LocalDate TODAY = LocalDate.of(2026, 5, 30);
  private static final LocalDate YESTERDAY = LocalDate.of(2026, 5, 29);
  private static final LocalDate TWO_DAYS_AGO = LocalDate.of(2026, 5, 28);
  private TickerRepository tickerRepository;
  private ASTAResultsRepository astaResultsRepository;
  private DailyPriceRepository dailyPriceRepository;
  private DailyIndicatorRepository dailyIndicatorRepository;
  private WeeklyIndicatorRepository weeklyIndicatorRepository;
  private IndicatorConfig indicatorConfig;
  private ASTAStrategy astaStrategy;

  @BeforeEach
  void setUp() {
    tickerRepository = mock(TickerRepository.class);
    astaResultsRepository = mock(ASTAResultsRepository.class);
    dailyPriceRepository = mock(DailyPriceRepository.class);
    dailyIndicatorRepository = mock(DailyIndicatorRepository.class);
    weeklyIndicatorRepository = mock(WeeklyIndicatorRepository.class);
    indicatorConfig = mock(IndicatorConfig.class);

    // Setup active indicators
    when(indicatorConfig.forTimeframe(Timeframe.DAILY))
        .thenReturn(
            List.of(
                IndicatorDefinition.builder()
                    .indicatorId(1L)
                    .indicatorType(IndicatorType.EMA)
                    .params(Map.of("period", 5))
                    .source(PriceSource.CLOSE)
                    .build(),
                IndicatorDefinition.builder()
                    .indicatorId(2L)
                    .indicatorType(IndicatorType.EMA)
                    .params(Map.of("period", 13))
                    .source(PriceSource.CLOSE)
                    .build(),
                IndicatorDefinition.builder()
                    .indicatorId(3L)
                    .indicatorType(IndicatorType.EMA)
                    .params(Map.of("period", 26))
                    .source(PriceSource.CLOSE)
                    .build(),
                IndicatorDefinition.builder()
                    .indicatorId(4L)
                    .indicatorType(IndicatorType.RSI)
                    .params(Map.of("period", 14))
                    .source(PriceSource.CLOSE)
                    .build(),
                IndicatorDefinition.builder()
                    .indicatorId(5L)
                    .indicatorType(IndicatorType.SMA)
                    .params(Map.of("period", 20))
                    .source(PriceSource.VOLUME)
                    .build(),
                IndicatorDefinition.builder()
                    .indicatorId(6L)
                    .indicatorType(IndicatorType.STOCHASTIC)
                    .params(Map.of("k", 14, "kSmooth", 3, "dSmooth", 3))
                    .source(PriceSource.CLOSE)
                    .build()));
    when(indicatorConfig.forTimeframe(Timeframe.WEEKLY))
        .thenReturn(
            List.of(
                IndicatorDefinition.builder()
                    .indicatorId(7L)
                    .indicatorType(IndicatorType.MACD)
                    .params(Map.of("fast", 12, "slow", 26, "signal", 9))
                    .source(PriceSource.CLOSE)
                    .build()));

    astaStrategy =
        new ASTAStrategy(
            tickerRepository,
            astaResultsRepository,
            dailyPriceRepository,
            dailyIndicatorRepository,
            weeklyIndicatorRepository,
            indicatorConfig,
            new WeeklyMacdEvaluator(),
            new DailyStochasticEvaluator(),
            new DailyRsiEvaluator(),
            new DailyVolumeEvaluator(),
            new DailyEmaEvaluator());
  }

  private DailyPrice dPrice(
      LocalDate date, double open, double high, double low, double close, double volume) {
    return DailyPrice.builder()
        .priceDate(date)
        .priceOpen(BigDecimal.valueOf(open))
        .priceHigh(BigDecimal.valueOf(high))
        .priceLow(BigDecimal.valueOf(low))
        .priceClose(BigDecimal.valueOf(close))
        .volume(BigDecimal.valueOf(volume))
        .build();
  }

  private void mockRangeQueries(
      Ticker ticker,
      List<DailyPrice> dailyPrices,
      List<DailyIndicator> dailyIndicators,
      List<WeeklyIndicator> weeklyIndicators) {
    LocalDate start = TODAY.minusDays(HISTORY_WINDOW);
    when(dailyPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            ticker, start))
        .thenReturn(dailyPrices);
    when(dailyIndicatorRepository.findSeriesFrom(eq(ticker), any(), eq(start)))
        .thenReturn(dailyIndicators);
    when(weeklyIndicatorRepository.findSeriesFrom(eq(ticker), any(), eq(start)))
        .thenReturn(weeklyIndicators);
    when(astaResultsRepository.findByTickerAndPriceDateGreaterThanEqual(ticker, TODAY))
        .thenReturn(List.of());
  }

  @Test
  void testComputeAndPersistCalculatesSignalsCorrectly() {
    Ticker ticker = Ticker.builder().tickerSymbol(SYMBOL).build();
    when(tickerRepository.findByTickerSymbolIgnoreCase(SYMBOL)).thenReturn(Optional.of(ticker));

    // Setup range query data
    LocalDate THREE_DAYS_AGO = LocalDate.of(2026, 5, 27);
    List<DailyPrice> dailyPrices =
        List.of(
            dPrice(THREE_DAYS_AGO, 98, 102, 97, 100, 800),
            dPrice(TWO_DAYS_AGO, 100, 105, 95, 102, 1000),
            dPrice(YESTERDAY, 102, 106, 101, 105, 1200),
            dPrice(TODAY, 105, 110, 104, 109, 1500));

    IndicatorDefinition stochDef =
        IndicatorDefinition.builder()
            .indicatorId(6L)
            .indicatorType(IndicatorType.STOCHASTIC)
            .source(PriceSource.CLOSE)
            .params(Map.of("k", 14, "kSmooth", 3, "dSmooth", 3))
            .build();
    IndicatorDefinition rsiDef =
        IndicatorDefinition.builder()
            .indicatorId(4L)
            .indicatorType(IndicatorType.RSI)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 14))
            .build();
    IndicatorDefinition volSmaDef =
        IndicatorDefinition.builder()
            .indicatorId(5L)
            .indicatorType(IndicatorType.SMA)
            .source(PriceSource.VOLUME)
            .params(Map.of("period", 20))
            .build();
    IndicatorDefinition ema5Def =
        IndicatorDefinition.builder()
            .indicatorId(1L)
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 5))
            .build();
    IndicatorDefinition ema13Def =
        IndicatorDefinition.builder()
            .indicatorId(2L)
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 13))
            .build();
    IndicatorDefinition ema26Def =
        IndicatorDefinition.builder()
            .indicatorId(3L)
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 26))
            .build();
    IndicatorDefinition macdDef =
        IndicatorDefinition.builder()
            .indicatorId(7L)
            .indicatorType(IndicatorType.MACD)
            .source(PriceSource.CLOSE)
            .params(Map.of("fast", 12, "slow", 26, "signal", 9))
            .build();

    List<DailyIndicator> dailyIndicators =
        List.of(
            // Stochastic
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(stochDef)
                .priceDate(YESTERDAY)
                .values(Map.of("k", BigDecimal.valueOf(70), "d", BigDecimal.valueOf(75)))
                .build(),
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(stochDef)
                .priceDate(TODAY)
                .values(Map.of("k", BigDecimal.valueOf(82), "d", BigDecimal.valueOf(80)))
                .build(),
            // RSI
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(rsiDef)
                .priceDate(YESTERDAY)
                .values(Map.of("value", BigDecimal.valueOf(45.0)))
                .build(),
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(rsiDef)
                .priceDate(TODAY)
                .values(Map.of("value", BigDecimal.valueOf(52.0)))
                .build(),
            // Vol SMA
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(volSmaDef)
                .priceDate(YESTERDAY)
                .values(Map.of("value", BigDecimal.valueOf(1100.0)))
                .build(),
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(volSmaDef)
                .priceDate(TODAY)
                .values(Map.of("value", BigDecimal.valueOf(1100.0)))
                .build(),
            // EMA 5
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(ema5Def)
                .priceDate(THREE_DAYS_AGO)
                .values(Map.of("value", BigDecimal.valueOf(100.0)))
                .build(),
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(ema5Def)
                .priceDate(TWO_DAYS_AGO)
                .values(Map.of("value", BigDecimal.valueOf(101.0)))
                .build(),
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(ema5Def)
                .priceDate(YESTERDAY)
                .values(Map.of("value", BigDecimal.valueOf(102.0)))
                .build(),
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(ema5Def)
                .priceDate(TODAY)
                .values(Map.of("value", BigDecimal.valueOf(104.0)))
                .build(),
            // EMA 13
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(ema13Def)
                .priceDate(THREE_DAYS_AGO)
                .values(Map.of("value", BigDecimal.valueOf(98.0)))
                .build(),
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(ema13Def)
                .priceDate(TWO_DAYS_AGO)
                .values(Map.of("value", BigDecimal.valueOf(99.0)))
                .build(),
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(ema13Def)
                .priceDate(YESTERDAY)
                .values(Map.of("value", BigDecimal.valueOf(100.0)))
                .build(),
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(ema13Def)
                .priceDate(TODAY)
                .values(Map.of("value", BigDecimal.valueOf(102.0)))
                .build(),
            // EMA 26
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(ema26Def)
                .priceDate(THREE_DAYS_AGO)
                .values(Map.of("value", BigDecimal.valueOf(96.0)))
                .build(),
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(ema26Def)
                .priceDate(TWO_DAYS_AGO)
                .values(Map.of("value", BigDecimal.valueOf(97.0)))
                .build(),
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(ema26Def)
                .priceDate(YESTERDAY)
                .values(Map.of("value", BigDecimal.valueOf(99.0)))
                .build(),
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(ema26Def)
                .priceDate(TODAY)
                .values(Map.of("value", BigDecimal.valueOf(101.0)))
                .build());

    List<WeeklyIndicator> weeklyIndicators =
        List.of(
            WeeklyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(macdDef)
                .priceDate(YESTERDAY)
                .values(Map.of("macd", BigDecimal.valueOf(1.0), "signal", BigDecimal.valueOf(1.2)))
                .build(),
            WeeklyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(macdDef)
                .priceDate(TODAY)
                .values(Map.of("macd", BigDecimal.valueOf(1.5), "signal", BigDecimal.valueOf(1.3)))
                .build());

    mockRangeQueries(ticker, dailyPrices, dailyIndicators, weeklyIndicators);

    when(astaResultsRepository.saveAll(anyList()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Run
    ASTAResults response = astaStrategy.processTicker(ticker, TODAY).getFirst();

    assertNotNull(response);
    assertEquals(TradeAction.BUY, response.getOverallSignal());
    assertEquals("Positive Crossover", response.getMacdValue());
    assertEquals(TradeAction.BUY, response.getMacdSignal());
    assertEquals("Positive Crossover", response.getStochasticValue());
    assertEquals(TradeAction.BUY, response.getStochasticSignal());
    assertEquals(TradeAction.BUY, response.getRsiSignal());
    assertEquals(TradeAction.BUY, response.getVolumeSignal());
    assertEquals(TradeAction.STRONG_BUY, response.getEmaSignal());

    verify(astaResultsRepository, times(1)).saveAll(anyList());
  }

  @Test
  void testComputeAnalysisContinuesOnTickerFailure() {
    Ticker ticker1 = Ticker.builder().tickerSymbol("T1").isActive(true).build();
    Ticker ticker2 = Ticker.builder().tickerSymbol("T2").isActive(true).build();
    when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(ticker1, ticker2));

    // Stub missing dates
    when(dailyPriceRepository.findEarliestDateMissingAnalysis(ticker1))
        .thenReturn(Optional.of(TODAY));
    when(dailyPriceRepository.findEarliestDateMissingAnalysis(ticker2))
        .thenReturn(Optional.of(TODAY));

    // Let T1 throw an exception during daily price fetch
    LocalDate start = TODAY.minusDays(HISTORY_WINDOW);
    when(dailyPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            eq(ticker1), eq(start)))
        .thenThrow(new RuntimeException("Injected data fetch error"));

    // Setup successful mocks for T2
    List<DailyPrice> dailyPrices2 = List.of(dPrice(TODAY, 100, 105, 95, 100, 1000));
    when(dailyPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            eq(ticker2), eq(start)))
        .thenReturn(dailyPrices2);
    when(dailyIndicatorRepository.findSeriesFrom(eq(ticker2), any(), eq(start)))
        .thenReturn(List.of());
    when(weeklyIndicatorRepository.findSeriesFrom(eq(ticker2), any(), eq(start)))
        .thenReturn(List.of());
    when(astaResultsRepository.findByTickerAndPriceDateGreaterThanEqual(ticker2, TODAY))
        .thenReturn(List.of());

    when(astaResultsRepository.saveAll(anyList()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Run
    astaStrategy.computeTechnicalAnalysis();

    // Verify that only T2 was successfully saved
    verify(astaResultsRepository, times(1)).saveAll(anyList());
  }

  @Test
  void testEvaluateDailyStochasticFlatReturnsHold() {
    Ticker ticker = Ticker.builder().tickerSymbol(SYMBOL).build();
    when(tickerRepository.findByTickerSymbolIgnoreCase(SYMBOL)).thenReturn(Optional.of(ticker));

    // Setup daily candles
    List<DailyPrice> dailyPrices =
        List.of(
            dPrice(YESTERDAY, 102, 106, 101, 105, 1200), dPrice(TODAY, 105, 110, 104, 109, 1500));

    // Setup daily Stochastic where latest %K == %D (both 80)
    IndicatorDefinition stochDef =
        IndicatorDefinition.builder()
            .indicatorId(6L)
            .indicatorType(IndicatorType.STOCHASTIC)
            .source(PriceSource.CLOSE)
            .params(Map.of("k", 14, "kSmooth", 3, "dSmooth", 3))
            .build();
    List<DailyIndicator> dailyIndicators =
        List.of(
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(stochDef)
                .priceDate(YESTERDAY)
                .values(Map.of("k", BigDecimal.valueOf(70), "d", BigDecimal.valueOf(75)))
                .build(),
            DailyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(stochDef)
                .priceDate(TODAY)
                .values(Map.of("k", BigDecimal.valueOf(80), "d", BigDecimal.valueOf(80)))
                .build());

    mockRangeQueries(ticker, dailyPrices, dailyIndicators, List.of());

    when(astaResultsRepository.saveAll(anyList()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Run
    ASTAResults response = astaStrategy.processTicker(ticker, TODAY).getFirst();

    // Verify stochastic signal is HOLD and value is "K = D"
    assertNotNull(response);
    assertEquals(TradeAction.HOLD, response.getStochasticSignal());
    assertEquals("K = D", response.getStochasticValue());
  }

  @Test
  void testEvaluateWeeklyMacdFlatReturnsHold() {
    Ticker ticker = Ticker.builder().tickerSymbol(SYMBOL).build();
    when(tickerRepository.findByTickerSymbolIgnoreCase(SYMBOL)).thenReturn(Optional.of(ticker));

    // Setup daily candles
    List<DailyPrice> dailyPrices =
        List.of(
            dPrice(YESTERDAY, 102, 106, 101, 105, 1200), dPrice(TODAY, 105, 110, 104, 109, 1500));

    // Setup weekly MACD where latest macd == signal (both 1.5)
    IndicatorDefinition macdDef =
        IndicatorDefinition.builder()
            .indicatorId(7L)
            .indicatorType(IndicatorType.MACD)
            .source(PriceSource.CLOSE)
            .params(Map.of("fast", 12, "slow", 26, "signal", 9))
            .build();
    List<WeeklyIndicator> weeklyIndicators =
        List.of(
            WeeklyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(macdDef)
                .priceDate(YESTERDAY)
                .values(Map.of("macd", BigDecimal.valueOf(1.0), "signal", BigDecimal.valueOf(1.2)))
                .build(),
            WeeklyIndicator.builder()
                .ticker(ticker)
                .indicatorDefinition(macdDef)
                .priceDate(TODAY)
                .values(Map.of("macd", BigDecimal.valueOf(1.5), "signal", BigDecimal.valueOf(1.5)))
                .build());

    mockRangeQueries(ticker, dailyPrices, List.of(), weeklyIndicators);

    when(astaResultsRepository.saveAll(anyList()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Run
    ASTAResults response = astaStrategy.processTicker(ticker, TODAY).getFirst();

    // Verify MACD signal is HOLD and value is "MACD = Signal"
    assertNotNull(response);
    assertEquals(TradeAction.HOLD, response.getMacdSignal());
    assertEquals("MACD = Signal", response.getMacdValue());
  }
}
