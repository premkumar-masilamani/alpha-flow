package com.alphaflow.engine.calculators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.DailySupportResistance;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.entities.WeeklySupportResistance;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.DailySupportResistanceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import com.alphaflow.persistence.repositories.WeeklySupportResistanceRepository;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SupportResistanceCalculatorTest {

  private static final LocalDate EPOCH = LocalDate.of(2026, 1, 1);

  private TickerRepository tickerRepo;
  private DailyPriceRepository dailyPriceRepo;
  private WeeklyPriceRepository weeklyPriceRepo;
  private DailySupportResistanceRepository dailySrRepo;
  private WeeklySupportResistanceRepository weeklySrRepo;
  private SupportResistanceCalculator calculator;
  private Ticker ticker;

  @BeforeEach
  void setUp() {
    tickerRepo = mock(TickerRepository.class);
    dailyPriceRepo = mock(DailyPriceRepository.class);
    weeklyPriceRepo = mock(WeeklyPriceRepository.class);
    dailySrRepo = mock(DailySupportResistanceRepository.class);
    weeklySrRepo = mock(WeeklySupportResistanceRepository.class);

    calculator =
        new SupportResistanceCalculator(
            tickerRepo, dailyPriceRepo, weeklyPriceRepo, dailySrRepo, weeklySrRepo);

    ticker =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("TEST")
            .tickerName("Test Ticker")
            .isActive(true)
            .build();
  }

  @Test
  void testComputeSupportResistancesSuccessAndErrorHandling() {
    Ticker t1 = Ticker.builder().tickerId(1L).tickerSymbol("AAPL").isActive(true).build();
    Ticker t2 = Ticker.builder().tickerId(2L).tickerSymbol("MSFT").isActive(true).build();

    when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(t1, t2));

    SupportResistanceCalculator calculatorSpy = spy(calculator);
    doThrow(new RuntimeException("Calculation error"))
        .when(calculatorSpy)
        .computeSupportResistancesForTicker(t1);
    doNothing().when(calculatorSpy).computeSupportResistancesForTicker(t2);

    calculatorSpy.computeSupportResistances();

    verify(calculatorSpy).computeSupportResistancesForTicker(t1);
    verify(calculatorSpy).computeSupportResistancesForTicker(t2);
  }

  @Test
  void testComputeSupportResistancesUsesSelfProxyIfNotNull() throws Exception {
    Ticker t1 = Ticker.builder().tickerId(1L).tickerSymbol("AAPL").isActive(true).build();
    when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(t1));

    SupportResistanceCalculator selfMock = mock(SupportResistanceCalculator.class);

    Field selfField = SupportResistanceCalculator.class.getDeclaredField("self");
    selfField.setAccessible(true);
    selfField.set(calculator, selfMock);

    calculator.computeSupportResistances();

    verify(selfMock).computeSupportResistancesForTicker(t1);
  }

  @Test
  void testDailyAndWeeklyCalculationAndPersistence() {
    List<DailyPrice> daily = createDailyBars(5);
    List<WeeklyPrice> weekly = createWeeklyBars(5);

    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(daily);
    when(weeklyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(weekly);
    when(dailySrRepo.findByTickerAndPriceDate(eq(ticker), any())).thenReturn(List.of());
    when(weeklySrRepo.findByTickerAndPriceDate(eq(ticker), any())).thenReturn(List.of());

    calculator.computeSupportResistancesForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailySupportResistance>> dailyCaptor = ArgumentCaptor.forClass(List.class);
    verify(dailySrRepo).saveAll(dailyCaptor.capture());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<WeeklySupportResistance>> weeklyCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(weeklySrRepo).saveAll(weeklyCaptor.capture());
  }

  @Test
  void testDuplicatePriceDateSkipsPersistence() {
    List<DailyPrice> daily = createDailyBars(5);
    List<WeeklyPrice> weekly = createWeeklyBars(5);

    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(daily);
    when(weeklyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(weekly);

    DailySupportResistance existingDaily =
        DailySupportResistance.builder()
            .ticker(ticker)
            .priceDate(daily.getLast().getPriceDate())
            .build();
    WeeklySupportResistance existingWeekly =
        WeeklySupportResistance.builder()
            .ticker(ticker)
            .priceDate(weekly.getLast().getPriceDate())
            .build();

    when(dailySrRepo.findByTickerAndPriceDate(ticker, daily.getLast().getPriceDate()))
        .thenReturn(List.of(existingDaily));
    when(weeklySrRepo.findByTickerAndPriceDate(ticker, weekly.getLast().getPriceDate()))
        .thenReturn(List.of(existingWeekly));

    calculator.computeSupportResistancesForTicker(ticker);

    verify(dailySrRepo, never()).saveAll(any());
    verify(weeklySrRepo, never()).saveAll(any());
  }

  @Test
  void testEmptyBarsReturnsEarly() {
    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
    when(weeklyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());

    calculator.computeSupportResistancesForTicker(ticker);

    verify(dailySrRepo, never()).findByTickerAndPriceDate(any(), any());
    verify(weeklySrRepo, never()).findByTickerAndPriceDate(any(), any());
    verify(dailySrRepo, never()).saveAll(any());
    verify(weeklySrRepo, never()).saveAll(any());
  }

  @Test
  void testTouchAndSliceBucketScenarios() {
    // Generate bars to specifically test touchCount >= 3 and role reversal
    List<DailyPrice> bars = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      bars.add(
          DailyPrice.builder()
              .ticker(ticker)
              .priceDate(EPOCH.plusDays(i))
              .priceOpen(new BigDecimal("100.00"))
              .priceHigh(new BigDecimal("100.50"))
              .priceLow(new BigDecimal("99.50"))
              .priceClose(new BigDecimal("100.20"))
              .volume(BigDecimal.TEN)
              .build());
    }

    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(bars);
    when(weeklyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
    when(dailySrRepo.findByTickerAndPriceDate(eq(ticker), any())).thenReturn(List.of());

    calculator.computeSupportResistancesForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailySupportResistance>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailySrRepo).saveAll(captor.capture());
    assertFalse(captor.getValue().isEmpty());
  }

  @Test
  void testUnsupportedTimeframeInLoadBarsThrowsException() throws Exception {
    Method loadBarsMethod =
        SupportResistanceCalculator.class.getDeclaredMethod(
            "loadBars", Ticker.class, Timeframe.class);
    loadBarsMethod.setAccessible(true);

    InvocationTargetException ex =
        assertThrows(
            InvocationTargetException.class,
            () -> loadBarsMethod.invoke(calculator, ticker, (Timeframe) null));
    assertInstanceOf(IllegalArgumentException.class, ex.getCause());
  }

  @Test
  void testUnsupportedTimeframeInSaveSupportResistancesThrowsException() throws Exception {
    Method saveMethod =
        SupportResistanceCalculator.class.getDeclaredMethod(
            "saveSupportResistances", Ticker.class, Timeframe.class, List.class);
    saveMethod.setAccessible(true);

    Class<?> bucketClass =
        Class.forName("com.alphaflow.engine.calculators.SupportResistanceCalculator$Bucket");
    Constructor<?> ctor =
        bucketClass.getDeclaredConstructor(
            LocalDate.class,
            BigDecimal.class,
            BigDecimal.class,
            SupportResistanceCalculator.LevelType.class);
    ctor.setAccessible(true);
    Object bucket =
        ctor.newInstance(
            EPOCH,
            BigDecimal.TEN,
            BigDecimal.valueOf(11),
            SupportResistanceCalculator.LevelType.SUPPORT);

    InvocationTargetException ex =
        assertThrows(
            InvocationTargetException.class,
            () -> saveMethod.invoke(calculator, ticker, (Timeframe) null, List.of(bucket)));
    assertInstanceOf(IllegalArgumentException.class, ex.getCause());
  }

  @Test
  void testUnsupportedTimeframeInIsAlreadyComputedThrowsException() throws Exception {
    Method isAlreadyComputedMethod =
        SupportResistanceCalculator.class.getDeclaredMethod(
            "isAlreadyComputed", Ticker.class, LocalDate.class, Timeframe.class);
    isAlreadyComputedMethod.setAccessible(true);

    InvocationTargetException ex =
        assertThrows(
            InvocationTargetException.class,
            () -> isAlreadyComputedMethod.invoke(calculator, ticker, EPOCH, (Timeframe) null));
    assertInstanceOf(IllegalArgumentException.class, ex.getCause());
  }

  private List<DailyPrice> createDailyBars(int count) {
    List<DailyPrice> bars = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      bars.add(
          DailyPrice.builder()
              .ticker(ticker)
              .priceDate(EPOCH.plusDays(i))
              .priceOpen(new BigDecimal("100.00"))
              .priceHigh(new BigDecimal("105.00"))
              .priceLow(new BigDecimal("95.00"))
              .priceClose(new BigDecimal("100.00"))
              .volume(new BigDecimal("1000"))
              .build());
    }
    return bars;
  }

  private List<WeeklyPrice> createWeeklyBars(int count) {
    List<WeeklyPrice> bars = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      bars.add(
          WeeklyPrice.builder()
              .ticker(ticker)
              .priceDate(EPOCH.plusWeeks(i))
              .priceOpen(new BigDecimal("100.00"))
              .priceHigh(new BigDecimal("105.00"))
              .priceLow(new BigDecimal("95.00"))
              .priceClose(new BigDecimal("100.00"))
              .volume(new BigDecimal("5000"))
              .build());
    }
    return bars;
  }
}
