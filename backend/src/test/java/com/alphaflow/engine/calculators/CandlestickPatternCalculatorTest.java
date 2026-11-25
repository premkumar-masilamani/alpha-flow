package com.alphaflow.engine.calculators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.dtos.PatternMatch;
import com.alphaflow.persistence.dtos.TickerSyncState;
import com.alphaflow.persistence.entities.DailyCandlestickPattern;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyCandlestickPattern;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.enums.CandlestickPattern;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.repositories.DailyCandlestickPatternRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.WeeklyCandlestickPatternRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CandlestickPatternCalculatorTest {

  private static final LocalDate HISTORICAL_MONDAY = LocalDate.of(2025, 1, 6);

  private final Ticker ticker =
      Ticker.builder()
          .tickerId(1L)
          .tickerSymbol("AAPL")
          .tickerName("Apple")
          .country(Country.US)
          .build();

  @Mock private DailyPriceRepository dailyPriceRepository;
  @Mock private WeeklyPriceRepository weeklyPriceRepository;
  @Mock private DailyCandlestickPatternRepository dailyCandlestickPatternRepository;
  @Mock private WeeklyCandlestickPatternRepository weeklyCandlestickPatternRepository;

  private CandlestickPatternCalculator calculator;

  @BeforeEach
  void setUp() {
    calculator =
        new CandlestickPatternCalculator(
            dailyPriceRepository,
            weeklyPriceRepository,
            dailyCandlestickPatternRepository,
            weeklyCandlestickPatternRepository);
  }

  private DailyPrice daily(LocalDate date, String open, String high, String low, String close) {
    return DailyPrice.builder()
        .ticker(ticker)
        .priceDate(date)
        .priceOpen(new BigDecimal(open))
        .priceHigh(new BigDecimal(high))
        .priceLow(new BigDecimal(low))
        .priceClose(new BigDecimal(close))
        .volume(BigDecimal.TEN)
        .build();
  }

  private WeeklyPrice weekly(LocalDate date, String open, String high, String low, String close) {
    return WeeklyPrice.builder()
        .ticker(ticker)
        .priceDate(date)
        .priceOpen(new BigDecimal(open))
        .priceHigh(new BigDecimal(high))
        .priceLow(new BigDecimal(low))
        .priceClose(new BigDecimal(close))
        .volume(BigDecimal.TEN)
        .build();
  }

  @Test
  void testComputePatternsAllUpToDateShortCircuitsDailyAndWeekly() {
    Ticker noSourceTicker =
        Ticker.builder()
            .tickerId(2L)
            .tickerSymbol("NOSRC")
            .country(Country.US)
            .isActive(true)
            .build();

    when(dailyCandlestickPatternRepository.findSyncStatesForActiveTickers())
        .thenReturn(
            List.of(
                new TickerSyncState(ticker, LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 10)),
                new TickerSyncState(noSourceTicker, null, null)));
    when(weeklyCandlestickPatternRepository.findSyncStatesForActiveTickers())
        .thenReturn(
            List.of(
                new TickerSyncState(ticker, HISTORICAL_MONDAY, HISTORICAL_MONDAY),
                new TickerSyncState(noSourceTicker, null, null)));

    calculator.computeCandleStickPatterns(Timeframe.DAILY);
    calculator.computeCandleStickPatterns(Timeframe.WEEKLY);

    verify(dailyPriceRepository, never()).findByTickerOrderByPriceDateAsc(any());
    verify(weeklyPriceRepository, never()).findByTickerOrderByPriceDateAsc(any());
    verify(dailyCandlestickPatternRepository, never()).saveAll(any());
    verify(weeklyCandlestickPatternRepository, never()).saveAll(any());
  }

  @Test
  void testComputePatternsUnsupportedTimeframeThrows() throws Exception {
    assertThrows(
        IllegalArgumentException.class, () -> calculator.computeCandleStickPatterns(null));

    Method loadBarsMethod =
        CandlestickPatternCalculator.class.getDeclaredMethod(
            "loadBars", Ticker.class, Timeframe.class);
    loadBarsMethod.setAccessible(true);
    InvocationTargetException loadException =
        assertThrows(
            InvocationTargetException.class,
            () -> loadBarsMethod.invoke(calculator, ticker, (Timeframe) null));
    assertInstanceOf(IllegalArgumentException.class, loadException.getCause());

    Method savePatternsMethod =
        CandlestickPatternCalculator.class.getDeclaredMethod(
            "savePatterns", Ticker.class, Timeframe.class, List.class, LocalDate.class);
    savePatternsMethod.setAccessible(true);
    InvocationTargetException cleanupException =
        assertThrows(
            InvocationTargetException.class,
            () ->
                savePatternsMethod.invoke(
                    calculator, ticker, (Timeframe) null, List.of(), HISTORICAL_MONDAY));
    assertInstanceOf(IllegalArgumentException.class, cleanupException.getCause());

    PatternMatch sampleMatch =
        new PatternMatch(HISTORICAL_MONDAY, CandlestickPattern.LONG_WHITE_BODY);
    InvocationTargetException saveException =
        assertThrows(
            InvocationTargetException.class,
            () ->
                savePatternsMethod.invoke(
                    calculator, ticker, (Timeframe) null, List.of(sampleMatch), null));
    assertInstanceOf(IllegalArgumentException.class, saveException.getCause());
  }

  @Test
  void testComputePatternsForTimeframeSuccessErrorAndSelfProxy() throws Exception {
    Ticker activeTicker1 =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("T1")
            .country(Country.US)
            .isActive(true)
            .build();
    Ticker activeTicker2 =
        Ticker.builder()
            .tickerId(2L)
            .tickerSymbol("T2")
            .country(Country.US)
            .isActive(true)
            .build();
    Ticker upToDateTicker =
        Ticker.builder()
            .tickerId(3L)
            .tickerSymbol("T3")
            .country(Country.US)
            .isActive(true)
            .build();

    LocalDate sourceDate = LocalDate.of(2025, 1, 10);
    LocalDate priorDate = LocalDate.of(2025, 1, 9);
    when(dailyCandlestickPatternRepository.findSyncStatesForActiveTickers())
        .thenReturn(
            List.of(
                new TickerSyncState(activeTicker1, sourceDate, null),
                new TickerSyncState(activeTicker2, sourceDate, priorDate),
                new TickerSyncState(upToDateTicker, sourceDate, sourceDate)));

    CandlestickPatternCalculator calculatorSpy = spy(calculator);
    doThrow(new RuntimeException("Calculators fail"))
        .when(calculatorSpy)
        .computeCandleStickPatternsForTicker(activeTicker1, Timeframe.DAILY, null);
    doNothing()
        .when(calculatorSpy)
        .computeCandleStickPatternsForTicker(activeTicker2, Timeframe.DAILY, priorDate);

    calculatorSpy.computeCandleStickPatterns(Timeframe.DAILY);

    verify(calculatorSpy).computeCandleStickPatternsForTicker(activeTicker1, Timeframe.DAILY, null);
    verify(calculatorSpy)
        .computeCandleStickPatternsForTicker(activeTicker2, Timeframe.DAILY, priorDate);
    verify(calculatorSpy, never())
        .computeCandleStickPatternsForTicker(eq(upToDateTicker), any(), any());

    CandlestickPatternCalculator selfMock = mock(CandlestickPatternCalculator.class);
    java.lang.reflect.Field selfField =
        CandlestickPatternCalculator.class.getDeclaredField("selfProxy");
    selfField.setAccessible(true);
    selfField.set(calculator, selfMock);

    when(weeklyCandlestickPatternRepository.findSyncStatesForActiveTickers())
        .thenReturn(
            List.of(
                new TickerSyncState(activeTicker1, HISTORICAL_MONDAY, null),
                new TickerSyncState(activeTicker2, HISTORICAL_MONDAY, null),
                new TickerSyncState(upToDateTicker, HISTORICAL_MONDAY, HISTORICAL_MONDAY)));

    calculator.computeCandleStickPatterns(Timeframe.WEEKLY);

    verify(selfMock).computeCandleStickPatternsForTicker(activeTicker1, Timeframe.WEEKLY, null);
    verify(selfMock).computeCandleStickPatternsForTicker(activeTicker2, Timeframe.WEEKLY, null);
    verify(selfMock, never())
        .computeCandleStickPatternsForTicker(eq(upToDateTicker), any(), any());
  }

  @Test
  void testNoPatternsCalculatedWithInsufficientData() {
    LocalDate now = LocalDate.now();
    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker))
        .thenReturn(
            List.of(
                daily(now, "100", "105", "98", "102"),
                daily(now.plusDays(1), "100", "105", "98", "102"),
                daily(now.plusDays(2), "100", "105", "98", "102"),
                daily(now.plusDays(3), "100", "105", "98", "102")));

    calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.DAILY, null);

    verify(dailyCandlestickPatternRepository, never()).saveAll(any());
  }

  @Test
  void testNoPatternsSavedWhenSufficientBarsYieldNoMatches() {
    LocalDate base = LocalDate.of(2026, 1, 1);
    List<DailyPrice> neutralBars =
        List.of(
            daily(base, "100", "108", "97", "105"),
            daily(base.plusDays(1), "103", "111", "100", "108"),
            daily(base.plusDays(2), "106", "114", "103", "111"),
            daily(base.plusDays(3), "109", "117", "106", "114"),
            daily(base.plusDays(4), "112", "116", "105", "108"));
    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(neutralBars);

    calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.DAILY, null);

    verify(dailyCandlestickPatternRepository, never()).saveAll(any());
  }

  @Test
  void testLatestCalculatedDateNotFoundInBarsTriggersFullRecomputation() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "100", "101", "99", "100"));
    }
    prices.add(daily(base.plusDays(14), "100", "110", "100", "110"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);

    calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.DAILY, base.minusDays(5));

    verify(dailyCandlestickPatternRepository, never())
        .deleteByTickerAndPriceDateGreaterThanEqual(any(), any());
    verify(dailyCandlestickPatternRepository).saveAll(any());
  }

  @Test
  void testBullishMarubozuDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    // Add 14 baseline candles
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "100", "101", "99", "100")); // body = 0, avgBody = 0
    }
    // Now add a Bullish Marubozu
    // Body = 10, range = 10, open=100, close=110, high=110, low=100
    prices.add(daily(base.plusDays(14), "100", "110", "100", "110"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);

    calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.DAILY, null);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());

    List<DailyCandlestickPattern> saved = captor.getValue();
    assertThat(saved).anyMatch(pattern -> pattern.getPattern() == CandlestickPattern.LONG_WHITE_BODY);
  }

  @Test
  void testBearishMarubozuDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "100", "101", "99", "100"));
    }
    // Bearish Marubozu: open=110, close=100, high=110, low=100, range=10, body=10
    prices.add(daily(base.plusDays(14), "110", "110", "100", "100"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);

    calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.DAILY, null);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .anyMatch(pattern -> pattern.getPattern() == CandlestickPattern.LONG_BLACK_BODY);
  }

  @Test
  void testBullishEngulfingDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "100", "102", "98", "100"));
    }
    // Red candle: open=100, close=95
    prices.add(daily(base.plusDays(14), "100", "101", "94", "95"));
    // Green candle engulfing: open=94, close=101
    prices.add(daily(base.plusDays(15), "94", "102", "93", "101"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);

    calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.DAILY, null);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .anyMatch(pattern -> pattern.getPattern() == CandlestickPattern.BULLISH_ENGULFING);
  }

  @Test
  void testBearishEngulfingDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "100", "102", "98", "100"));
    }
    // Green candle: open=95, close=100
    prices.add(daily(base.plusDays(14), "95", "101", "94", "100"));
    // Red candle engulfing: open=101, close=94
    prices.add(daily(base.plusDays(15), "101", "102", "93", "94"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);

    calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.DAILY, null);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .anyMatch(pattern -> pattern.getPattern() == CandlestickPattern.BEARISH_ENGULFING);
  }

  @Test
  void testHammerDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    // Create SMA trend baseline (high prices) with body = 10
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "190", "215", "185", "200"));
    }
    // Downtrend price + Hammer
    prices.add(daily(base.plusDays(14), "110", "110.1", "107.5", "110.1"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);

    calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.DAILY, null);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).anyMatch(pattern -> pattern.getPattern() == CandlestickPattern.HAMMER);
  }

  @Test
  void testInvertedHammerDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "190", "215", "185", "200"));
    }
    prices.add(daily(base.plusDays(14), "110.1", "112.5", "110", "110"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);

    calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.DAILY, null);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .anyMatch(pattern -> pattern.getPattern() == CandlestickPattern.INVERTED_HAMMER);
  }

  @Test
  void testHangingManDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "90", "115", "85", "100"));
    }
    prices.add(daily(base.plusDays(14), "200", "200.1", "197.5", "200.1"));
    prices.add(daily(base.plusDays(15), "200", "201", "194", "195"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);

    calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.DAILY, null);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .anyMatch(pattern -> pattern.getPattern() == CandlestickPattern.HANGING_MAN);
  }

  @Test
  void testMorningStarDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "190", "215", "185", "200"));
    }
    prices.add(daily(base.plusDays(14), "115", "116", "104", "105"));
    prices.add(daily(base.plusDays(15), "99", "99.1", "98.8", "98.9"));
    prices.add(daily(base.plusDays(16), "99.5", "113", "99", "112"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);

    calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.DAILY, null);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .anyMatch(pattern -> pattern.getPattern() == CandlestickPattern.MORNING_STAR);
  }

  @Test
  void testEveningStarDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "90", "115", "85", "100"));
    }
    prices.add(daily(base.plusDays(14), "185", "196", "184", "195"));
    prices.add(daily(base.plusDays(15), "201", "201.2", "200.9", "201.1"));
    prices.add(daily(base.plusDays(16), "200.5", "201", "187", "188"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);

    calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.DAILY, null);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .anyMatch(pattern -> pattern.getPattern() == CandlestickPattern.EVENING_STAR);
  }

  @Test
  void testWeeklyCalculations() {
    List<WeeklyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(weekly(base.plusDays(j * 7), "100", "101", "99", "100"));
    }
    // Bullish Marubozu
    prices.add(weekly(base.plusDays(14 * 7), "100", "110", "100", "110"));

    when(weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);

    calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.WEEKLY, null);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<WeeklyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(weeklyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .anyMatch(pattern -> pattern.getPattern() == CandlestickPattern.LONG_WHITE_BODY);
  }

  @Test
  void testIncrementalCalculationWithLastDate() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "100", "101", "99", "100"));
    }
    prices.add(daily(base.plusDays(14), "100", "110", "100", "110"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);

    calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.DAILY, base.plusDays(14));

    verify(dailyCandlestickPatternRepository)
        .deleteByTickerAndPriceDateGreaterThanEqual(ticker, base.plusDays(10));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .anyMatch(
            pattern ->
                pattern.getPriceDate().equals(base.plusDays(14))
                    && pattern.getPattern() == CandlestickPattern.LONG_WHITE_BODY);
  }

  @Test
  void testWeeklyIncrementalCalculationWithLastDateIdempotentRefresh() {
    List<WeeklyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(weekly(base.plusDays(j * 7), "100", "101", "99", "100"));
    }
    prices.add(weekly(base.plusDays(14 * 7), "100", "110", "100", "110"));

    when(weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);

    // Run twice to confirm mid-week refresh idempotency
    calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.WEEKLY, base.plusDays(14 * 7));
    calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.WEEKLY, base.plusDays(14 * 7));

    verify(weeklyCandlestickPatternRepository, times(2))
        .deleteByTickerAndPriceDateGreaterThanEqual(ticker, base.plusDays(10 * 7));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<WeeklyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(weeklyCandlestickPatternRepository, times(2)).saveAll(captor.capture());
    assertThat(captor.getValue())
        .anyMatch(
            pattern ->
                pattern.getPriceDate().equals(base.plusDays(14 * 7))
                    && pattern.getPattern() == CandlestickPattern.LONG_WHITE_BODY);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testNewlyImplementedPatterns() {
    List<DailyPrice> base = new ArrayList<>();
    LocalDate start = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      base.add(daily(start.plusDays(j), "100", "103", "97", "102"));
    }

    // Helper to verify single patterns
    var patternsToTest =
        List.of(
            // A. Bullish Reversals
            new Object[] {
              CandlestickPattern.BULLISH_BELT_HOLD,
              new DailyPrice[] {daily(start.plusDays(14), "100", "108", "100", "108")}
            },
            new Object[] {
              CandlestickPattern.BULLISH_HARAMI,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "101", "90", "90"),
                daily(start.plusDays(15), "92", "98", "92", "98")
              }
            },
            new Object[] {
              CandlestickPattern.BULLISH_HARAMI_CROSS,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "101", "90", "90"),
                daily(start.plusDays(15), "95", "95", "95", "95")
              }
            },
            new Object[] {
              CandlestickPattern.PIERCING_LINE,
              new DailyPrice[] {
                daily(start.plusDays(14), "110", "111", "90", "90"),
                daily(start.plusDays(15), "89", "105", "89", "105")
              }
            },
            new Object[] {
              CandlestickPattern.BULLISH_DOJI_STAR,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "101", "90", "90"),
                daily(start.plusDays(15), "88", "88", "88", "88")
              }
            },
            new Object[] {
              CandlestickPattern.BULLISH_MEETING_LINES,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "101", "90", "90"),
                daily(start.plusDays(15), "85", "90.01", "85", "90.01")
              }
            },
            new Object[] {
              CandlestickPattern.THREE_WHITE_SOLDIERS,
              new DailyPrice[] {
                daily(start.plusDays(14), "90", "95", "90", "95"),
                daily(start.plusDays(15), "93", "98", "93", "98"),
                daily(start.plusDays(16), "96", "101", "96", "101")
              }
            },
            new Object[] {
              CandlestickPattern.MORNING_DOJI_STAR,
              new DailyPrice[] {
                daily(start.plusDays(14), "115", "116", "105", "105"),
                daily(start.plusDays(15), "99", "99", "99", "99"),
                daily(start.plusDays(16), "99.5", "112", "99.5", "112")
              }
            },
            new Object[] {
              CandlestickPattern.BULLISH_ABANDONED_BABY,
              new DailyPrice[] {
                daily(start.plusDays(14), "115", "116", "105", "105"),
                daily(start.plusDays(15), "99", "99.05", "98.9", "99"),
                daily(start.plusDays(16), "105", "115", "104.9", "115")
              }
            },
            new Object[] {
              CandlestickPattern.BULLISH_TRI_STAR,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "100.05", "100", "100"),
                daily(start.plusDays(15), "98", "98.05", "98", "98"),
                daily(start.plusDays(16), "99", "99.05", "99", "99")
              }
            },
            new Object[] {
              CandlestickPattern.BULLISH_BREAKAWAY,
              new DailyPrice[] {
                daily(start.plusDays(14), "120", "121", "110", "110"),
                daily(start.plusDays(15), "108", "108", "105", "105"),
                daily(start.plusDays(16), "104", "104", "101", "101"),
                daily(start.plusDays(17), "100", "100", "97", "97"),
                daily(start.plusDays(18), "96", "109", "96", "109")
              }
            },
            new Object[] {
              CandlestickPattern.THREE_INSIDE_UP,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "101", "90", "90"),
                daily(start.plusDays(15), "92", "98", "92", "98"),
                daily(start.plusDays(16), "97", "102", "97", "102")
              }
            },
            new Object[] {
              CandlestickPattern.THREE_OUTSIDE_UP,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "101", "95", "95"),
                daily(start.plusDays(15), "94", "101", "94", "101"),
                daily(start.plusDays(16), "100", "105", "100", "105")
              }
            },
            new Object[] {
              CandlestickPattern.BULLISH_KICKING,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "100", "90", "90"),
                daily(start.plusDays(15), "101", "111", "101", "111")
              }
            },
            new Object[] {
              CandlestickPattern.UNIQUE_THREE_RIVERS_BOTTOM,
              new DailyPrice[] {
                daily(start.plusDays(14), "120", "121", "110", "110"),
                daily(start.plusDays(15), "115", "115", "108", "112"),
                daily(start.plusDays(16), "110", "110.2", "110", "110.2")
              }
            },
            new Object[] {
              CandlestickPattern.THREE_STARS_IN_SOUTH,
              new DailyPrice[] {
                daily(start.plusDays(14), "120", "121", "105", "110"),
                daily(start.plusDays(15), "112", "112", "106", "108"),
                daily(start.plusDays(16), "109", "109", "108.9", "108.9")
              }
            },
            new Object[] {
              CandlestickPattern.CONCEALING_SWALLOW,
              new DailyPrice[] {
                daily(start.plusDays(14), "120", "120", "110", "110"),
                daily(start.plusDays(15), "110", "110", "100", "100"),
                daily(start.plusDays(16), "98", "102", "92", "92"),
                daily(start.plusDays(17), "103", "103", "91", "91")
              }
            },
            new Object[] {
              CandlestickPattern.BULLISH_STICK_SANDWICH,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "101", "90", "90"),
                daily(start.plusDays(15), "88", "98", "88", "98"),
                daily(start.plusDays(16), "100", "101", "90", "90")
              }
            },
            new Object[] {
              CandlestickPattern.HOMING_PIGEON,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "101", "90", "90"),
                daily(start.plusDays(15), "98", "98", "92", "92")
              }
            },
            new Object[] {
              CandlestickPattern.LADDER_BOTTOM,
              new DailyPrice[] {
                daily(start.plusDays(14), "120", "120", "110", "110"),
                daily(start.plusDays(15), "110", "110", "100", "100"),
                daily(start.plusDays(16), "100", "100", "90", "90"),
                daily(start.plusDays(17), "85", "95", "80", "80"),
                daily(start.plusDays(18), "92", "102", "92", "102")
              }
            },
            new Object[] {
              CandlestickPattern.MATCHING_LOW,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "101", "90", "90"),
                daily(start.plusDays(15), "95", "95", "90", "90")
              }
            },

            // B. Bearish Reversals
            new Object[] {
              CandlestickPattern.SHOOTING_STAR,
              new DailyPrice[] {daily(start.plusDays(14), "100", "102.5", "100", "100.1")}
            },
            new Object[] {
              CandlestickPattern.BEARISH_BELT_HOLD,
              new DailyPrice[] {daily(start.plusDays(14), "108", "108", "100", "100")}
            },
            new Object[] {
              CandlestickPattern.BEARISH_HARAMI,
              new DailyPrice[] {
                daily(start.plusDays(14), "90", "100", "90", "100"),
                daily(start.plusDays(15), "98", "98", "92", "92")
              }
            },
            new Object[] {
              CandlestickPattern.BEARISH_HARAMI_CROSS,
              new DailyPrice[] {
                daily(start.plusDays(14), "90", "100", "90", "100"),
                daily(start.plusDays(15), "95", "95", "95", "95")
              }
            },
            new Object[] {
              CandlestickPattern.DARK_CLOUD_COVER,
              new DailyPrice[] {
                daily(start.plusDays(14), "90", "100", "90", "100"),
                daily(start.plusDays(15), "105", "105", "92", "92")
              }
            },
            new Object[] {
              CandlestickPattern.BEARISH_DOJI_STAR,
              new DailyPrice[] {
                daily(start.plusDays(14), "90", "100", "90", "100"),
                daily(start.plusDays(15), "102", "102", "102", "102")
              }
            },
            new Object[] {
              CandlestickPattern.BEARISH_MEETING_LINES,
              new DailyPrice[] {
                daily(start.plusDays(14), "90", "100", "90", "100"),
                daily(start.plusDays(15), "105", "105", "100.01", "100.01")
              }
            },
            new Object[] {
              CandlestickPattern.THREE_BLACK_CROWS,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "100", "95", "95"),
                daily(start.plusDays(15), "97", "97", "92", "92"),
                daily(start.plusDays(16), "94", "94", "89", "89")
              }
            },
            new Object[] {
              CandlestickPattern.EVENING_DOJI_STAR,
              new DailyPrice[] {
                daily(start.plusDays(14), "85", "95", "85", "95"),
                daily(start.plusDays(15), "101", "101", "101", "101"),
                daily(start.plusDays(16), "100", "88", "100", "88")
              }
            },
            new Object[] {
              CandlestickPattern.BEARISH_ABANDONED_BABY,
              new DailyPrice[] {
                daily(start.plusDays(14), "85", "95", "85", "95"),
                daily(start.plusDays(15), "101", "101.05", "100.8", "101"),
                daily(start.plusDays(16), "95", "95", "85", "85")
              }
            },
            new Object[] {
              CandlestickPattern.BEARISH_TRI_STAR,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "100.05", "100", "100"),
                daily(start.plusDays(15), "102", "102.05", "102", "102"),
                daily(start.plusDays(16), "101", "101.05", "101", "101")
              }
            },
            new Object[] {
              CandlestickPattern.BEARISH_BREAKAWAY,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "110", "100", "110"),
                daily(start.plusDays(15), "112", "115", "112", "115"),
                daily(start.plusDays(16), "116", "119", "116", "119"),
                daily(start.plusDays(17), "120", "123", "120", "123"),
                daily(start.plusDays(18), "124", "124", "111", "111")
              }
            },
            new Object[] {
              CandlestickPattern.THREE_INSIDE_DOWN,
              new DailyPrice[] {
                daily(start.plusDays(14), "90", "100", "90", "100"),
                daily(start.plusDays(15), "98", "98", "92", "92"),
                daily(start.plusDays(16), "93", "93", "88", "88")
              }
            },
            new Object[] {
              CandlestickPattern.THREE_OUTSIDE_DOWN,
              new DailyPrice[] {
                daily(start.plusDays(14), "95", "100", "95", "100"),
                daily(start.plusDays(15), "101", "101", "94", "94"),
                daily(start.plusDays(16), "95", "95", "90", "90")
              }
            },
            new Object[] {
              CandlestickPattern.BEARISH_KICKING,
              new DailyPrice[] {
                daily(start.plusDays(14), "90", "100", "90", "100"),
                daily(start.plusDays(15), "89", "89", "79", "79")
              }
            },
            new Object[] {
              CandlestickPattern.LADDER_TOP,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "110", "100", "110"),
                daily(start.plusDays(15), "110", "120", "110", "120"),
                daily(start.plusDays(16), "120", "130", "120", "130"),
                daily(start.plusDays(17), "130", "145", "130", "135"),
                daily(start.plusDays(18), "128", "128", "118", "118")
              }
            },
            new Object[] {
              CandlestickPattern.MATCHING_HIGH,
              new DailyPrice[] {
                daily(start.plusDays(14), "90", "100", "90", "100"),
                daily(start.plusDays(15), "95", "100", "95", "100")
              }
            },
            new Object[] {
              CandlestickPattern.UPSIDE_GAP_TWO_CROWS,
              new DailyPrice[] {
                daily(start.plusDays(14), "90", "100", "90", "100"),
                daily(start.plusDays(15), "105", "105", "102", "102"),
                daily(start.plusDays(16), "106", "106", "101", "101")
              }
            },
            new Object[] {
              CandlestickPattern.IDENTICAL_THREE_CROWS,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "100", "95", "95"),
                daily(start.plusDays(15), "95", "95", "90", "90"),
                daily(start.plusDays(16), "90", "90", "85", "85")
              }
            },
            new Object[] {
              CandlestickPattern.DELIBERATION,
              new DailyPrice[] {
                daily(start.plusDays(14), "80", "90", "80", "90"),
                daily(start.plusDays(15), "90", "100", "90", "100"),
                daily(start.plusDays(16), "102", "105", "102", "103")
              }
            },
            new Object[] {
              CandlestickPattern.ADVANCE_BLOCK,
              new DailyPrice[] {
                daily(start.plusDays(14), "80", "91", "80", "90"),
                daily(start.plusDays(15), "89", "99", "89", "97"),
                daily(start.plusDays(16), "96", "103", "96", "100")
              }
            },
            new Object[] {
              CandlestickPattern.TWO_CROWS,
              new DailyPrice[] {
                daily(start.plusDays(14), "90", "100", "90", "100"),
                daily(start.plusDays(15), "105", "105", "102", "102"),
                daily(start.plusDays(16), "103", "103", "95", "95")
              }
            },

            // C. Bullish Continuation
            new Object[] {
              CandlestickPattern.BULLISH_SEPARATING_LINES,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "101", "90", "90"),
                daily(start.plusDays(15), "100", "110", "100", "110")
              }
            },
            new Object[] {
              CandlestickPattern.RISING_THREE_METHODS,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "110", "100", "110"),
                daily(start.plusDays(15), "109", "109", "106", "106"),
                daily(start.plusDays(16), "107", "107", "104", "104"),
                daily(start.plusDays(17), "105", "105", "102", "102"),
                daily(start.plusDays(18), "101", "112", "101", "112")
              }
            },
            new Object[] {
              CandlestickPattern.UPSIDE_TASUKI_GAP,
              new DailyPrice[] {
                daily(start.plusDays(14), "90", "100", "90", "100"),
                daily(start.plusDays(15), "105", "115", "105", "115"),
                daily(start.plusDays(16), "110", "110", "102", "102")
              }
            },
            new Object[] {
              CandlestickPattern.BULLISH_SIDE_BY_SIDE_WHITE_LINES,
              new DailyPrice[] {
                daily(start.plusDays(14), "90", "100", "90", "100"),
                daily(start.plusDays(15), "105", "115", "105", "115"),
                daily(start.plusDays(16), "105", "115", "105", "115")
              }
            },
            new Object[] {
              CandlestickPattern.BULLISH_THREE_LINE_STRIKE,
              new DailyPrice[] {
                daily(start.plusDays(14), "90", "95", "90", "95"),
                daily(start.plusDays(15), "95", "100", "95", "100"),
                daily(start.plusDays(16), "100", "105", "100", "105"),
                daily(start.plusDays(17), "106", "106", "89", "89")
              }
            },
            new Object[] {
              CandlestickPattern.UPSIDE_GAP_THREE_METHODS,
              new DailyPrice[] {
                daily(start.plusDays(14), "90", "100", "90", "100"),
                daily(start.plusDays(15), "105", "115", "105", "115"),
                daily(start.plusDays(16), "110", "110", "98", "98")
              }
            },
            new Object[] {
              CandlestickPattern.BULLISH_ON_NECK_LINE,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "101", "90", "90"),
                daily(start.plusDays(15), "85", "90", "85", "90")
              }
            },
            new Object[] {
              CandlestickPattern.BULLISH_IN_NECK_LINE,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "101", "90", "90"),
                daily(start.plusDays(15), "85", "90", "85", "90")
              }
            },

            // D. Bearish Continuation
            new Object[] {
              CandlestickPattern.BEARISH_SEPARATING_LINES,
              new DailyPrice[] {
                daily(start.plusDays(14), "90", "100", "90", "100"),
                daily(start.plusDays(15), "90", "80", "90", "80")
              }
            },
            new Object[] {
              CandlestickPattern.FALLING_THREE_METHODS,
              new DailyPrice[] {
                daily(start.plusDays(14), "110", "110", "100", "100"),
                daily(start.plusDays(15), "101", "104", "101", "104"),
                daily(start.plusDays(16), "103", "106", "103", "106"),
                daily(start.plusDays(17), "105", "108", "105", "108"),
                daily(start.plusDays(18), "109", "109", "98", "98")
              }
            },
            new Object[] {
              CandlestickPattern.DOWNSIDE_TASUKI_GAP,
              new DailyPrice[] {
                daily(start.plusDays(14), "110", "110", "100", "100"),
                daily(start.plusDays(15), "95", "95", "85", "85"),
                daily(start.plusDays(16), "90", "98", "90", "98")
              }
            },
            new Object[] {
              CandlestickPattern.BEARISH_SIDE_BY_SIDE_WHITE_LINES,
              new DailyPrice[] {
                daily(start.plusDays(14), "110", "110", "100", "100"),
                daily(start.plusDays(15), "85", "95", "85", "95"),
                daily(start.plusDays(16), "85", "95", "85", "95")
              }
            },
            new Object[] {
              CandlestickPattern.BEARISH_THREE_LINE_STRIKE,
              new DailyPrice[] {
                daily(start.plusDays(14), "110", "110", "105", "105"),
                daily(start.plusDays(15), "105", "105", "100", "100"),
                daily(start.plusDays(16), "100", "100", "95", "95"),
                daily(start.plusDays(17), "94", "111", "94", "111")
              }
            },
            new Object[] {
              CandlestickPattern.DOWNSIDE_GAP_THREE_METHODS,
              new DailyPrice[] {
                daily(start.plusDays(14), "110", "110", "100", "100"),
                daily(start.plusDays(15), "95", "95", "85", "85"),
                daily(start.plusDays(16), "90", "102", "90", "102")
              }
            },
            new Object[] {
              CandlestickPattern.BEARISH_ON_NECK_LINE,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "101", "90", "90"),
                daily(start.plusDays(15), "85", "90", "85", "90")
              }
            },
            new Object[] {
              CandlestickPattern.BEARISH_IN_NECK_LINE,
              new DailyPrice[] {
                daily(start.plusDays(14), "100", "101", "88", "90"),
                daily(start.plusDays(15), "85", "90.01", "85", "90.01")
              }
            });

    for (Object[] spec : patternsToTest) {
      DailyPrice[] adds = (DailyPrice[]) spec[1];

      List<DailyPrice> testPrices = new ArrayList<>(base);
      for (DailyPrice dailyPrice : adds) {
        testPrices.add(dailyPrice);
      }

      when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(testPrices);

      calculator.computeCandleStickPatternsForTicker(ticker, Timeframe.DAILY, null);

      ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
      verify(dailyCandlestickPatternRepository).saveAll(captor.capture());

      CandlestickPattern pattern = (CandlestickPattern) spec[0];
      List<DailyCandlestickPattern> saved = captor.getValue();
      boolean matched = saved.stream().anyMatch(p -> p.getPattern() == pattern);
      if (!matched) {
        System.out.println(
            "FAILED TO DETECT "
                + pattern
                + "! Saved patterns: "
                + saved.stream().map(p -> p.getPattern() + "@" + p.getPriceDate()).toList());
      }
      assertThat(saved)
          .withFailMessage("Failed to detect pattern: " + pattern)
          .anyMatch(p -> p.getPattern() == pattern);

      org.mockito.Mockito.reset(dailyCandlestickPatternRepository);
    }
  }

  @Test
  void testPipelineJobMethods() {
    assertThat(calculator.getJobName()).isEqualTo("CALCULATE_CANDLESTICK_PATTERN");

    LocalDate base = LocalDate.of(2026, 1, 1);
    when(dailyCandlestickPatternRepository.findSyncStatesForActiveTickers())
        .thenReturn(List.of(new TickerSyncState(ticker, base, base)));
    assertThat(calculator.shouldRun(Timeframe.DAILY)).isFalse();

    when(dailyCandlestickPatternRepository.findSyncStatesForActiveTickers())
        .thenReturn(List.of(new TickerSyncState(ticker, base.plusDays(1), base)));
    assertThat(calculator.shouldRun(Timeframe.DAILY)).isTrue();

    calculator.execute(Timeframe.DAILY);
  }
}
