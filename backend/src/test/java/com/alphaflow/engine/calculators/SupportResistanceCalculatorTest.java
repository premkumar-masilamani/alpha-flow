package com.alphaflow.engine.calculators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.alphaflow.engine.calculators.dtos.Bucket;
import com.alphaflow.engine.calculators.enums.LevelType;
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
    DailySupportResistance saved = captor.getValue().getFirst();
    org.junit.jupiter.api.Assertions.assertNotNull(saved.getFirstTouchDate());
    org.junit.jupiter.api.Assertions.assertNotNull(saved.getLastTouchDate());
  }

  @Test
  void testBucketTouchDatesTrackingAndReset() {
    Bucket bucket =
        new Bucket(EPOCH, BigDecimal.valueOf(100), BigDecimal.valueOf(105), LevelType.SUPPORT);
    org.junit.jupiter.api.Assertions.assertNull(bucket.getFirstTouchDate());
    org.junit.jupiter.api.Assertions.assertNull(bucket.getLastTouchDate());

    LocalDate d1 = LocalDate.of(2026, 1, 10);
    bucket.incrementTouchCount(d1);
    assertEquals(1, bucket.getTouchCount());
    assertEquals(d1, bucket.getFirstTouchDate());
    assertEquals(d1, bucket.getLastTouchDate());

    LocalDate d2 = LocalDate.of(2026, 1, 15);
    bucket.incrementTouchCount(d2);
    assertEquals(2, bucket.getTouchCount());
    assertEquals(d1, bucket.getFirstTouchDate());
    assertEquals(d2, bucket.getLastTouchDate());

    LocalDate d3 = LocalDate.of(2026, 1, 20);
    bucket.incrementTouchCount(d3);
    assertEquals(3, bucket.getTouchCount());
    assertEquals(d1, bucket.getFirstTouchDate());
    assertEquals(d3, bucket.getLastTouchDate());

    bucket.resetTouchCount();
    assertEquals(0, bucket.getTouchCount());
    org.junit.jupiter.api.Assertions.assertNull(bucket.getFirstTouchDate());
    org.junit.jupiter.api.Assertions.assertNull(bucket.getLastTouchDate());
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

    Bucket bucket = new Bucket(EPOCH, BigDecimal.TEN, BigDecimal.valueOf(11), LevelType.SUPPORT);

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

  @Test
  void testBucketConsecutiveBreachCountTrackingAndReset() {
    Bucket bucket =
        new Bucket(EPOCH, BigDecimal.valueOf(100), BigDecimal.valueOf(105), LevelType.SUPPORT);
    assertEquals(0, bucket.getConsecutiveBreachCount());

    bucket.incrementConsecutiveBreachCount();
    assertEquals(1, bucket.getConsecutiveBreachCount());

    bucket.incrementConsecutiveBreachCount();
    assertEquals(2, bucket.getConsecutiveBreachCount());

    bucket.resetConsecutiveBreachCount();
    assertEquals(0, bucket.getConsecutiveBreachCount());

    bucket.incrementConsecutiveBreachCount();
    assertEquals(1, bucket.getConsecutiveBreachCount());
    bucket.resetTouchCount();
    assertEquals(0, bucket.getConsecutiveBreachCount());
  }

  @Test
  void testSupportFalseBreakoutPreservesBucketAndAwardsTouch() {
    List<DailyPrice> bars = new ArrayList<>();
    // Day 0: Touch 1 (Support [99.00, 100.00])
    bars.add(createBar(0, "100.20", "100.50", "99.50", "99.80"));
    // Day 1: Touch 2
    bars.add(createBar(1, "100.20", "100.50", "99.20", "99.70"));
    // Day 2: False breakout breach (close < 99.00), breachCount becomes 1 <= 2
    bars.add(createBar(2, "99.50", "99.50", "98.00", "98.50"));
    // Day 3: Reclaim and Anchor bar (P = (100.80 + 99.20 + 100.00) / 3 = 100.00, W = 1.00)
    // Close >= 99.00, breachCount resets, credited as Touch 3
    bars.add(createBar(3, "99.50", "100.80", "99.20", "100.00"));

    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(bars);
    when(weeklyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
    when(dailySrRepo.findByTickerAndPriceDate(eq(ticker), any())).thenReturn(List.of());

    calculator.computeSupportResistancesForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailySupportResistance>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailySrRepo).saveAll(captor.capture());
    List<DailySupportResistance> saved = captor.getValue();
    assertFalse(saved.isEmpty());

    DailySupportResistance sr =
        saved.stream()
            .filter(
                s ->
                    s.getZoneBottom().compareTo(new BigDecimal("99.0000")) == 0
                        || s.getZoneBottom().compareTo(new BigDecimal("99")) == 0)
            .findFirst()
            .orElse(null);

    assertNotNull(sr, "Support bucket [99.00, 100.00] should be proven and saved");
    assertEquals(3, sr.getTouchCount());
    assertEquals(EPOCH, sr.getFirstTouchDate());
    assertEquals(EPOCH.plusDays(3), sr.getLastTouchDate());
  }

  @Test
  void testSupportConfirmedBreakdownResetsBucket() {
    List<DailyPrice> bars = new ArrayList<>();
    // Day 0: Touch 1
    bars.add(createBar(0, "100.20", "100.50", "99.50", "99.80"));
    // Day 1: Touch 2
    bars.add(createBar(1, "100.20", "100.50", "99.20", "99.70"));
    // Day 2: Breach 1 (close < 99.00) -> breachCount = 1
    bars.add(createBar(2, "99.50", "99.50", "98.00", "98.50"));
    // Day 3: Breach 2 (close < 99.00) -> breachCount = 2 >= 2 -> resetTouchCount()!
    bars.add(createBar(3, "98.50", "98.80", "97.50", "98.00"));
    // Day 4: Anchor bar (P = 100.00, W = 1.00)
    bars.add(createBar(4, "100.00", "100.00", "100.00", "100.00"));

    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(bars);
    when(weeklyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
    when(dailySrRepo.findByTickerAndPriceDate(eq(ticker), any())).thenReturn(List.of());

    calculator.computeSupportResistancesForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailySupportResistance>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailySrRepo).saveAll(captor.capture());
    List<DailySupportResistance> saved = captor.getValue();

    boolean hasProvenLevel =
        saved.stream()
            .anyMatch(
                s ->
                    s.getZoneBottom().compareTo(new BigDecimal("99.0000")) == 0
                        || s.getZoneBottom().compareTo(new BigDecimal("99")) == 0);
    assertFalse(hasProvenLevel, "Broken support bucket should NOT be saved as proven");
  }

  @Test
  void testResistanceFalseBreakoutPreservesBucketAndAwardsTouch() {
    List<DailyPrice> bars = new ArrayList<>();
    // Day 0: Touch 1 (Resistance [100.00, 101.00])
    bars.add(createBar(0, "99.50", "100.50", "99.00", "99.80"));
    // Day 1: Touch 2
    bars.add(createBar(1, "99.50", "100.80", "99.00", "99.70"));
    // Day 2: False breakout breach above (close > 101.00) -> breachCount = 1
    bars.add(createBar(2, "100.50", "102.50", "100.20", "101.80"));
    // Day 3: Reclaim and Anchor bar (P = (100.80 + 99.20 + 100.00) / 3 = 100.00, W = 1.00)
    // Close <= 101.00, breachCount resets, credited as Touch 3
    bars.add(createBar(3, "100.50", "100.80", "99.20", "100.00"));

    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(bars);
    when(weeklyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
    when(dailySrRepo.findByTickerAndPriceDate(eq(ticker), any())).thenReturn(List.of());

    calculator.computeSupportResistancesForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailySupportResistance>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailySrRepo).saveAll(captor.capture());
    List<DailySupportResistance> saved = captor.getValue();
    assertFalse(saved.isEmpty());

    DailySupportResistance sr =
        saved.stream()
            .filter(
                s ->
                    s.getZoneBottom().compareTo(new BigDecimal("100.0000")) == 0
                        || s.getZoneBottom().compareTo(new BigDecimal("100")) == 0)
            .findFirst()
            .orElse(null);

    assertNotNull(sr, "Resistance bucket [100.00, 101.00] should be proven and saved");
    assertEquals(3, sr.getTouchCount());
    assertEquals(EPOCH, sr.getFirstTouchDate());
    assertEquals(EPOCH.plusDays(3), sr.getLastTouchDate());
  }

  @Test
  void testResistanceConfirmedBreakoutResetsBucket() {
    List<DailyPrice> bars = new ArrayList<>();
    // Day 0: Touch 1
    bars.add(createBar(0, "99.50", "100.50", "99.00", "99.80"));
    // Day 1: Touch 2
    bars.add(createBar(1, "99.50", "100.80", "99.00", "99.70"));
    // Day 2: Breach 1 (close > 101.00) -> breachCount = 1
    bars.add(createBar(2, "100.50", "102.50", "100.20", "101.80"));
    // Day 3: Breach 2 (close > 101.00) -> breachCount = 2 >= 2 -> resetTouchCount()!
    bars.add(createBar(3, "101.50", "103.00", "101.20", "102.50"));
    // Day 4: Anchor bar (P = 100.00, W = 1.00)
    bars.add(createBar(4, "100.00", "100.00", "100.00", "100.00"));

    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(bars);
    when(weeklyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
    when(dailySrRepo.findByTickerAndPriceDate(eq(ticker), any())).thenReturn(List.of());

    calculator.computeSupportResistancesForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailySupportResistance>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailySrRepo).saveAll(captor.capture());
    List<DailySupportResistance> saved = captor.getValue();

    boolean hasProvenLevel =
        saved.stream()
            .anyMatch(
                s ->
                    s.getZoneBottom().compareTo(new BigDecimal("100.0000")) == 0
                        || s.getZoneBottom().compareTo(new BigDecimal("100")) == 0);
    assertFalse(hasProvenLevel, "Broken resistance bucket should NOT be saved as proven");
  }

  @Test
  void testIntraBarWickSweepOverlapRegistersTouch() {
    List<DailyPrice> bars = new ArrayList<>();
    // Low dips down to 98.20 (< 99.00), but close defends at 99.50 (>= 99.00) -> Touch 1
    bars.add(createBar(0, "100.20", "100.50", "98.20", "99.50"));
    // Day 1: Touch 2
    bars.add(createBar(1, "100.20", "100.50", "98.50", "99.60"));
    // Day 2: Touch 3 & Anchor Bar (P = (100.80 + 98.50 + 100.70) / 3 = 100.00, W = 1.00)
    bars.add(createBar(2, "100.20", "100.80", "98.50", "100.70"));

    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(bars);
    when(weeklyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
    when(dailySrRepo.findByTickerAndPriceDate(eq(ticker), any())).thenReturn(List.of());

    calculator.computeSupportResistancesForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailySupportResistance>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailySrRepo).saveAll(captor.capture());
    List<DailySupportResistance> saved = captor.getValue();

    DailySupportResistance sr =
        saved.stream()
            .filter(
                s ->
                    s.getZoneBottom().compareTo(new BigDecimal("99.0000")) == 0
                        || s.getZoneBottom().compareTo(new BigDecimal("99")) == 0)
            .findFirst()
            .orElse(null);

    assertNotNull(sr, "Support bucket [99.00, 100.00] should register wick sweep touches");
    assertEquals(3, sr.getTouchCount());
    assertEquals(EPOCH, sr.getFirstTouchDate());
    assertEquals(EPOCH.plusDays(2), sr.getLastTouchDate());
  }

  @Test
  void testLegacyBehaviorWhenConfirmationBarsIsOne() throws Exception {
    Field field = SupportResistanceCalculator.class.getDeclaredField("breakoutConfirmationBars");
    field.setAccessible(true);
    field.set(calculator, 1);

    List<DailyPrice> bars = new ArrayList<>();
    // Day 0: Touch 1
    bars.add(createBar(0, "100.20", "100.50", "99.50", "99.80"));
    // Day 1: Touch 2
    bars.add(createBar(1, "100.20", "100.50", "99.20", "99.70"));
    // Day 2: Single breach (close < 99.00) -> immediately invalidates when confirmation bars = 1
    bars.add(createBar(2, "99.50", "99.50", "98.00", "98.50"));
    // Day 3: Reclaim! Touch 1
    bars.add(createBar(3, "99.00", "100.50", "99.20", "100.20"));
    // Day 4: Anchor bar (P = 100.00, W = 1.00)
    bars.add(createBar(4, "100.00", "100.00", "100.00", "100.00"));

    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(bars);
    when(weeklyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
    when(dailySrRepo.findByTickerAndPriceDate(eq(ticker), any())).thenReturn(List.of());

    calculator.computeSupportResistancesForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailySupportResistance>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailySrRepo).saveAll(captor.capture());
    List<DailySupportResistance> saved = captor.getValue();

    boolean hasProvenLevel =
        saved.stream()
            .anyMatch(
                s ->
                    s.getZoneBottom().compareTo(new BigDecimal("99.0000")) == 0
                        || s.getZoneBottom().compareTo(new BigDecimal("99")) == 0);
    assertFalse(
        hasProvenLevel,
        "Support bucket should have been invalidated by single breach when confirmation bars = 1");
  }

  private DailyPrice createBar(int dayOffset, String open, String high, String low, String close) {
    return DailyPrice.builder()
        .ticker(ticker)
        .priceDate(EPOCH.plusDays(dayOffset))
        .priceOpen(new BigDecimal(open))
        .priceHigh(new BigDecimal(high))
        .priceLow(new BigDecimal(low))
        .priceClose(new BigDecimal(close))
        .volume(BigDecimal.TEN)
        .build();
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
