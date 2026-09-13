package com.alphaflow.engine.calculators;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.dtos.ChartPatternMatch;
import com.alphaflow.engine.calculators.dtos.ExtremaPoint;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.persistence.entities.DailyChartPattern;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyChartPattern;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.enums.ChartPatternStatus;
import com.alphaflow.persistence.enums.ChartPatternType;
import com.alphaflow.persistence.repositories.DailyChartPatternRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyChartPatternRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChartPatternCalculatorTest {

  private TickerRepository tickerRepository;
  private DailyPriceRepository dailyPriceRepository;
  private WeeklyPriceRepository weeklyPriceRepository;
  private DailyChartPatternRepository dailyChartPatternRepository;
  private WeeklyChartPatternRepository weeklyChartPatternRepository;
  private ChartPatternCalculator calculator;
  private Ticker ticker;

  @BeforeEach
  void setUp() {
    tickerRepository = mock(TickerRepository.class);
    dailyPriceRepository = mock(DailyPriceRepository.class);
    weeklyPriceRepository = mock(WeeklyPriceRepository.class);
    dailyChartPatternRepository = mock(DailyChartPatternRepository.class);
    weeklyChartPatternRepository = mock(WeeklyChartPatternRepository.class);

    calculator =
        new ChartPatternCalculator(
            tickerRepository,
            dailyPriceRepository,
            weeklyPriceRepository,
            dailyChartPatternRepository,
            weeklyChartPatternRepository);

    ticker = Ticker.builder().tickerId(1L).tickerSymbol("AAPL").isActive(true).build();
  }

  private PriceBar bar(int day, double open, double high, double low, double close) {
    return new PriceBar(
        LocalDate.of(2026, 1, 1).plusDays(day),
        BigDecimal.valueOf(open).setScale(4, RoundingMode.HALF_UP),
        BigDecimal.valueOf(high).setScale(4, RoundingMode.HALF_UP),
        BigDecimal.valueOf(low).setScale(4, RoundingMode.HALF_UP),
        BigDecimal.valueOf(close).setScale(4, RoundingMode.HALF_UP),
        BigDecimal.valueOf(1000000));
  }

  private DailyPrice dailyPrice(int day, double open, double high, double low, double close) {
    return DailyPrice.builder()
        .priceDate(LocalDate.of(2026, 1, 1).plusDays(day))
        .priceOpen(BigDecimal.valueOf(open).setScale(4, RoundingMode.HALF_UP))
        .priceHigh(BigDecimal.valueOf(high).setScale(4, RoundingMode.HALF_UP))
        .priceLow(BigDecimal.valueOf(low).setScale(4, RoundingMode.HALF_UP))
        .priceClose(BigDecimal.valueOf(close).setScale(4, RoundingMode.HALF_UP))
        .volume(BigDecimal.valueOf(1000000))
        .ticker(ticker)
        .build();
  }

  private WeeklyPrice weeklyPrice(int week, double open, double high, double low, double close) {
    return WeeklyPrice.builder()
        .priceDate(LocalDate.of(2026, 1, 1).plusWeeks(week))
        .priceOpen(BigDecimal.valueOf(open).setScale(4, RoundingMode.HALF_UP))
        .priceHigh(BigDecimal.valueOf(high).setScale(4, RoundingMode.HALF_UP))
        .priceLow(BigDecimal.valueOf(low).setScale(4, RoundingMode.HALF_UP))
        .priceClose(BigDecimal.valueOf(close).setScale(4, RoundingMode.HALF_UP))
        .volume(BigDecimal.valueOf(5000000))
        .ticker(ticker)
        .build();
  }

  @Test
  void testComputeChartPatternsSuccessAndExceptionHandling() {
    Ticker ticker2 = Ticker.builder().tickerId(2L).tickerSymbol("FAIL").isActive(true).build();
    when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(ticker, ticker2));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
    when(weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker2))
        .thenThrow(new RuntimeException("DB error"));

    calculator.computeChartPatterns();

    verify(dailyPriceRepository, times(1)).findByTickerOrderByPriceDateAsc(ticker);
    verify(dailyPriceRepository, times(1)).findByTickerOrderByPriceDateAsc(ticker2);
  }

  @Test
  void testComputeChartPatternsForTickerWithSufficientDataAndSavesBothTimeframes() {
    List<DailyPrice> dailyPrices = new ArrayList<>();
    List<WeeklyPrice> weeklyPrices = new ArrayList<>();

    // Create 30 flat bars
    for (int i = 0; i < 30; i++) {
      dailyPrices.add(dailyPrice(i, 100, 101, 99, 100));
      weeklyPrices.add(weeklyPrice(i, 100, 101, 99, 100));
    }

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(dailyPrices);
    when(weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(weeklyPrices);

    calculator.computeChartPatternsForTicker(ticker);

    verify(dailyPriceRepository, times(1)).findByTickerOrderByPriceDateAsc(ticker);
    verify(weeklyPriceRepository, times(1)).findByTickerOrderByPriceDateAsc(ticker);
  }

  @Test
  void testComputeChartPatternsForTickerWithInsufficientData() {
    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
    when(weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());

    calculator.computeChartPatternsForTicker(ticker);

    verify(dailyChartPatternRepository, never()).save(any());
    verify(weeklyChartPatternRepository, never()).save(any());
  }

  @Test
  void testLoadBarsUnsupportedTimeframe() throws Exception {
    Method loadBarsMethod =
        ChartPatternCalculator.class.getDeclaredMethod("loadBars", Ticker.class, Timeframe.class);
    loadBarsMethod.setAccessible(true);

    InvocationTargetException ex =
        assertThrows(
            InvocationTargetException.class, () -> loadBarsMethod.invoke(calculator, ticker, null));
    assertInstanceOf(IllegalArgumentException.class, ex.getCause());
  }

  @Test
  void testSavePatternsUnsupportedTimeframe() throws Exception {
    Method savePatternsMethod =
        ChartPatternCalculator.class.getDeclaredMethod(
            "savePatterns", Ticker.class, Timeframe.class, List.class);
    savePatternsMethod.setAccessible(true);

    InvocationTargetException ex =
        assertThrows(
            InvocationTargetException.class,
            () -> savePatternsMethod.invoke(calculator, ticker, null, List.of()));
    assertInstanceOf(IllegalArgumentException.class, ex.getCause());
  }

  @Test
  void testSavePatternsDailyAndWeeklyUpsert() throws Exception {
    Method savePatternsMethod =
        ChartPatternCalculator.class.getDeclaredMethod(
            "savePatterns", Ticker.class, Timeframe.class, List.class);
    savePatternsMethod.setAccessible(true);

    ChartPatternMatch match =
        ChartPatternMatch.builder()
            .patternType(ChartPatternType.DOUBLE_TOP)
            .sentiment(ChartPatternType.DOUBLE_TOP.getSentiment())
            .status(ChartPatternStatus.COMPLETED)
            .startDate(LocalDate.of(2026, 1, 1))
            .endDate(LocalDate.of(2026, 1, 10))
            .breakoutDate(LocalDate.of(2026, 1, 12))
            .necklinePrice(new BigDecimal("90.0000"))
            .necklineSlope(new BigDecimal("0.0000"))
            .targetPrice(new BigDecimal("80.0000"))
            .stopLossPrice(new BigDecimal("99.0000"))
            .invalidationPrice(new BigDecimal("100.0000"))
            .pivotPoints(List.of())
            .build();

    // 1. Daily new insert
    when(dailyChartPatternRepository.findByTickerAndPatternTypeAndStartDate(
            ticker, ChartPatternType.DOUBLE_TOP, LocalDate.of(2026, 1, 1)))
        .thenReturn(Optional.empty());
    savePatternsMethod.invoke(calculator, ticker, Timeframe.DAILY, List.of(match));
    verify(dailyChartPatternRepository, times(1)).save(any(DailyChartPattern.class));

    // 2. Daily update existing
    DailyChartPattern existingDaily =
        DailyChartPattern.builder()
            .id(1L)
            .ticker(ticker)
            .patternType(ChartPatternType.DOUBLE_TOP)
            .status(ChartPatternStatus.IN_PROGRESS)
            .startDate(LocalDate.of(2026, 1, 1))
            .build();
    when(dailyChartPatternRepository.findByTickerAndPatternTypeAndStartDate(
            ticker, ChartPatternType.DOUBLE_TOP, LocalDate.of(2026, 1, 1)))
        .thenReturn(Optional.of(existingDaily));
    savePatternsMethod.invoke(calculator, ticker, Timeframe.DAILY, List.of(match));
    assertEquals(ChartPatternStatus.COMPLETED, existingDaily.getStatus());
    assertEquals(new BigDecimal("99.0000"), existingDaily.getStopLossPrice());

    // 3. Weekly new insert
    when(weeklyChartPatternRepository.findByTickerAndPatternTypeAndStartDate(
            ticker, ChartPatternType.DOUBLE_TOP, LocalDate.of(2026, 1, 1)))
        .thenReturn(Optional.empty());
    savePatternsMethod.invoke(calculator, ticker, Timeframe.WEEKLY, List.of(match));
    verify(weeklyChartPatternRepository, times(1)).save(any(WeeklyChartPattern.class));

    // 4. Weekly update existing
    WeeklyChartPattern existingWeekly =
        WeeklyChartPattern.builder()
            .id(2L)
            .ticker(ticker)
            .patternType(ChartPatternType.DOUBLE_TOP)
            .status(ChartPatternStatus.IN_PROGRESS)
            .startDate(LocalDate.of(2026, 1, 1))
            .build();
    when(weeklyChartPatternRepository.findByTickerAndPatternTypeAndStartDate(
            ticker, ChartPatternType.DOUBLE_TOP, LocalDate.of(2026, 1, 1)))
        .thenReturn(Optional.of(existingWeekly));
    savePatternsMethod.invoke(calculator, ticker, Timeframe.WEEKLY, List.of(match));
    assertEquals(ChartPatternStatus.COMPLETED, existingWeekly.getStatus());
    assertEquals(new BigDecimal("99.0000"), existingWeekly.getStopLossPrice());
  }

  @Test
  void testDetectAllPatternsUnderMinBarsOrPivots() {
    assertTrue(calculator.detectAllPatterns(List.of()).isEmpty());

    List<PriceBar> flatBars = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      flatBars.add(bar(i, 100, 100, 100, 100));
    }
    assertTrue(calculator.detectAllPatterns(flatBars).isEmpty());
  }

  @Test
  void testFilterAlternatingExtremaConsecutiveConsolidation() {
    assertTrue(calculator.filterAlternatingExtrema(List.of()).isEmpty());

    List<ExtremaPoint> raw =
        List.of(
            new ExtremaPoint(0, LocalDate.now(), new BigDecimal("100"), true),
            new ExtremaPoint(1, LocalDate.now(), new BigDecimal("105"), true), // Higher high
            new ExtremaPoint(2, LocalDate.now(), new BigDecimal("90"), false),
            new ExtremaPoint(3, LocalDate.now(), new BigDecimal("85"), false), // Lower low
            new ExtremaPoint(4, LocalDate.now(), new BigDecimal("110"), true));

    List<ExtremaPoint> filtered = calculator.filterAlternatingExtrema(raw);
    assertEquals(3, filtered.size());
    assertEquals(new BigDecimal("105"), filtered.get(0).price());
    assertEquals(new BigDecimal("85"), filtered.get(1).price());
    assertEquals(new BigDecimal("110"), filtered.get(2).price());
  }

  @Test
  void testDoubleTopDetectionAndStatuses() {
    // Construct Double Top: H1 at 100, Trough at 90, H2 at 100
    // Window = 2, so peaks need 2 bars on each side
    List<PriceBar> bars = new ArrayList<>();
    // Left padding
    bars.add(bar(0, 80, 81, 79, 80));
    bars.add(bar(1, 85, 86, 84, 85));
    // Peak 1 at index 2
    bars.add(bar(2, 98, 100, 97, 99)); // High 100
    // Trough at index 5
    bars.add(bar(3, 95, 96, 94, 95));
    bars.add(bar(4, 91, 92, 90, 91));
    bars.add(bar(5, 89, 91, 88, 90)); // Low 88
    bars.add(bar(6, 92, 93, 91, 92));
    bars.add(bar(7, 95, 96, 94, 95));
    // Peak 2 at index 8
    bars.add(bar(8, 98, 100, 97, 99)); // High 100
    // Right padding for peak 2
    bars.add(bar(9, 95, 96, 94, 95));
    bars.add(bar(10, 92, 93, 91, 92));
    // Additional bars to test breakout, target, invalidation
    bars.add(bar(11, 90, 91, 89, 90));
    bars.add(bar(12, 88, 89, 87, 88));
    bars.add(bar(13, 86, 87, 85, 86));
    bars.add(bar(14, 84, 85, 83, 84));
    bars.add(bar(15, 80, 81, 75, 76)); // Hits target <= 88 - (100 - 88) = 76!

    List<ChartPatternMatch> matches = calculator.detectAllPatterns(bars);
    boolean found = matches.stream().anyMatch(m -> m.patternType() == ChartPatternType.DOUBLE_TOP);
    assertTrue(found);
    ChartPatternMatch dtMatch =
        matches.stream()
            .filter(m -> m.patternType() == ChartPatternType.DOUBLE_TOP)
            .findFirst()
            .orElseThrow();
    assertNotNull(dtMatch.stopLossPrice());
    assertNotNull(dtMatch.targetPrice());
  }

  @Test
  void testDoubleBottomDetection() {
    List<PriceBar> bars = new ArrayList<>();
    bars.add(bar(0, 120, 121, 119, 120));
    bars.add(bar(1, 115, 116, 114, 115));
    // Trough 1 at index 2 (Low 100)
    bars.add(bar(2, 102, 103, 100, 101));
    bars.add(bar(3, 105, 106, 104, 105));
    bars.add(bar(4, 109, 110, 108, 109));
    // Peak at index 5 (High 112)
    bars.add(bar(5, 111, 112, 110, 111));
    bars.add(bar(6, 108, 109, 107, 108));
    bars.add(bar(7, 104, 105, 103, 104));
    // Trough 2 at index 8 (Low 100)
    bars.add(bar(8, 102, 103, 100, 101));
    bars.add(bar(9, 105, 106, 104, 105));
    bars.add(bar(10, 108, 109, 107, 108));
    bars.add(bar(11, 110, 111, 109, 110));
    bars.add(bar(12, 113, 114, 112, 113)); // Breakout > 112
    bars.add(bar(13, 115, 116, 114, 115));
    bars.add(bar(14, 120, 125, 119, 124)); // Hits target >= 112 + 12 = 124

    List<ChartPatternMatch> matches = calculator.detectAllPatterns(bars);
    boolean found =
        matches.stream().anyMatch(m -> m.patternType() == ChartPatternType.DOUBLE_BOTTOM);
    assertTrue(found);
  }

  @Test
  void testTripleTopAndTripleBottom() {
    // Triple Top: H1(100), L1(90), H2(100), L2(90), H3(100)
    List<PriceBar> bars = new ArrayList<>();
    bars.add(bar(0, 80, 81, 79, 80));
    bars.add(bar(1, 85, 86, 84, 85));
    bars.add(bar(2, 98, 100, 97, 99)); // H1 (index 2)
    bars.add(bar(3, 95, 96, 94, 95));
    bars.add(bar(4, 91, 92, 90, 91)); // L1 (index 4)
    bars.add(bar(5, 94, 95, 93, 94));
    bars.add(bar(6, 98, 100, 97, 99)); // H2 (index 6)
    bars.add(bar(7, 95, 96, 94, 95));
    bars.add(bar(8, 91, 92, 90, 91)); // L2 (index 8)
    bars.add(bar(9, 94, 95, 93, 94));
    bars.add(bar(10, 98, 100, 97, 99)); // H3 (index 10)
    bars.add(bar(11, 95, 96, 94, 95));
    bars.add(bar(12, 93, 94, 92, 93));
    bars.add(bar(13, 89, 90, 88, 89)); // Breakout < 90
    bars.add(bar(14, 88, 89, 87, 88));
    bars.add(bar(15, 87, 88, 86, 87));

    List<ChartPatternMatch> matches = calculator.detectAllPatterns(bars);
    boolean found = matches.stream().anyMatch(m -> m.patternType() == ChartPatternType.TRIPLE_TOP);
    assertTrue(found);

    // Triple Bottom: L1(100), H1(110), L2(100), H2(110), L3(100)
    List<PriceBar> bottomBars = new ArrayList<>();
    bottomBars.add(bar(0, 120, 121, 119, 120));
    bottomBars.add(bar(1, 115, 116, 114, 115));
    bottomBars.add(bar(2, 102, 103, 100, 101)); // L1
    bottomBars.add(bar(3, 105, 106, 104, 105));
    bottomBars.add(bar(4, 109, 110, 108, 109)); // H1
    bottomBars.add(bar(5, 106, 107, 105, 106));
    bottomBars.add(bar(6, 102, 103, 100, 101)); // L2
    bottomBars.add(bar(7, 105, 106, 104, 105));
    bottomBars.add(bar(8, 109, 110, 108, 109)); // H2
    bottomBars.add(bar(9, 106, 107, 105, 106));
    bottomBars.add(bar(10, 102, 103, 100, 101)); // L3
    bottomBars.add(bar(11, 105, 106, 104, 105));
    bottomBars.add(bar(12, 107, 108, 106, 107));
    bottomBars.add(bar(13, 111, 112, 110, 111)); // Breakout > 110
    bottomBars.add(bar(14, 112, 113, 111, 112));
    bottomBars.add(bar(15, 113, 114, 112, 113));

    List<ChartPatternMatch> bottomMatches = calculator.detectAllPatterns(bottomBars);
    boolean foundBottom =
        bottomMatches.stream().anyMatch(m -> m.patternType() == ChartPatternType.TRIPLE_BOTTOM);
    assertTrue(foundBottom);
  }

  @Test
  void testHeadAndShouldersAndInverse() {
    // H&S: LS(100), T1(90), Head(110), T2(90), RS(100)
    List<PriceBar> bars = new ArrayList<>();
    bars.add(bar(0, 80, 81, 79, 80));
    bars.add(bar(1, 85, 86, 84, 85));
    bars.add(bar(2, 98, 100, 97, 99)); // LS (index 2)
    bars.add(bar(3, 95, 96, 94, 95));
    bars.add(bar(4, 91, 92, 90, 91)); // T1 (index 4)
    bars.add(bar(5, 98, 100, 97, 99));
    bars.add(bar(6, 108, 110, 107, 109)); // Head (index 6)
    bars.add(bar(7, 98, 100, 97, 99));
    bars.add(bar(8, 91, 92, 90, 91)); // T2 (index 8)
    bars.add(bar(9, 95, 96, 94, 95));
    bars.add(bar(10, 98, 100, 97, 99)); // RS (index 10)
    bars.add(bar(11, 95, 96, 94, 95));
    bars.add(bar(12, 92, 93, 91, 92));
    bars.add(bar(13, 88, 89, 87, 88)); // Breakout < 90
    bars.add(bar(14, 87, 88, 86, 87));
    bars.add(bar(15, 86, 87, 85, 86));

    List<ChartPatternMatch> matches = calculator.detectAllPatterns(bars);
    boolean found =
        matches.stream().anyMatch(m -> m.patternType() == ChartPatternType.HEAD_AND_SHOULDERS);
    assertTrue(found);

    // Inverse H&S: LS(100), P1(110), Head(90), P2(110), RS(100)
    List<PriceBar> invBars = new ArrayList<>();
    invBars.add(bar(0, 120, 121, 119, 120));
    invBars.add(bar(1, 115, 116, 114, 115));
    invBars.add(bar(2, 102, 103, 100, 101)); // LS (index 2)
    invBars.add(bar(3, 105, 106, 104, 105));
    invBars.add(bar(4, 109, 110, 108, 109)); // P1 (index 4)
    invBars.add(bar(5, 102, 103, 101, 102));
    invBars.add(bar(6, 92, 93, 90, 91)); // Head (index 6)
    invBars.add(bar(7, 102, 103, 101, 102));
    invBars.add(bar(8, 109, 110, 108, 109)); // P2 (index 8)
    invBars.add(bar(9, 105, 106, 104, 105));
    invBars.add(bar(10, 102, 103, 100, 101)); // RS (index 10)
    invBars.add(bar(11, 105, 106, 104, 105));
    invBars.add(bar(12, 108, 109, 107, 108));
    invBars.add(bar(13, 111, 112, 110, 111)); // Breakout > 110
    invBars.add(bar(14, 112, 113, 111, 112));
    invBars.add(bar(15, 113, 114, 112, 113));

    List<ChartPatternMatch> invMatches = calculator.detectAllPatterns(invBars);
    boolean foundInv =
        invMatches.stream()
            .anyMatch(m -> m.patternType() == ChartPatternType.INVERSE_HEAD_AND_SHOULDERS);
    assertTrue(foundInv);
  }

  @Test
  void testTriangles() {
    // Ascending: H1(100), L1(80), H2(100), L2(85)
    List<PriceBar> ascBars = new ArrayList<>();
    ascBars.add(bar(0, 70, 71, 69, 70));
    ascBars.add(bar(1, 80, 81, 79, 80));
    ascBars.add(bar(2, 98, 100, 97, 99)); // H1
    ascBars.add(bar(3, 90, 91, 89, 90));
    ascBars.add(bar(4, 82, 83, 80, 81)); // L1
    ascBars.add(bar(5, 90, 91, 89, 90));
    ascBars.add(bar(6, 98, 100, 97, 99)); // H2
    ascBars.add(bar(7, 92, 93, 91, 92));
    ascBars.add(bar(8, 87, 88, 85, 86)); // L2 (higher low)
    ascBars.add(bar(9, 90, 91, 89, 90));
    ascBars.add(bar(10, 95, 96, 94, 95));
    ascBars.add(bar(11, 98, 99, 97, 98));
    ascBars.add(bar(12, 101, 102, 100, 101)); // Breakout > 100
    ascBars.add(bar(13, 102, 103, 101, 102));
    ascBars.add(bar(14, 103, 104, 102, 103));
    ascBars.add(bar(15, 104, 105, 103, 104));

    List<ChartPatternMatch> ascMatches = calculator.detectAllPatterns(ascBars);
    assertTrue(
        ascMatches.stream().anyMatch(m -> m.patternType() == ChartPatternType.ASCENDING_TRIANGLE));

    // Descending: L1(80), H1(100), L2(80), H2(95)
    List<PriceBar> descBars = new ArrayList<>();
    descBars.add(bar(0, 110, 111, 109, 110));
    descBars.add(bar(1, 95, 96, 94, 95));
    descBars.add(bar(2, 82, 83, 80, 81)); // L1
    descBars.add(bar(3, 90, 91, 89, 90));
    descBars.add(bar(4, 98, 100, 97, 99)); // H1
    descBars.add(bar(5, 90, 91, 89, 90));
    descBars.add(bar(6, 82, 83, 80, 81)); // L2
    descBars.add(bar(7, 88, 89, 87, 88));
    descBars.add(bar(8, 94, 95, 93, 94)); // H2 (lower high)
    descBars.add(bar(9, 90, 91, 89, 90));
    descBars.add(bar(10, 85, 86, 84, 85));
    descBars.add(bar(11, 82, 83, 81, 82));
    descBars.add(bar(12, 78, 79, 77, 78)); // Breakout < 80
    descBars.add(bar(13, 77, 78, 76, 77));
    descBars.add(bar(14, 76, 77, 75, 76));
    descBars.add(bar(15, 75, 76, 74, 75));

    List<ChartPatternMatch> descMatches = calculator.detectAllPatterns(descBars);
    assertTrue(
        descMatches.stream()
            .anyMatch(m -> m.patternType() == ChartPatternType.DESCENDING_TRIANGLE));

    // Symmetrical: H1(100), L1(70), H2(90), L2(80)
    List<PriceBar> symBars = new ArrayList<>();
    symBars.add(bar(0, 60, 61, 59, 60));
    symBars.add(bar(1, 80, 81, 79, 80));
    symBars.add(bar(2, 98, 100, 97, 99)); // H1
    symBars.add(bar(3, 85, 86, 84, 85));
    symBars.add(bar(4, 72, 73, 70, 71)); // L1
    symBars.add(bar(5, 80, 81, 79, 80));
    symBars.add(bar(6, 88, 90, 87, 89)); // H2 (lower high)
    symBars.add(bar(7, 85, 86, 84, 85));
    symBars.add(bar(8, 82, 83, 80, 81)); // L2 (higher low)
    symBars.add(bar(9, 85, 86, 84, 85));
    symBars.add(bar(10, 88, 89, 87, 88));
    symBars.add(bar(11, 91, 92, 90, 91)); // Breakout > 90
    symBars.add(bar(12, 92, 93, 91, 92));
    symBars.add(bar(13, 93, 94, 92, 93));
    symBars.add(bar(14, 94, 95, 93, 94));
    symBars.add(bar(15, 95, 96, 94, 95));

    List<ChartPatternMatch> symMatches = calculator.detectAllPatterns(symBars);
    assertTrue(
        symMatches.stream()
            .anyMatch(m -> m.patternType() == ChartPatternType.SYMMETRICAL_TRIANGLE));
  }

  @Test
  void testWedgesAndCupAndHandle() {
    // Rising Wedge: L1(70), H1(90), L2(80), H2(95) -> Highs rise 5, lows rise 10 (lows rise faster)
    List<PriceBar> rwBars = new ArrayList<>();
    rwBars.add(bar(0, 79, 81, 78, 80));
    rwBars.add(bar(1, 77, 78, 74, 75));
    rwBars.add(bar(2, 74, 75, 70, 71)); // L1
    rwBars.add(bar(3, 75, 86, 74, 85));
    rwBars.add(bar(4, 85, 90, 84, 88)); // H1
    rwBars.add(bar(5, 87, 86, 84, 85));
    rwBars.add(bar(6, 84, 85, 80, 82)); // L2
    rwBars.add(bar(7, 82, 91, 83, 90));
    rwBars.add(bar(8, 90, 95, 88, 94)); // H2
    rwBars.add(bar(9, 93, 90, 86, 88));
    rwBars.add(bar(10, 88, 85, 82, 84));
    rwBars.add(bar(11, 84, 84, 78, 78)); // Breakout < 80
    rwBars.add(bar(12, 78, 78, 74, 75));
    rwBars.add(bar(13, 75, 75, 70, 70));
    rwBars.add(bar(14, 70, 71, 69, 70));
    rwBars.add(bar(15, 70, 71, 69, 70));

    List<ChartPatternMatch> rwMatches = calculator.detectAllPatterns(rwBars);
    assertTrue(rwMatches.stream().anyMatch(m -> m.patternType() == ChartPatternType.RISING_WEDGE));

    // Falling Wedge: H1(100), L1(80), H2(90), L2(75) -> Highs fall 10, lows fall 5 (highs fall
    // faster)
    List<PriceBar> fwBars = new ArrayList<>();
    fwBars.add(bar(0, 92, 94, 90, 93));
    fwBars.add(bar(1, 94, 96, 92, 95));
    fwBars.add(bar(2, 96, 100, 95, 99)); // H1
    fwBars.add(bar(3, 94, 94, 84, 86));
    fwBars.add(bar(4, 85, 86, 80, 82)); // L1
    fwBars.add(bar(5, 83, 86, 83, 85));
    fwBars.add(bar(6, 86, 90, 84, 88)); // H2
    fwBars.add(bar(7, 87, 85, 80, 82));
    fwBars.add(bar(8, 81, 82, 75, 77)); // L2
    fwBars.add(bar(9, 78, 84, 78, 82));
    fwBars.add(bar(10, 82, 88, 82, 86));
    fwBars.add(bar(11, 87, 92, 86, 91)); // Breakout > 90
    fwBars.add(bar(12, 91, 95, 90, 94));
    fwBars.add(bar(13, 94, 98, 93, 97));
    fwBars.add(bar(14, 97, 100, 96, 99));
    fwBars.add(bar(15, 99, 102, 98, 101));

    List<ChartPatternMatch> fwMatches = calculator.detectAllPatterns(fwBars);
    assertTrue(fwMatches.stream().anyMatch(m -> m.patternType() == ChartPatternType.FALLING_WEDGE));

    // Cup and Handle: Left Rim 100, Cup Bottom 80, Right Rim 100, Handle 94
    List<PriceBar> chBars = new ArrayList<>();
    chBars.add(bar(0, 92, 94, 90, 93));
    chBars.add(bar(1, 94, 96, 92, 95));
    chBars.add(bar(2, 96, 100, 95, 99)); // Left Rim (index 2, H=100)
    chBars.add(bar(3, 94, 94, 84, 86));
    chBars.add(bar(4, 85, 86, 80, 82)); // Cup Bottom (index 4, L=80)
    chBars.add(bar(5, 86, 94, 84, 92));
    chBars.add(bar(6, 98, 100, 97, 99)); // Right Rim (index 6, H=100, L=97)
    chBars.add(bar(7, 97, 98, 96, 97));
    chBars.add(bar(8, 95, 96, 94, 95)); // Handle Trough (index 8, L=94)
    chBars.add(bar(9, 96, 98, 96, 97));
    chBars.add(bar(10, 98, 99, 97, 98));
    chBars.add(bar(11, 99, 102, 98, 101)); // Breakout > 100
    chBars.add(bar(12, 102, 108, 101, 106));
    chBars.add(bar(13, 106, 112, 105, 110));
    chBars.add(bar(14, 110, 118, 109, 116));
    chBars.add(bar(15, 116, 122, 115, 121));

    List<ChartPatternMatch> chMatches = calculator.detectAllPatterns(chBars);
    assertTrue(
        chMatches.stream().anyMatch(m -> m.patternType() == ChartPatternType.CUP_AND_HANDLE));
  }

  @Test
  void testEvaluateStatusAndBreakoutBranches() throws Exception {
    Method evaluateStatusMethod =
        ChartPatternCalculator.class.getDeclaredMethod(
            "evaluateStatus",
            List.class,
            int.class,
            BigDecimal.class,
            BigDecimal.class,
            BigDecimal.class,
            boolean.class);
    evaluateStatusMethod.setAccessible(true);

    Method findBreakoutDateMethod =
        ChartPatternCalculator.class.getDeclaredMethod(
            "findBreakoutDate", List.class, int.class, BigDecimal.class, boolean.class);
    findBreakoutDateMethod.setAccessible(true);

    List<PriceBar> testBars =
        List.of(bar(0, 100, 101, 99, 100), bar(1, 100, 101, 99, 100), bar(2, 100, 101, 99, 100));

    // 1. lastPivotIndex >= priceBars.size() - 1 => IN_PROGRESS
    ChartPatternStatus statusInProgress =
        (ChartPatternStatus)
            evaluateStatusMethod.invoke(
                calculator,
                testBars,
                2,
                new BigDecimal("110"),
                new BigDecimal("120"),
                new BigDecimal("90"),
                true);
    assertEquals(ChartPatternStatus.IN_PROGRESS, statusInProgress);

    // 2. Bullish invalidation before breakout
    List<PriceBar> invalidBeforeBreakoutBars =
        List.of(bar(0, 100, 101, 99, 100), bar(1, 88, 89, 87, 88)); // Below invalidation 90
    ChartPatternStatus statusInvalidBefore =
        (ChartPatternStatus)
            evaluateStatusMethod.invoke(
                calculator,
                invalidBeforeBreakoutBars,
                0,
                new BigDecimal("110"),
                new BigDecimal("120"),
                new BigDecimal("90"),
                true);
    assertEquals(ChartPatternStatus.INVALIDATED, statusInvalidBefore);

    // 3. Bullish invalidation after breakout
    List<PriceBar> invalidAfterBreakoutBars =
        List.of(
            bar(0, 100, 101, 99, 100),
            bar(1, 112, 113, 111, 112), // Breakout > 110
            bar(2, 85, 86, 84, 85)); // Invalidation < 90
    ChartPatternStatus statusInvalidAfter =
        (ChartPatternStatus)
            evaluateStatusMethod.invoke(
                calculator,
                invalidAfterBreakoutBars,
                0,
                new BigDecimal("110"),
                new BigDecimal("120"),
                new BigDecimal("90"),
                true);
    assertEquals(ChartPatternStatus.INVALIDATED, statusInvalidAfter);

    // 4. Bullish completed (broken out, but neither target nor invalidation reached)
    List<PriceBar> completedBars =
        List.of(
            bar(0, 100, 101, 99, 100),
            bar(1, 112, 113, 111, 112), // Breakout > 110
            bar(2, 113, 114, 112, 113));
    ChartPatternStatus statusCompleted =
        (ChartPatternStatus)
            evaluateStatusMethod.invoke(
                calculator,
                completedBars,
                0,
                new BigDecimal("110"),
                new BigDecimal("120"),
                new BigDecimal("90"),
                true);
    assertEquals(ChartPatternStatus.COMPLETED, statusCompleted);

    // 5. Bearish invalidation before breakout
    List<PriceBar> bearishInvalidBeforeBars =
        List.of(bar(0, 100, 101, 99, 100), bar(1, 112, 113, 111, 112)); // Above invalidation 110
    ChartPatternStatus statusBearishInvalidBefore =
        (ChartPatternStatus)
            evaluateStatusMethod.invoke(
                calculator,
                bearishInvalidBeforeBars,
                0,
                new BigDecimal("90"),
                new BigDecimal("80"),
                new BigDecimal("110"),
                false);
    assertEquals(ChartPatternStatus.INVALIDATED, statusBearishInvalidBefore);

    // 6. Bearish invalidation after breakout
    List<PriceBar> bearishInvalidAfterBars =
        List.of(
            bar(0, 100, 101, 99, 100),
            bar(1, 88, 89, 87, 88), // Breakout < 90
            bar(2, 115, 116, 114, 115)); // Above invalidation 110
    ChartPatternStatus statusBearishInvalidAfter =
        (ChartPatternStatus)
            evaluateStatusMethod.invoke(
                calculator,
                bearishInvalidAfterBars,
                0,
                new BigDecimal("90"),
                new BigDecimal("80"),
                new BigDecimal("110"),
                false);
    assertEquals(ChartPatternStatus.INVALIDATED, statusBearishInvalidAfter);

    // 7. Bearish target reached
    List<PriceBar> bearishTargetBars =
        List.of(
            bar(0, 100, 101, 99, 100),
            bar(1, 88, 89, 87, 88), // Breakout < 90
            bar(2, 79, 80, 78, 79)); // Hits target <= 80
    ChartPatternStatus statusBearishTarget =
        (ChartPatternStatus)
            evaluateStatusMethod.invoke(
                calculator,
                bearishTargetBars,
                0,
                new BigDecimal("90"),
                new BigDecimal("80"),
                new BigDecimal("110"),
                false);
    assertEquals(ChartPatternStatus.TARGET_REACHED, statusBearishTarget);

    // 8. Bearish completed (not hit target, not hit invalidation)
    List<PriceBar> bearishCompletedBars =
        List.of(
            bar(0, 100, 101, 99, 100),
            bar(1, 88, 89, 87, 88), // Breakout < 90
            bar(2, 85, 86, 84, 85));
    ChartPatternStatus statusBearishCompleted =
        (ChartPatternStatus)
            evaluateStatusMethod.invoke(
                calculator,
                bearishCompletedBars,
                0,
                new BigDecimal("90"),
                new BigDecimal("80"),
                new BigDecimal("110"),
                false);
    assertEquals(ChartPatternStatus.COMPLETED, statusBearishCompleted);

    // 9. findBreakoutDate returns null when no breakout occurs
    List<PriceBar> noBreakoutBars = List.of(bar(0, 100, 101, 99, 100), bar(1, 100, 101, 99, 100));
    LocalDate breakoutDateNull =
        (LocalDate)
            findBreakoutDateMethod.invoke(
                calculator, noBreakoutBars, 0, new BigDecimal("110"), true);
    assertNull(breakoutDateNull);
  }
}
