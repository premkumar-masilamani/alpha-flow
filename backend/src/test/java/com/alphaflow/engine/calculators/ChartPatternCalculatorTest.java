package com.alphaflow.engine.calculators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.dtos.ChartPatternMatch;
import com.alphaflow.engine.configs.ChartPatternConfig;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.structure.DowAtrSwingDetector;
import com.alphaflow.engine.structure.DowTrendClassifier;
import com.alphaflow.engine.structure.dtos.SwingPoint;
import com.alphaflow.engine.structure.enums.SwingType;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.enums.ChartPatternStatus;
import com.alphaflow.persistence.enums.ChartPatternType;
import com.alphaflow.persistence.repositories.DailyChartPatternRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyChartPatternRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ChartPatternCalculatorTest {

  private DailyPriceRepository dailyPriceRepository;
  private WeeklyPriceRepository weeklyPriceRepository;
  private DailyChartPatternRepository dailyChartPatternRepository;
  private WeeklyChartPatternRepository weeklyChartPatternRepository;
  private TickerRepository tickerRepository;
  private DowAtrSwingDetector dowAtrSwingDetector;
  private DowTrendClassifier dowTrendClassifier;
  private ChartPatternCalculator calculator;
  private Ticker ticker;

  @BeforeEach
  void setUp() {
    dailyPriceRepository = mock(DailyPriceRepository.class);
    weeklyPriceRepository = mock(WeeklyPriceRepository.class);
    dailyChartPatternRepository = mock(DailyChartPatternRepository.class);
    weeklyChartPatternRepository = mock(WeeklyChartPatternRepository.class);
    tickerRepository = mock(TickerRepository.class);
    dowAtrSwingDetector = new DowAtrSwingDetector();
    dowTrendClassifier = new DowTrendClassifier();

    calculator =
        new ChartPatternCalculator(
            dailyPriceRepository,
            weeklyPriceRepository,
            dailyChartPatternRepository,
            weeklyChartPatternRepository,
            tickerRepository,
            dowAtrSwingDetector,
            dowTrendClassifier);

    ticker = Ticker.builder().tickerId(1L).tickerSymbol("AAPL").isActive(true).build();
  }

  private PriceBar bar(int dayOffset, double open, double high, double low, double close) {
    return new PriceBar(
        LocalDate.of(2026, 1, 1).plusDays(dayOffset),
        BigDecimal.valueOf(open),
        BigDecimal.valueOf(high),
        BigDecimal.valueOf(low),
        BigDecimal.valueOf(close),
        BigDecimal.valueOf(1000));
  }

  private SwingPoint swing(int barIndex, double price, SwingType type) {
    return new SwingPoint(
        barIndex,
        LocalDate.of(2026, 1, 1).plusDays(barIndex),
        BigDecimal.valueOf(price),
        type,
        BigDecimal.valueOf(2.0));
  }

  private List<PriceBar> flatBars(int count, double price) {
    List<PriceBar> bars = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      bars.add(bar(i, price, price + 1.0, price - 1.0, price));
    }
    return bars;
  }

  private List<SwingPoint> uptrendPrefixStartingHigh() {
    return List.of(
        swing(0, 80, SwingType.HIGH),
        swing(2, 70, SwingType.LOW),
        swing(4, 92, SwingType.HIGH),
        swing(6, 82, SwingType.LOW));
  }

  private List<SwingPoint> downtrendPrefixStartingLow() {
    return List.of(
        swing(0, 130, SwingType.LOW),
        swing(2, 140, SwingType.HIGH),
        swing(4, 118, SwingType.LOW),
        swing(6, 128, SwingType.HIGH));
  }

  private List<DailyPrice> createDailyPricesWithDoubleTop() {
    // 14 warm-up bars + HH/HL uptrend + Double Top swings clearly exceeding 2*ATR
    double[] closes = {
      80,
      80,
      80,
      80,
      80,
      80,
      80,
      80,
      80,
      80,
      80,
      80,
      80,
      80, // 0..13 seed
      82,
      85,
      88,
      92,
      96, // rally to 96
      90,
      84, // pull back to 84
      92,
      100,
      108, // HH to 108
      100,
      92, // HL to 92
      102,
      112,
      120, // Peak 1 at 120
      110,
      102, // Neckline at 102
      112,
      119, // Peak 2 at 119
      108,
      98, // Breakout below 102
      94,
      82 // Target reached
    };
    List<DailyPrice> prices = new ArrayList<>();
    for (int i = 0; i < closes.length; i++) {
      BigDecimal close = BigDecimal.valueOf(closes[i]);
      prices.add(
          DailyPrice.builder()
              .ticker(ticker)
              .priceDate(LocalDate.of(2026, 1, 1).plusDays(i))
              .priceOpen(close)
              .priceHigh(close.add(BigDecimal.ONE))
              .priceLow(close.subtract(BigDecimal.ONE))
              .priceClose(close)
              .volume(BigDecimal.valueOf(1000))
              .build());
    }
    return prices;
  }

  private List<WeeklyPrice> createWeeklyPricesWithDoubleTop() {
    List<DailyPrice> daily = createDailyPricesWithDoubleTop();
    List<WeeklyPrice> weekly = new ArrayList<>();
    for (int i = 0; i < daily.size(); i++) {
      DailyPrice dailyPrice = daily.get(i);
      weekly.add(
          WeeklyPrice.builder()
              .ticker(ticker)
              .priceDate(LocalDate.of(2025, 1, 6).plusWeeks(i))
              .priceOpen(dailyPrice.getPriceOpen())
              .priceHigh(dailyPrice.getPriceHigh())
              .priceLow(dailyPrice.getPriceLow())
              .priceClose(dailyPrice.getPriceClose())
              .volume(dailyPrice.getVolume())
              .build());
    }
    return weekly;
  }

  @Test
  void testGetJobNameAndShouldRun() {
    assertEquals(ChartPatternCalculator.JOB_NAME, calculator.getJobName());

    when(dailyChartPatternRepository.hasPendingDailyChartPatterns()).thenReturn(true);
    when(weeklyChartPatternRepository.hasPendingWeeklyChartPatterns()).thenReturn(false);

    assertTrue(calculator.shouldRun(Timeframe.DAILY));
    assertFalse(calculator.shouldRun(Timeframe.WEEKLY));
    assertThrows(IllegalArgumentException.class, () -> calculator.shouldRun(null));
  }

  @Test
  void testExecuteUnsupportedTimeframeThrows() {
    assertThrows(IllegalArgumentException.class, () -> calculator.execute(null));
  }

  @Test
  void testExecuteDailySavesPatternsAndPurgesOlder() {
    List<DailyPrice> prices = createDailyPricesWithDoubleTop();
    when(dailyChartPatternRepository.findTickersWithPendingDailyChartPatterns())
        .thenReturn(List.of(ticker));
    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);

    calculator.execute(Timeframe.DAILY);

    LocalDate latestDate = prices.getLast().getPriceDate();
    verify(dailyChartPatternRepository).findTickersWithPendingDailyChartPatterns();
    verify(tickerRepository, never()).findByIsActiveTrue();
    verify(dailyChartPatternRepository)
        .deleteByTickerAndPriceDateLessThan(
            ticker, latestDate.minusDays(ChartPatternConfig.DAILY_RETENTION_DAYS));
    verify(dailyChartPatternRepository).saveAll(anyList());
  }

  @Test
  void testExecuteDailySkipsWhenNoPendingTickers() {
    when(dailyChartPatternRepository.findTickersWithPendingDailyChartPatterns())
        .thenReturn(List.of());

    calculator.execute(Timeframe.DAILY);

    verify(dailyChartPatternRepository).findTickersWithPendingDailyChartPatterns();
    verify(tickerRepository, never()).findByIsActiveTrue();
    verify(dailyPriceRepository, never()).findByTickerOrderByPriceDateAsc(any());
    verify(dailyChartPatternRepository, never()).deleteByTickerAndPriceDateLessThan(any(), any());
    verify(dailyChartPatternRepository, never()).saveAll(anyList());
  }

  @Test
  void testExecuteWeeklyRefreshesInProgressWeekAndPurgesOlder() {
    List<WeeklyPrice> prices = createWeeklyPricesWithDoubleTop();
    LocalDate latestDate = prices.getLast().getPriceDate();
    when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(ticker));
    when(weeklyChartPatternRepository.findLatestPriceDatesForActiveTickers())
        .thenReturn(Map.of(ticker, latestDate));
    when(weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);

    calculator.execute(Timeframe.WEEKLY);

    verify(weeklyChartPatternRepository)
        .deleteByTickerAndPriceDateLessThan(
            ticker, latestDate.minusDays(ChartPatternConfig.WEEKLY_RETENTION_DAYS));
    verify(weeklyChartPatternRepository).deleteByTickerAndPriceDate(ticker, latestDate);
    verify(weeklyChartPatternRepository).saveAll(anyList());
  }

  @Test
  void testExecuteWeeklyNewWeekDoesNotDeleteCurrentDate() {
    List<WeeklyPrice> prices = createWeeklyPricesWithDoubleTop();
    LocalDate latestDate = prices.getLast().getPriceDate();
    when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(ticker));
    when(weeklyChartPatternRepository.findLatestPriceDatesForActiveTickers())
        .thenReturn(Map.of(ticker, latestDate.minusWeeks(1)));
    when(weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);

    calculator.execute(Timeframe.WEEKLY);

    verify(weeklyChartPatternRepository, never()).deleteByTickerAndPriceDate(any(), any());
    verify(weeklyChartPatternRepository).saveAll(anyList());
  }

  @Test
  void testExecuteWeeklyNoPriorDateAndNoPatternsDetected() {
    List<WeeklyPrice> flatWeekly = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      flatWeekly.add(
          WeeklyPrice.builder()
              .ticker(ticker)
              .priceDate(LocalDate.of(2026, 1, 5).plusWeeks(i))
              .priceOpen(BigDecimal.valueOf(100))
              .priceHigh(BigDecimal.valueOf(101))
              .priceLow(BigDecimal.valueOf(99))
              .priceClose(BigDecimal.valueOf(100))
              .volume(BigDecimal.valueOf(1000))
              .build());
    }
    when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(ticker));
    when(weeklyChartPatternRepository.findLatestPriceDatesForActiveTickers()).thenReturn(Map.of());
    when(weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(flatWeekly);

    calculator.execute(Timeframe.WEEKLY);

    verify(weeklyChartPatternRepository, never()).deleteByTickerAndPriceDate(any(), any());
    verify(weeklyChartPatternRepository, never()).saveAll(anyList());
  }

  @Test
  void testExecuteHandlesInsufficientBarsAndPerTickerException() {
    Ticker failingTicker = Ticker.builder().tickerId(2L).tickerSymbol("ERR").isActive(true).build();
    when(dailyChartPatternRepository.findTickersWithPendingDailyChartPatterns())
        .thenReturn(List.of(ticker, failingTicker));
    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(failingTicker))
        .thenThrow(new RuntimeException("DB error"));

    calculator.execute(Timeframe.DAILY);

    verify(dailyChartPatternRepository, never()).saveAll(anyList());
  }

  @Test
  void testComputeChartPatternsForTickerUnsupportedTimeframeThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> calculator.computeChartPatternsForTicker(ticker, null, null));
  }

  @Test
  void testDetectAllPatternsInsufficientBarsAndInsufficientSwings() {
    assertTrue(calculator.detectAllPatterns(flatBars(10, 100)).isEmpty());
    assertTrue(
        calculator
            .detectPatternsFromSwings(
                flatBars(20, 100),
                List.of(swing(1, 100, SwingType.HIGH), swing(3, 90, SwingType.LOW)))
            .isEmpty());
  }

  @Test
  void testDoubleTopRequiresUptrendAndEvaluatesStatuses() {
    List<PriceBar> bars = new ArrayList<>(flatBars(15, 100));
    bars.set(11, bar(11, 100, 101, 88, 89)); // breakout below neckline 90
    bars.set(12, bar(12, 89, 90, 75, 76)); // target 80 reached

    List<SwingPoint> swings = new ArrayList<>(uptrendPrefixStartingHigh());
    swings.add(swing(8, 100, SwingType.HIGH));
    swings.add(swing(9, 90, SwingType.LOW));
    swings.add(swing(10, 99, SwingType.HIGH));

    List<ChartPatternMatch> matches = calculator.detectPatternsFromSwings(bars, swings);
    ChartPatternMatch doubleTop =
        matches.stream()
            .filter(match -> match.patternType() == ChartPatternType.DOUBLE_TOP)
            .findFirst()
            .orElseThrow();

    assertEquals(ChartPatternStatus.TARGET_REACHED, doubleTop.status());
    assertEquals(bars.get(11).date(), doubleTop.breakoutDate());
    assertEquals(3, doubleTop.pivotPoints().size());

    // Without prior uptrend, Double Top is rejected
    List<SwingPoint> noTrendSwings =
        List.of(
            swing(8, 100, SwingType.HIGH),
            swing(9, 90, SwingType.LOW),
            swing(10, 99, SwingType.HIGH));
    assertTrue(
        calculator.detectPatternsFromSwings(bars, noTrendSwings).stream()
            .noneMatch(match -> match.patternType() == ChartPatternType.DOUBLE_TOP));
  }

  @Test
  void testDoubleBottomRequiresDowntrendAndEvaluatesStatuses() {
    List<PriceBar> bars = new ArrayList<>(flatBars(15, 100));
    bars.set(11, bar(11, 110, 115, 109, 114)); // breakout above neckline 110
    bars.set(12, bar(12, 114, 125, 113, 124)); // target 120 reached

    List<SwingPoint> swings = new ArrayList<>(downtrendPrefixStartingLow());
    swings.add(swing(8, 100, SwingType.LOW));
    swings.add(swing(9, 110, SwingType.HIGH));
    swings.add(swing(10, 101, SwingType.LOW));

    List<ChartPatternMatch> matches = calculator.detectPatternsFromSwings(bars, swings);
    ChartPatternMatch doubleBottom =
        matches.stream()
            .filter(match -> match.patternType() == ChartPatternType.DOUBLE_BOTTOM)
            .findFirst()
            .orElseThrow();

    assertEquals(ChartPatternStatus.TARGET_REACHED, doubleBottom.status());
    assertEquals(bars.get(11).date(), doubleBottom.breakoutDate());
    assertEquals(3, doubleBottom.pivotPoints().size());

    // Reject when spread exceeds tolerance or depth too shallow
    List<SwingPoint> rejectedSwings = new ArrayList<>(downtrendPrefixStartingLow());
    rejectedSwings.add(swing(8, 100, SwingType.LOW));
    rejectedSwings.add(swing(9, 100.5, SwingType.HIGH)); // shallow depth
    rejectedSwings.add(swing(10, 100, SwingType.LOW));
    assertTrue(
        calculator.detectPatternsFromSwings(bars, rejectedSwings).stream()
            .noneMatch(match -> match.patternType() == ChartPatternType.DOUBLE_BOTTOM));
  }

  @Test
  void testDoubleTopRejectedOnWideSpreadAndShallowDepth() {
    List<SwingPoint> wideSpread = new ArrayList<>(uptrendPrefixStartingHigh());
    wideSpread.add(swing(8, 120, SwingType.HIGH));
    wideSpread.add(swing(9, 100, SwingType.LOW));
    wideSpread.add(swing(10, 105, SwingType.HIGH));
    List<PriceBar> bars = flatBars(15, 100);
    assertTrue(
        calculator.detectPatternsFromSwings(bars, wideSpread).stream()
            .noneMatch(match -> match.patternType() == ChartPatternType.DOUBLE_TOP));

    List<SwingPoint> shallowDepth = new ArrayList<>(uptrendPrefixStartingHigh());
    shallowDepth.add(swing(8, 100, SwingType.HIGH));
    shallowDepth.add(swing(9, 99.5, SwingType.LOW));
    shallowDepth.add(swing(10, 100, SwingType.HIGH));
    assertTrue(
        calculator.detectPatternsFromSwings(bars, shallowDepth).stream()
            .noneMatch(match -> match.patternType() == ChartPatternType.DOUBLE_TOP));
  }

  @Test
  void testTripleTopAndTripleBottomWithDowTrendGating() {
    List<PriceBar> bars = flatBars(20, 100);

    List<SwingPoint> tripleTopSwings = new ArrayList<>(uptrendPrefixStartingHigh());
    tripleTopSwings.add(swing(8, 110, SwingType.HIGH));
    tripleTopSwings.add(swing(9, 95, SwingType.LOW));
    tripleTopSwings.add(swing(10, 111, SwingType.HIGH));
    tripleTopSwings.add(swing(11, 96, SwingType.LOW));
    tripleTopSwings.add(swing(12, 109, SwingType.HIGH));

    List<ChartPatternMatch> topMatches = calculator.detectPatternsFromSwings(bars, tripleTopSwings);
    assertTrue(
        topMatches.stream().anyMatch(match -> match.patternType() == ChartPatternType.TRIPLE_TOP));

    List<SwingPoint> tripleBottomSwings = new ArrayList<>(downtrendPrefixStartingLow());
    tripleBottomSwings.add(swing(8, 90, SwingType.LOW));
    tripleBottomSwings.add(swing(9, 105, SwingType.HIGH));
    tripleBottomSwings.add(swing(10, 91, SwingType.LOW));
    tripleBottomSwings.add(swing(11, 104, SwingType.HIGH));
    tripleBottomSwings.add(swing(12, 89.5, SwingType.LOW));

    List<ChartPatternMatch> bottomMatches =
        calculator.detectPatternsFromSwings(bars, tripleBottomSwings);
    assertTrue(
        bottomMatches.stream()
            .anyMatch(match -> match.patternType() == ChartPatternType.TRIPLE_BOTTOM));

    // Reject shallow depth on triple top and triple bottom
    List<SwingPoint> shallowTripleTop = new ArrayList<>(uptrendPrefixStartingHigh());
    shallowTripleTop.add(swing(8, 100, SwingType.HIGH));
    shallowTripleTop.add(swing(9, 99.5, SwingType.LOW));
    shallowTripleTop.add(swing(10, 100, SwingType.HIGH));
    shallowTripleTop.add(swing(11, 99.5, SwingType.LOW));
    shallowTripleTop.add(swing(12, 100, SwingType.HIGH));
    assertTrue(
        calculator.detectPatternsFromSwings(bars, shallowTripleTop).stream()
            .noneMatch(match -> match.patternType() == ChartPatternType.TRIPLE_TOP));

    List<SwingPoint> shallowTripleBottom = new ArrayList<>(downtrendPrefixStartingLow());
    shallowTripleBottom.add(swing(8, 100, SwingType.LOW));
    shallowTripleBottom.add(swing(9, 100.5, SwingType.HIGH));
    shallowTripleBottom.add(swing(10, 100, SwingType.LOW));
    shallowTripleBottom.add(swing(11, 100.5, SwingType.HIGH));
    shallowTripleBottom.add(swing(12, 100, SwingType.LOW));
    assertTrue(
        calculator.detectPatternsFromSwings(bars, shallowTripleBottom).stream()
            .noneMatch(match -> match.patternType() == ChartPatternType.TRIPLE_BOTTOM));
  }

  @Test
  void testHeadAndShouldersAndInverseHeadAndShoulders() {
    List<SwingPoint> hnsSwings = new ArrayList<>(uptrendPrefixStartingHigh());
    hnsSwings.add(swing(8, 110, SwingType.HIGH));
    hnsSwings.add(swing(9, 95, SwingType.LOW));
    hnsSwings.add(swing(10, 125, SwingType.HIGH));
    hnsSwings.add(swing(11, 96, SwingType.LOW));
    hnsSwings.add(swing(12, 112, SwingType.HIGH));
    List<PriceBar> bars = flatBars(20, 100);

    assertTrue(
        calculator.detectPatternsFromSwings(bars, hnsSwings).stream()
            .anyMatch(match -> match.patternType() == ChartPatternType.HEAD_AND_SHOULDERS));

    List<SwingPoint> invHnsSwings = new ArrayList<>(downtrendPrefixStartingLow());
    invHnsSwings.add(swing(8, 95, SwingType.LOW));
    invHnsSwings.add(swing(9, 110, SwingType.HIGH));
    invHnsSwings.add(swing(10, 80, SwingType.LOW));
    invHnsSwings.add(swing(11, 109, SwingType.HIGH));
    invHnsSwings.add(swing(12, 93, SwingType.LOW));

    assertTrue(
        calculator.detectPatternsFromSwings(bars, invHnsSwings).stream()
            .anyMatch(match -> match.patternType() == ChartPatternType.INVERSE_HEAD_AND_SHOULDERS));

    // Reject asymmetric shoulders and non-prominent head
    List<SwingPoint> badShoulders = new ArrayList<>(uptrendPrefixStartingHigh());
    badShoulders.add(swing(8, 100, SwingType.HIGH));
    badShoulders.add(swing(9, 90, SwingType.LOW));
    badShoulders.add(swing(10, 125, SwingType.HIGH));
    badShoulders.add(swing(11, 90, SwingType.LOW));
    badShoulders.add(swing(12, 120, SwingType.HIGH));
    assertTrue(
        calculator.detectPatternsFromSwings(bars, badShoulders).stream()
            .noneMatch(match -> match.patternType() == ChartPatternType.HEAD_AND_SHOULDERS));

    List<SwingPoint> badInverseShoulders = new ArrayList<>(downtrendPrefixStartingLow());
    badInverseShoulders.add(swing(8, 100, SwingType.LOW));
    badInverseShoulders.add(swing(9, 110, SwingType.HIGH));
    badInverseShoulders.add(swing(10, 80, SwingType.LOW));
    badInverseShoulders.add(swing(11, 110, SwingType.HIGH));
    badInverseShoulders.add(swing(12, 85, SwingType.LOW));
    assertTrue(
        calculator.detectPatternsFromSwings(bars, badInverseShoulders).stream()
            .noneMatch(
                match -> match.patternType() == ChartPatternType.INVERSE_HEAD_AND_SHOULDERS));
  }

  @Test
  void testTrianglesWedgesAndCupAndHandle() {
    List<PriceBar> bars = flatBars(20, 100);

    // Ascending Triangle + Symmetrical Triangle + Cup & Handle
    List<SwingPoint> ascAndCupSwings =
        List.of(
            swing(2, 120, SwingType.HIGH),
            swing(4, 100, SwingType.LOW),
            swing(6, 118, SwingType.HIGH),
            swing(8, 112, SwingType.LOW));
    List<ChartPatternMatch> ascMatches = calculator.detectPatternsFromSwings(bars, ascAndCupSwings);
    assertTrue(
        ascMatches.stream()
            .anyMatch(match -> match.patternType() == ChartPatternType.ASCENDING_TRIANGLE));
    assertTrue(
        ascMatches.stream()
            .anyMatch(match -> match.patternType() == ChartPatternType.SYMMETRICAL_TRIANGLE));
    assertTrue(
        ascMatches.stream()
            .anyMatch(match -> match.patternType() == ChartPatternType.CUP_AND_HANDLE));

    // Descending Triangle
    List<SwingPoint> descSwings =
        List.of(
            swing(2, 100, SwingType.LOW),
            swing(4, 120, SwingType.HIGH),
            swing(6, 101, SwingType.LOW),
            swing(8, 112, SwingType.HIGH));
    assertTrue(
        calculator.detectPatternsFromSwings(bars, descSwings).stream()
            .anyMatch(match -> match.patternType() == ChartPatternType.DESCENDING_TRIANGLE));

    // Rising Wedge
    List<SwingPoint> risingWedgeSwings =
        List.of(
            swing(2, 100, SwingType.LOW),
            swing(4, 115, SwingType.HIGH),
            swing(6, 110, SwingType.LOW),
            swing(8, 120, SwingType.HIGH));
    assertTrue(
        calculator.detectPatternsFromSwings(bars, risingWedgeSwings).stream()
            .anyMatch(match -> match.patternType() == ChartPatternType.RISING_WEDGE));

    // Non-converging rising channel (rejected as rising wedge)
    List<SwingPoint> risingChannel =
        List.of(
            swing(2, 100, SwingType.LOW),
            swing(4, 110, SwingType.HIGH),
            swing(6, 105, SwingType.LOW),
            swing(8, 125, SwingType.HIGH));
    assertTrue(
        calculator.detectPatternsFromSwings(bars, risingChannel).stream()
            .noneMatch(match -> match.patternType() == ChartPatternType.RISING_WEDGE));

    // Falling Wedge
    List<SwingPoint> fallingWedgeSwings =
        List.of(
            swing(2, 120, SwingType.HIGH),
            swing(4, 100, SwingType.LOW),
            swing(6, 108, SwingType.HIGH),
            swing(8, 95, SwingType.LOW));
    assertTrue(
        calculator.detectPatternsFromSwings(bars, fallingWedgeSwings).stream()
            .anyMatch(match -> match.patternType() == ChartPatternType.FALLING_WEDGE));

    // Non-converging falling channel (rejected as falling wedge)
    List<SwingPoint> fallingChannel =
        List.of(
            swing(2, 120, SwingType.HIGH),
            swing(4, 105, SwingType.LOW),
            swing(6, 115, SwingType.HIGH),
            swing(8, 90, SwingType.LOW));
    assertTrue(
        calculator.detectPatternsFromSwings(bars, fallingChannel).stream()
            .noneMatch(match -> match.patternType() == ChartPatternType.FALLING_WEDGE));
  }

  @Test
  void testCupAndHandleRejectionBranches() {
    List<PriceBar> bars = flatBars(15, 100);
    // Shallow cup (< 4%)
    List<SwingPoint> shallowCup =
        List.of(
            swing(1, 100, SwingType.HIGH),
            swing(2, 98, SwingType.LOW),
            swing(3, 100, SwingType.HIGH),
            swing(4, 99, SwingType.LOW));
    assertTrue(
        calculator.detectPatternsFromSwings(bars, shallowCup).stream()
            .noneMatch(match -> match.patternType() == ChartPatternType.CUP_AND_HANDLE));

    // Handle deeper than half of cup
    List<SwingPoint> deepHandle =
        List.of(
            swing(1, 120, SwingType.HIGH),
            swing(2, 100, SwingType.LOW),
            swing(3, 120, SwingType.HIGH),
            swing(4, 105, SwingType.LOW));
    assertTrue(
        calculator.detectPatternsFromSwings(bars, deepHandle).stream()
            .noneMatch(match -> match.patternType() == ChartPatternType.CUP_AND_HANDLE));
  }

  @Test
  void testStatusEvaluationAllBranchesBullishAndBearish() {
    // Bullish: IN_PROGRESS when last pivot is last bar
    List<PriceBar> lastBarList = flatBars(9, 105);
    List<SwingPoint> bullishSwings =
        List.of(
            swing(2, 120, SwingType.HIGH),
            swing(4, 100, SwingType.LOW),
            swing(6, 119, SwingType.HIGH),
            swing(8, 110, SwingType.LOW));
    ChartPatternMatch atLastBar =
        calculator.detectPatternsFromSwings(lastBarList, bullishSwings).stream()
            .filter(match -> match.patternType() == ChartPatternType.ASCENDING_TRIANGLE)
            .findFirst()
            .orElseThrow();
    assertEquals(ChartPatternStatus.IN_PROGRESS, atLastBar.status());
    assertNull(atLastBar.breakoutDate());

    // Bullish: IN_PROGRESS with subsequent bars inside range
    List<PriceBar> inRangeBars = new ArrayList<>(flatBars(11, 110));
    ChartPatternMatch inProgress =
        calculator.detectPatternsFromSwings(inRangeBars, bullishSwings).stream()
            .filter(match -> match.patternType() == ChartPatternType.ASCENDING_TRIANGLE)
            .findFirst()
            .orElseThrow();
    assertEquals(ChartPatternStatus.IN_PROGRESS, inProgress.status());
    assertNull(inProgress.breakoutDate());

    // Bullish: INVALIDATED before breakout, followed by a later trigger cross
    List<PriceBar> invalidatedPreBars = new ArrayList<>(flatBars(11, 110));
    invalidatedPreBars.set(9, bar(9, 100, 101, 95, 96));
    invalidatedPreBars.set(10, bar(10, 120, 126, 119, 125));
    ChartPatternMatch invPre =
        calculator.detectPatternsFromSwings(invalidatedPreBars, bullishSwings).stream()
            .filter(match -> match.patternType() == ChartPatternType.ASCENDING_TRIANGLE)
            .findFirst()
            .orElseThrow();
    assertEquals(ChartPatternStatus.INVALIDATED, invPre.status());
    assertNull(invPre.breakoutDate());

    // Bullish: COMPLETED (broken out, neither target nor invalidation hit)
    List<PriceBar> completedBars = new ArrayList<>(flatBars(11, 110));
    completedBars.set(9, bar(9, 120, 125, 119, 123));
    completedBars.set(10, bar(10, 123, 126, 121, 124));
    ChartPatternMatch completed =
        calculator.detectPatternsFromSwings(completedBars, bullishSwings).stream()
            .filter(match -> match.patternType() == ChartPatternType.ASCENDING_TRIANGLE)
            .findFirst()
            .orElseThrow();
    assertEquals(ChartPatternStatus.COMPLETED, completed.status());
    assertEquals(completedBars.get(9).date(), completed.breakoutDate());

    // Bullish: INVALIDATED after breakout
    List<PriceBar> invalidatedPostBars = new ArrayList<>(flatBars(11, 110));
    invalidatedPostBars.set(9, bar(9, 120, 125, 119, 123));
    invalidatedPostBars.set(10, bar(10, 123, 124, 95, 96));
    ChartPatternMatch invPost =
        calculator.detectPatternsFromSwings(invalidatedPostBars, bullishSwings).stream()
            .filter(match -> match.patternType() == ChartPatternType.ASCENDING_TRIANGLE)
            .findFirst()
            .orElseThrow();
    assertEquals(ChartPatternStatus.INVALIDATED, invPost.status());
    assertEquals(invalidatedPostBars.get(9).date(), invPost.breakoutDate());

    // Bearish: INVALIDATED before breakout (with later trigger cross), COMPLETED, & after breakout
    List<SwingPoint> bearishSwings =
        List.of(
            swing(2, 100, SwingType.LOW),
            swing(4, 120, SwingType.HIGH),
            swing(6, 101, SwingType.LOW),
            swing(8, 112, SwingType.HIGH));

    List<PriceBar> bearInvPre = new ArrayList<>(flatBars(11, 110));
    bearInvPre.set(9, bar(9, 118, 125, 117, 123));
    bearInvPre.set(10, bar(10, 100, 101, 94, 95));
    ChartPatternMatch bearishInvPre =
        calculator.detectPatternsFromSwings(bearInvPre, bearishSwings).stream()
            .filter(match -> match.patternType() == ChartPatternType.DESCENDING_TRIANGLE)
            .findFirst()
            .orElseThrow();
    assertEquals(ChartPatternStatus.INVALIDATED, bearishInvPre.status());
    assertNull(bearishInvPre.breakoutDate());

    List<PriceBar> bearCompleted = new ArrayList<>(flatBars(11, 110));
    bearCompleted.set(9, bar(9, 100, 101, 95, 96));
    bearCompleted.set(10, bar(10, 96, 98, 92, 94));
    ChartPatternMatch bearishCompleted =
        calculator.detectPatternsFromSwings(bearCompleted, bearishSwings).stream()
            .filter(match -> match.patternType() == ChartPatternType.DESCENDING_TRIANGLE)
            .findFirst()
            .orElseThrow();
    assertEquals(ChartPatternStatus.COMPLETED, bearishCompleted.status());
    assertEquals(bearCompleted.get(9).date(), bearishCompleted.breakoutDate());

    List<PriceBar> bearInvPost = new ArrayList<>(flatBars(11, 110));
    bearInvPost.set(9, bar(9, 100, 101, 95, 96));
    bearInvPost.set(10, bar(10, 96, 125, 95, 123));
    ChartPatternMatch bearishInvPost =
        calculator.detectPatternsFromSwings(bearInvPost, bearishSwings).stream()
            .filter(match -> match.patternType() == ChartPatternType.DESCENDING_TRIANGLE)
            .findFirst()
            .orElseThrow();
    assertEquals(ChartPatternStatus.INVALIDATED, bearishInvPost.status());
    assertEquals(bearInvPost.get(9).date(), bearishInvPost.breakoutDate());
  }

  @Test
  void testRemainingHeadAndShouldersWedgeAndSelfProxyBranches() {
    // H&S where head > leftShoulder but head <= rightShoulder
    List<SwingPoint> higherRightShoulder = new ArrayList<>(uptrendPrefixStartingHigh());
    higherRightShoulder.add(swing(8, 100, SwingType.HIGH));
    higherRightShoulder.add(swing(9, 90, SwingType.LOW));
    higherRightShoulder.add(swing(10, 115, SwingType.HIGH));
    higherRightShoulder.add(swing(11, 92, SwingType.LOW));
    higherRightShoulder.add(swing(12, 120, SwingType.HIGH));
    List<PriceBar> bars = flatBars(20, 100);
    assertTrue(
        calculator.detectPatternsFromSwings(bars, higherRightShoulder).stream()
            .noneMatch(match -> match.patternType() == ChartPatternType.HEAD_AND_SHOULDERS));

    // Inverse H&S where head < leftShoulder but head >= rightShoulder
    List<SwingPoint> lowerRightShoulder = new ArrayList<>(downtrendPrefixStartingLow());
    lowerRightShoulder.add(swing(8, 100, SwingType.LOW));
    lowerRightShoulder.add(swing(9, 110, SwingType.HIGH));
    lowerRightShoulder.add(swing(10, 85, SwingType.LOW));
    lowerRightShoulder.add(swing(11, 108, SwingType.HIGH));
    lowerRightShoulder.add(swing(12, 80, SwingType.LOW));
    assertTrue(
        calculator.detectPatternsFromSwings(bars, lowerRightShoulder).stream()
            .noneMatch(
                match -> match.patternType() == ChartPatternType.INVERSE_HEAD_AND_SHOULDERS));

    // Rising wedge rejected when H2 > H1 but L2 <= L1
    List<SwingPoint> expandingLow =
        List.of(
            swing(2, 100, SwingType.LOW),
            swing(4, 115, SwingType.HIGH),
            swing(6, 98, SwingType.LOW),
            swing(8, 120, SwingType.HIGH));
    assertTrue(
        calculator.detectPatternsFromSwings(bars, expandingLow).stream()
            .noneMatch(match -> match.patternType() == ChartPatternType.RISING_WEDGE));

    // Verify selfProxy invocation path
    ChartPatternCalculator mockProxy = mock(ChartPatternCalculator.class);
    ReflectionTestUtils.setField(calculator, "selfProxy", mockProxy);
    when(dailyChartPatternRepository.findTickersWithPendingDailyChartPatterns())
        .thenReturn(List.of(ticker));
    calculator.execute(Timeframe.DAILY);
    verify(mockProxy).computeChartPatternsForTicker(ticker, Timeframe.DAILY, null);
  }
}
