package com.alphaflow.engine.calculators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.dtos.Bucket;
import com.alphaflow.engine.calculators.enums.LevelType;
import com.alphaflow.engine.configs.SupportResistanceConfig;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.structure.DowAtrSwingDetector;
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
  private SupportResistanceConfig config;
  private DowAtrSwingDetector swingDetector;
  private SupportResistanceCalculator calculator;
  private Ticker ticker;

  @BeforeEach
  void setUp() {
    tickerRepo = mock(TickerRepository.class);
    dailyPriceRepo = mock(DailyPriceRepository.class);
    weeklyPriceRepo = mock(WeeklyPriceRepository.class);
    dailySrRepo = mock(DailySupportResistanceRepository.class);
    weeklySrRepo = mock(WeeklySupportResistanceRepository.class);
    config = new SupportResistanceConfig();
    config.setAtrPeriod(3);
    config.setSwingAtrMultiplier(new BigDecimal("2.0"));
    config.setZoneAtrMultiplier(new BigDecimal("1.0"));
    config.setMinTouchCount(2);
    swingDetector = new DowAtrSwingDetector();

    calculator =
        new SupportResistanceCalculator(
            tickerRepo,
            dailyPriceRepo,
            weeklyPriceRepo,
            dailySrRepo,
            weeklySrRepo,
            config,
            swingDetector);

    ticker = Ticker.builder().tickerId(1L).tickerSymbol("AAPL").isActive(true).build();
  }

  private PriceBar bar(int dayOffset, int open, int high, int low, int close) {
    return new PriceBar(
        EPOCH.plusDays(dayOffset),
        BigDecimal.valueOf(open),
        BigDecimal.valueOf(high),
        BigDecimal.valueOf(low),
        BigDecimal.valueOf(close),
        BigDecimal.TEN);
  }

  /** Appends steady 10-point bars stepping from {@code startPrice} to {@code endPrice}. */
  private void appendLeg(List<PriceBar> bars, int startPrice, int endPrice) {
    int current = startPrice;
    int step = (endPrice >= startPrice) ? 10 : -10;
    while (current != endPrice) {
      int next = current + step;
      if ((step > 0 && next > endPrice) || (step < 0 && next < endPrice)) {
        next = endPrice;
      }
      int high = Math.max(current, next);
      int low = Math.min(current, next);
      bars.add(bar(bars.size(), current, high, low, next));
      current = next;
    }
  }

  private DailyPrice dailyFrom(PriceBar priceBar) {
    return DailyPrice.builder()
        .ticker(ticker)
        .priceDate(priceBar.date())
        .priceOpen(priceBar.open())
        .priceHigh(priceBar.high())
        .priceLow(priceBar.low())
        .priceClose(priceBar.close())
        .volume(priceBar.volume())
        .build();
  }

  private WeeklyPrice weeklyFrom(PriceBar priceBar) {
    return WeeklyPrice.builder()
        .ticker(ticker)
        .priceDate(priceBar.date())
        .priceOpen(priceBar.open())
        .priceHigh(priceBar.high())
        .priceLow(priceBar.low())
        .priceClose(priceBar.close())
        .volume(priceBar.volume())
        .build();
  }

  /**
   * Builds a steady 10-point-ATR series forming:
   * - Swing High #1 = 150, Swing High #2 = 151 (cluster around 150.5, zone ~ [145.5, 155.5])
   * - Swing Low #1 = 100, Swing Low #2 = 101 (cluster around 100.5, zone ~ [95.5, 105.5])
   * Ends at {@code finalClose} via a steady multi-bar leg (preserving ATR ~ 10).
   */
  private List<PriceBar> buildOscillatingBars(int finalClose) {
    List<PriceBar> bars = new ArrayList<>();
    // Seed ATR(3) = 10
    bars.add(bar(0, 120, 125, 115, 120));
    bars.add(bar(1, 120, 125, 115, 120));
    bars.add(bar(2, 120, 125, 115, 120));
    // Leg 1: 120 -> 150 (Peak #1 = 150)
    appendLeg(bars, 120, 150);
    // Leg 2: 150 -> 100 (confirms Peak #1 = 150; Trough #1 = 100)
    appendLeg(bars, 150, 100);
    // Leg 3: 100 -> 151 (confirms Trough #1 = 100; Peak #2 = 151)
    appendLeg(bars, 100, 151);
    // Leg 4: 151 -> 101 (confirms Peak #2 = 151; Trough #2 = 101)
    appendLeg(bars, 151, 101);
    // Leg 5: 101 -> 131 (confirms Trough #2 = 101)
    appendLeg(bars, 101, 131);
    // Final leg to desired close (without triggering a new swing unless intended)
    appendLeg(bars, 131, finalClose);
    return bars;
  }

  @Test
  void testComputeSupportResistancesOrchestratorSuccessAndErrorIsolation() {
    Ticker okTicker = Ticker.builder().tickerId(1L).tickerSymbol("AAPL").isActive(true).build();
    Ticker failingTicker =
        Ticker.builder().tickerId(2L).tickerSymbol("FAIL").isActive(true).build();

    when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(okTicker, failingTicker));
    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(okTicker)).thenReturn(List.of());
    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(failingTicker))
        .thenThrow(new RuntimeException("DB error"));

    calculator.computeSupportResistances(Timeframe.DAILY);

    verify(dailyPriceRepo).findByTickerOrderByPriceDateAsc(okTicker);
    verify(dailyPriceRepo).findByTickerOrderByPriceDateAsc(failingTicker);
  }

  @Test
  void testComputeSupportResistancesUsesSelfProxyWhenInjected() throws Exception {
    SupportResistanceCalculator mockSelf = mock(SupportResistanceCalculator.class);
    Field selfField = SupportResistanceCalculator.class.getDeclaredField("self");
    selfField.setAccessible(true);
    selfField.set(calculator, mockSelf);

    when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(ticker));

    calculator.computeSupportResistances(Timeframe.WEEKLY);

    verify(mockSelf).computeSupportResistancesForTicker(ticker, Timeframe.WEEKLY);
  }

  @Test
  void testComputeSupportResistancesForTickerDailyAndWeeklyEndToEnd() {
    List<PriceBar> bars = buildOscillatingBars(125);
    List<DailyPrice> dailyPrices = bars.stream().map(this::dailyFrom).toList();
    List<WeeklyPrice> weeklyPrices = bars.stream().map(this::weeklyFrom).toList();

    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(dailyPrices);
    when(weeklyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(weeklyPrices);
    when(dailySrRepo.findByTickerAndPriceDate(eq(ticker), any())).thenReturn(List.of());
    when(weeklySrRepo.findByTickerAndPriceDate(eq(ticker), any())).thenReturn(List.of());

    calculator.computeSupportResistancesForTicker(ticker, Timeframe.DAILY);
    calculator.computeSupportResistancesForTicker(ticker, Timeframe.WEEKLY);

    LocalDate latestDate = bars.getLast().date();
    verify(dailySrRepo).deleteByTickerAndPriceDateLessThan(ticker, latestDate.minusDays(30));
    verify(weeklySrRepo).deleteByTickerAndPriceDateLessThan(ticker, latestDate.minusDays(180));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailySupportResistance>> dailyCaptor = ArgumentCaptor.forClass(List.class);
    verify(dailySrRepo).saveAll(dailyCaptor.capture());
    List<DailySupportResistance> savedDaily = dailyCaptor.getValue();
    assertEquals(2, savedDaily.size());
    assertEquals("RESISTANCE", savedDaily.get(0).getLevelType());
    assertEquals(2, savedDaily.get(0).getTouchCount());
    assertEquals("SUPPORT", savedDaily.get(1).getLevelType());
    assertEquals(2, savedDaily.get(1).getTouchCount());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<WeeklySupportResistance>> weeklyCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(weeklySrRepo).saveAll(weeklyCaptor.capture());
    assertEquals(2, weeklyCaptor.getValue().size());
  }

  @Test
  void testSkipsAlreadyComputedDailyAndWeekly() {
    List<PriceBar> bars = buildOscillatingBars(125);
    LocalDate latestDate = bars.getLast().date();

    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(ticker))
        .thenReturn(bars.stream().map(this::dailyFrom).toList());
    when(weeklyPriceRepo.findByTickerOrderByPriceDateAsc(ticker))
        .thenReturn(bars.stream().map(this::weeklyFrom).toList());
    when(dailySrRepo.findByTickerAndPriceDate(ticker, latestDate))
        .thenReturn(List.of(DailySupportResistance.builder().build()));
    when(weeklySrRepo.findByTickerAndPriceDate(ticker, latestDate))
        .thenReturn(List.of(WeeklySupportResistance.builder().build()));

    calculator.computeSupportResistancesForTicker(ticker, Timeframe.DAILY);
    calculator.computeSupportResistancesForTicker(ticker, Timeframe.WEEKLY);

    verify(dailySrRepo, never()).saveAll(any());
    verify(weeklySrRepo, never()).saveAll(any());
  }

  @Test
  void testDoesNotSaveWhenNoBucketsComputed() {
    List<PriceBar> flatBars = List.of(bar(0, 100, 105, 95, 100));
    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(ticker))
        .thenReturn(flatBars.stream().map(this::dailyFrom).toList());
    when(dailySrRepo.findByTickerAndPriceDate(eq(ticker), any())).thenReturn(List.of());

    calculator.computeSupportResistancesForTicker(ticker, Timeframe.DAILY);

    verify(dailySrRepo).deleteByTickerAndPriceDateLessThan(eq(ticker), any());
    verify(dailySrRepo, never()).saveAll(any());
  }

  @Test
  void testComputeBucketsReturnsEmptyWhenZoneWidthIsNonPositive() {
    config.setZoneAtrMultiplier(BigDecimal.ZERO);
    List<PriceBar> bars = buildOscillatingBars(125);

    List<Bucket> buckets = calculator.computeBuckets(bars);

    assertTrue(buckets.isEmpty());
  }

  @Test
  void testPolarityFlipReinforcesZoneAndFlipsRoleOnBreakout() {
    List<PriceBar> bars = new ArrayList<>();
    bars.add(bar(0, 120, 125, 115, 120));
    bars.add(bar(1, 120, 125, 115, 120));
    bars.add(bar(2, 120, 125, 115, 120)); // Seed ATR(3) = 10
    // Peak #1 at 150
    appendLeg(bars, 120, 150);
    final LocalDate peakDate = bars.getLast().date();
    // Pullback to Trough #1 at 110 (confirms Peak #1 = 150)
    appendLeg(bars, 150, 110);
    // Breakout rally through 150 to Peak #2 at 200 (confirms Trough #1 = 110)
    appendLeg(bars, 110, 200);
    // Pullback to Trough #2 at 151 (confirms Peak #2 = 200)
    appendLeg(bars, 200, 151);
    final LocalDate retestDate = bars.getLast().date();
    // Rally to 185 (confirms Trough #2 = 151 -> clusters with Peak #1 = 150!)
    appendLeg(bars, 151, 185);

    List<Bucket> buckets = calculator.computeBuckets(bars);

    // Isolated pivots (110 and 200) have 1 touch (< 2) and are excluded.
    // Polarity zone around 150.5 has 2 touches (1 HIGH + 1 LOW), and close (185) > top -> SUPPORT!
    assertEquals(1, buckets.size());
    Bucket polarityZone = buckets.getFirst();
    assertEquals(2, polarityZone.getTouchCount());
    assertEquals(LevelType.SUPPORT, polarityZone.getLevelType());
    assertEquals(peakDate, polarityZone.getFirstTouchDate());
    assertEquals(retestDate, polarityZone.getLastTouchDate());
  }

  @Test
  void testInsideZoneRetainsLastSwingStructuralRole() {
    // 1. Price rallies into the ~150.5 zone (close=151, inside [~145.5, ~155.5]) -> stays RESISTANCE
    List<PriceBar> testingResistanceBars = buildOscillatingBars(151);
    List<Bucket> resistanceTestBuckets = calculator.computeBuckets(testingResistanceBars);
    Bucket upperZone = resistanceTestBuckets.getFirst();
    assertTrue(new BigDecimal("151").compareTo(upperZone.getBottom()) >= 0);
    assertTrue(new BigDecimal("151").compareTo(upperZone.getTop()) <= 0);
    assertEquals(
        LevelType.RESISTANCE,
        upperZone.getLevelType(),
        "Close inside resistance band without clearing top must remain RESISTANCE");

    // 2. Price pulls back into the ~100.5 zone (close=102, inside [~95.5, ~105.5]) -> stays SUPPORT
    List<PriceBar> testingSupportBars = buildOscillatingBars(102);
    List<Bucket> supportTestBuckets = calculator.computeBuckets(testingSupportBars);
    Bucket lowerZone = supportTestBuckets.getLast();
    assertTrue(new BigDecimal("102").compareTo(lowerZone.getBottom()) >= 0);
    assertTrue(new BigDecimal("102").compareTo(lowerZone.getTop()) <= 0);
    assertEquals(
        LevelType.SUPPORT,
        lowerZone.getLevelType(),
        "Close inside support band without breaking bottom must remain SUPPORT");
  }

  @Test
  void testApplyNonOverlappingSuppressionAndDeterministicTieBreaking() {
    Bucket strong =
        new Bucket(
            EPOCH.plusDays(20),
            new BigDecimal("100.0000"),
            new BigDecimal("110.0000"),
            LevelType.SUPPORT,
            5,
            EPOCH,
            EPOCH.plusDays(15));
    Bucket overlappingAbove =
        new Bucket(
            EPOCH.plusDays(20),
            new BigDecimal("105.0000"),
            new BigDecimal("115.0000"),
            LevelType.SUPPORT,
            3,
            EPOCH,
            EPOCH.plusDays(16));
    Bucket aboveNonOverlap =
        new Bucket(
            EPOCH.plusDays(20),
            new BigDecimal("120.0000"),
            new BigDecimal("130.0000"),
            LevelType.RESISTANCE,
            4,
            EPOCH,
            EPOCH.plusDays(12));
    Bucket belowNonOverlap =
        new Bucket(
            EPOCH.plusDays(20),
            new BigDecimal("80.0000"),
            new BigDecimal("90.0000"),
            LevelType.SUPPORT,
            4,
            EPOCH,
            EPOCH.plusDays(12));

    List<Bucket> selected =
        calculator.applyNonOverlappingSuppression(
            List.of(overlappingAbove, belowNonOverlap, strong, aboveNonOverlap));

    assertEquals(3, selected.size());
    assertFalse(selected.contains(overlappingAbove));
    assertEquals(aboveNonOverlap, selected.get(0));
    assertEquals(strong, selected.get(1));
    assertEquals(belowNonOverlap, selected.get(2));
  }

  @Test
  void testUnsupportedTimeframeBranchesThrowIllegalArgumentException() throws Exception {
    Method loadBarsMethod =
        SupportResistanceCalculator.class.getDeclaredMethod(
            "loadBars", Ticker.class, Timeframe.class);
    loadBarsMethod.setAccessible(true);
    InvocationTargetException loadException =
        assertThrows(
            InvocationTargetException.class,
            () -> loadBarsMethod.invoke(calculator, ticker, (Timeframe) null));
    assertTrue(loadException.getCause() instanceof IllegalArgumentException);

    Method isAlreadyComputedMethod =
        SupportResistanceCalculator.class.getDeclaredMethod(
            "isAlreadyComputed", Ticker.class, LocalDate.class, Timeframe.class);
    isAlreadyComputedMethod.setAccessible(true);
    InvocationTargetException existsException =
        assertThrows(
            InvocationTargetException.class,
            () -> isAlreadyComputedMethod.invoke(calculator, ticker, EPOCH, (Timeframe) null));
    assertTrue(existsException.getCause() instanceof IllegalArgumentException);

    Method deleteMethod =
        SupportResistanceCalculator.class.getDeclaredMethod(
            "deleteOlderSupportResistances", Ticker.class, LocalDate.class, Timeframe.class);
    deleteMethod.setAccessible(true);
    InvocationTargetException deleteException =
        assertThrows(
            InvocationTargetException.class,
            () -> deleteMethod.invoke(calculator, ticker, EPOCH, (Timeframe) null));
    assertTrue(deleteException.getCause() instanceof IllegalArgumentException);

    Method saveMethod =
        SupportResistanceCalculator.class.getDeclaredMethod(
            "saveSupportResistances", Ticker.class, List.class, Timeframe.class);
    saveMethod.setAccessible(true);
    Bucket sampleBucket =
        new Bucket(
            EPOCH,
            new BigDecimal("100.0000"),
            new BigDecimal("110.0000"),
            LevelType.SUPPORT,
            2,
            EPOCH,
            EPOCH);
    InvocationTargetException saveException =
        assertThrows(
            InvocationTargetException.class,
            () -> saveMethod.invoke(calculator, ticker, List.of(sampleBucket), (Timeframe) null));
    assertTrue(saveException.getCause() instanceof IllegalArgumentException);
  }
}
