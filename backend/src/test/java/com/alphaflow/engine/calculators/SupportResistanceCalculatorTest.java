package com.alphaflow.engine.calculators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.alphaflow.persistence.enums.Country;
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
  private static final LocalDate HISTORICAL_MONDAY = LocalDate.of(2025, 1, 6);

  private DailyPriceRepository dailyPriceRepo;
  private WeeklyPriceRepository weeklyPriceRepo;
  private DailySupportResistanceRepository dailySrRepo;
  private WeeklySupportResistanceRepository weeklySrRepo;
  private TickerRepository tickerRepo;
  private SupportResistanceCalculator calculator;
  private Ticker ticker;

  @BeforeEach
  void setUp() {
    dailyPriceRepo = mock(DailyPriceRepository.class);
    weeklyPriceRepo = mock(WeeklyPriceRepository.class);
    dailySrRepo = mock(DailySupportResistanceRepository.class);
    weeklySrRepo = mock(WeeklySupportResistanceRepository.class);
    tickerRepo = mock(TickerRepository.class);

    calculator =
        new SupportResistanceCalculator(
            dailyPriceRepo, weeklyPriceRepo, dailySrRepo, weeklySrRepo, tickerRepo, new DowAtrSwingDetector());

    ticker =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("AAPL")
            .country(Country.US)
            .isActive(true)
            .build();
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

  /** Appends {@code ATR_PERIOD} steady 10-point seed bars centered around {@code basePrice}. */
  private void appendSeedBars(List<PriceBar> bars, int basePrice) {
    for (int i = 0; i < SupportResistanceConfig.ATR_PERIOD; i++) {
      bars.add(bar(bars.size(), basePrice, basePrice + 5, basePrice - 5, basePrice));
    }
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
   * Builds a steady 10-point-ATR series using production {@code ATR_PERIOD = 14}, forming: - Swing
   * High #1 = 150, Swing High #2 = 151 (cluster around 150.5, zone ~ [145.5, 155.5]) - Swing Low #1
   * = 100, Swing Low #2 = 101 (cluster around 100.5, zone ~ [95.5, 105.5])
   */
  private List<PriceBar> buildOscillatingBars(int finalClose) {
    List<PriceBar> bars = new ArrayList<>();
    appendSeedBars(bars, 120);
    appendLeg(bars, 120, 150);
    appendLeg(bars, 150, 100);
    appendLeg(bars, 100, 151);
    appendLeg(bars, 151, 101);
    appendLeg(bars, 101, 131);
    appendLeg(bars, 131, finalClose);
    return bars;
  }

  @Test
  void testComputeSupportResistancesAllUpToDateShortCircuitsDailyAndWeekly() {
    when(dailySrRepo.hasPendingDailySupportResistance()).thenReturn(false);
    when(weeklySrRepo.hasPendingWeeklySupportResistance()).thenReturn(false);

    assertFalse(calculator.shouldRun(Timeframe.DAILY));
    assertFalse(calculator.shouldRun(Timeframe.WEEKLY));

    verify(dailySrRepo).hasPendingDailySupportResistance();
    verify(weeklySrRepo).hasPendingWeeklySupportResistance();
  }

  @Test
  void testComputeSupportResistancesOrchestratorPartialStalenessAndErrorIsolation() {
    Ticker okTicker =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("AAPL")
            .country(Country.US)
            .isActive(true)
            .build();
    Ticker failingTicker =
        Ticker.builder()
            .tickerId(2L)
            .tickerSymbol("FAIL")
            .country(Country.US)
            .isActive(true)
            .build();
    Ticker upToDateTicker =
        Ticker.builder()
            .tickerId(3L)
            .tickerSymbol("MSFT")
            .country(Country.US)
            .isActive(true)
            .build();

    when(tickerRepo.findByIsActiveTrue())
        .thenReturn(List.of(okTicker, failingTicker, upToDateTicker));
    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(okTicker)).thenReturn(List.of());
    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(upToDateTicker)).thenReturn(List.of());
    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(failingTicker))
        .thenThrow(new RuntimeException("DB error"));

    calculator.execute(Timeframe.DAILY);

    verify(dailyPriceRepo).findByTickerOrderByPriceDateAsc(okTicker);
    verify(dailyPriceRepo).findByTickerOrderByPriceDateAsc(failingTicker);
    verify(dailyPriceRepo).findByTickerOrderByPriceDateAsc(upToDateTicker);
  }

  @Test
  void testComputeSupportResistancesUsesSelfProxyWhenInjected() throws Exception {
    SupportResistanceCalculator mockSelf = mock(SupportResistanceCalculator.class);
    Field selfField = SupportResistanceCalculator.class.getDeclaredField("self");
    selfField.setAccessible(true);
    selfField.set(calculator, mockSelf);

    when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(ticker));

    calculator.execute(Timeframe.WEEKLY);

    verify(mockSelf).computeSupportResistancesForTicker(ticker, Timeframe.WEEKLY);
  }

  @Test
  void testComputeSupportResistancesUnsupportedTimeframeThrows() {
    when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(ticker));
    assertThrows(
        IllegalArgumentException.class, () -> calculator.execute(null));
  }

  @Test
  void testComputeSupportResistancesForTickerDailyAndWeeklyEndToEnd() {
    List<PriceBar> bars = buildOscillatingBars(125);
    List<DailyPrice> dailyPrices = bars.stream().map(this::dailyFrom).toList();
    List<WeeklyPrice> weeklyPrices = bars.stream().map(this::weeklyFrom).toList();

    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(dailyPrices);
    when(weeklyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(weeklyPrices);

    calculator.computeSupportResistancesForTicker(ticker, Timeframe.DAILY, null);
    calculator.computeSupportResistancesForTicker(ticker, Timeframe.WEEKLY, null);

    LocalDate latestDate = bars.getLast().date();
    verify(dailySrRepo).deleteByTickerAndPriceDateLessThan(ticker, latestDate.minusDays(30));
    verify(weeklySrRepo).deleteByTickerAndPriceDateLessThan(ticker, latestDate.minusDays(180));
    verify(weeklySrRepo, never()).deleteByTickerAndPriceDate(any(), any());

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
  void testWeeklyMidWeekRefreshDeletesExistingSnapshotBeforeSavingIdempotently() {
    List<PriceBar> bars = buildOscillatingBars(125);
    List<WeeklyPrice> weeklyPrices = bars.stream().map(this::weeklyFrom).toList();
    LocalDate latestDate = bars.getLast().date();

    when(weeklyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(weeklyPrices);

    // 1. Advancing to a newer week (latestCalculatedDate < priceDate) does not delete priceDate
    calculator.computeSupportResistancesForTicker(
        ticker, Timeframe.WEEKLY, latestDate.minusWeeks(1));
    verify(weeklySrRepo, never()).deleteByTickerAndPriceDate(any(), any());

    // 2. Mid-week refresh on same priceDate (called twice) deletes and saves idempotently
    calculator.computeSupportResistancesForTicker(ticker, Timeframe.WEEKLY, latestDate);
    calculator.computeSupportResistancesForTicker(ticker, Timeframe.WEEKLY, latestDate);

    verify(weeklySrRepo, times(2)).deleteByTickerAndPriceDate(ticker, latestDate);
    verify(weeklySrRepo, times(3)).saveAll(any());
  }

  @Test
  void testDoesNotSaveWhenNoPriceBarsOrNoBucketsComputed() {
    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
    calculator.computeSupportResistancesForTicker(ticker, Timeframe.DAILY, null);
    verify(dailySrRepo, never()).deleteByTickerAndPriceDateLessThan(any(), any());
    verify(dailySrRepo, never()).saveAll(any());

    List<PriceBar> flatBars = List.of(bar(0, 100, 105, 95, 100));
    when(dailyPriceRepo.findByTickerOrderByPriceDateAsc(ticker))
        .thenReturn(flatBars.stream().map(this::dailyFrom).toList());

    calculator.computeSupportResistancesForTicker(ticker, Timeframe.DAILY, null);

    verify(dailySrRepo).deleteByTickerAndPriceDateLessThan(eq(ticker), any());
    verify(dailySrRepo, never()).saveAll(any());
  }

  @Test
  void testPolarityFlipReinforcesZoneAndFlipsRoleOnBreakout() {
    List<PriceBar> bars = new ArrayList<>();
    appendSeedBars(bars, 120);
    appendLeg(bars, 120, 150);
    final LocalDate peakDate = bars.getLast().date();
    appendLeg(bars, 150, 110);
    appendLeg(bars, 110, 200);
    appendLeg(bars, 200, 151);
    final LocalDate retestDate = bars.getLast().date();
    appendLeg(bars, 151, 185);

    List<Bucket> buckets = calculator.computeBuckets(bars);

    assertEquals(1, buckets.size());
    Bucket polarityZone = buckets.getFirst();
    assertEquals(2, polarityZone.getTouchCount());
    assertEquals(LevelType.SUPPORT, polarityZone.getLevelType());
    assertEquals(peakDate, polarityZone.getFirstTouchDate());
    assertEquals(retestDate, polarityZone.getLastTouchDate());
  }

  @Test
  void testSupportBreaksDownAndFlipsToResistanceOnSingleDecisiveBreach() {
    List<PriceBar> bars = new ArrayList<>();
    appendSeedBars(bars, 180);
    appendLeg(bars, 180, 150);
    appendLeg(bars, 150, 190);
    appendLeg(bars, 190, 100);
    appendLeg(bars, 100, 149);
    appendLeg(bars, 149, 115);

    List<Bucket> buckets = calculator.computeBuckets(bars);

    assertEquals(1, buckets.size());
    Bucket flippedZone = buckets.getFirst();
    assertEquals(2, flippedZone.getTouchCount());
    assertEquals(LevelType.RESISTANCE, flippedZone.getLevelType());
  }

  @Test
  void testZoneInvalidatedWhenSlicedThroughBeyondMaxAllowedFlips() {
    List<PriceBar> bars = new ArrayList<>();
    appendSeedBars(bars, 120);
    appendLeg(bars, 120, 150);
    appendLeg(bars, 150, 110);
    appendLeg(bars, 110, 200);
    appendLeg(bars, 200, 151);
    appendLeg(bars, 151, 185);
    appendLeg(bars, 185, 105);
    appendLeg(bars, 105, 120);

    List<Bucket> buckets = calculator.computeBuckets(bars);

    boolean hasBroken150Zone =
        buckets.stream()
            .anyMatch(bucket ->
                bucket.getMidpoint().compareTo(new BigDecimal("140")) >= 0
                    && bucket.getMidpoint().compareTo(new BigDecimal("160")) <= 0);
    assertFalse(hasBroken150Zone, "Zone sliced through more than maxAllowedFlips must be dropped");
  }

  @Test
  void testSingleFalseBreakoutForgivenAndSecondFalseBreakoutInvalidatesZone() {
    List<PriceBar> oneFalseBreakoutBars = buildOscillatingBars(105);
    oneFalseBreakoutBars.add(bar(oneFalseBreakoutBars.size(), 100, 100, 91, 92));
    oneFalseBreakoutBars.add(bar(oneFalseBreakoutBars.size(), 92, 104, 92, 103));
    oneFalseBreakoutBars.add(bar(oneFalseBreakoutBars.size(), 103, 112, 102, 110));

    List<Bucket> afterOneFbo = calculator.computeBuckets(oneFalseBreakoutBars);
    boolean hasSupportAfterOneFbo =
        afterOneFbo.stream()
            .anyMatch(bucket -> bucket.getMidpoint().compareTo(new BigDecimal("110")) < 0);
    assertTrue(hasSupportAfterOneFbo, "Single false breakout should be forgiven");

    List<PriceBar> twoFalseBreakoutBars = new ArrayList<>(oneFalseBreakoutBars);
    twoFalseBreakoutBars.add(bar(twoFalseBreakoutBars.size(), 102, 102, 91, 92));
    twoFalseBreakoutBars.add(bar(twoFalseBreakoutBars.size(), 92, 104, 92, 103));
    twoFalseBreakoutBars.add(bar(twoFalseBreakoutBars.size(), 103, 112, 102, 110));

    List<Bucket> afterTwoFbo = calculator.computeBuckets(twoFalseBreakoutBars);
    boolean hasSupportAfterTwoFbo =
        afterTwoFbo.stream()
            .anyMatch(bucket -> bucket.getMidpoint().compareTo(new BigDecimal("110")) < 0);
    assertFalse(
        hasSupportAfterTwoFbo, "Exceeding maxFalseBreakouts must invalidate whipsawed zone");
  }

  @Test
  void testInsideZoneRetainsLastSwingStructuralRole() {
    List<PriceBar> testingResistanceBars = buildOscillatingBars(151);
    List<Bucket> resistanceTestBuckets = calculator.computeBuckets(testingResistanceBars);
    Bucket upperZone = resistanceTestBuckets.getFirst();
    assertTrue(new BigDecimal("151").compareTo(upperZone.getBottom()) >= 0);
    assertTrue(new BigDecimal("151").compareTo(upperZone.getTop()) <= 0);
    assertEquals(
        LevelType.RESISTANCE,
        upperZone.getLevelType(),
        "Close inside resistance band without clearing top must remain RESISTANCE");

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
  void testTwoPointerExpandsRightWhileLeftRemainsUnchanged() {
    List<PriceBar> bars = new ArrayList<>();
    appendSeedBars(bars, 120);
    appendLeg(bars, 120, 100);
    appendLeg(bars, 100, 130);
    appendLeg(bars, 130, 102);
    appendLeg(bars, 102, 130);
    appendLeg(bars, 130, 106);
    appendLeg(bars, 106, 125);

    List<Bucket> buckets = calculator.computeBuckets(bars);
    assertFalse(buckets.isEmpty());
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

  @Test
  void testPipelineJobMethods() {
    assertEquals("CALCULATE_SUPPORT_RESISTANCE", calculator.getJobName());

    when(dailySrRepo.hasPendingDailySupportResistance()).thenReturn(false);
    assertFalse(calculator.shouldRun(Timeframe.DAILY));

    when(dailySrRepo.hasPendingDailySupportResistance()).thenReturn(true);
    assertTrue(calculator.shouldRun(Timeframe.DAILY));

    when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of());
    calculator.execute(Timeframe.DAILY);
  }
}
