package com.alphaflow.engine.calculators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.indicators.EmaIndicator;
import com.alphaflow.engine.indicators.MacdIndicator;
import com.alphaflow.engine.indicators.RsiIndicator;
import com.alphaflow.engine.indicators.SmaIndicator;
import com.alphaflow.engine.indicators.StochasticIndicator;
import com.alphaflow.engine.indicators.utils.IndicatorRegistry;
import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Indicator;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyIndicator;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import com.alphaflow.persistence.repositories.DailyIndicatorRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.IndicatorDefinitionRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyIndicatorRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IndicatorCalculatorTest {

  private static final LocalDate EPOCH = LocalDate.of(2021, 1, 4); // a Monday
  private static final LocalDate HISTORICAL_MONDAY = LocalDate.of(2025, 1, 6);
  private static final int BAR_COUNT = 12;

  private DailyPriceRepository dailyRepo;
  private WeeklyPriceRepository weeklyRepo;
  private DailyIndicatorRepository dailyIndicatorRepo;
  private WeeklyIndicatorRepository weeklyIndicatorRepo;
  private TickerRepository tickerRepository;
  private IndicatorDefinitionRepository indicatorDefinitionRepository;
  private IndicatorCalculator calculator;
  private Ticker ticker;
  private IndicatorDefinition emaDefinition;
  private IndicatorDefinition smaVolumeDefinition;

  private static IndicatorDefinition emaDef() {
    return IndicatorDefinition.builder()
        .indicatorDefinitionId(1L)
        .indicatorType(IndicatorType.EMA)
        .source(PriceSource.CLOSE)
        .params(Map.of("period", 3))
        .build();
  }

  private static IndicatorDefinition smaVolumeDef() {
    return IndicatorDefinition.builder()
        .indicatorDefinitionId(2L)
        .indicatorType(IndicatorType.SMA)
        .source(PriceSource.VOLUME)
        .params(Map.of("period", 2))
        .build();
  }

  private static LocalDate date(int index) {
    return EPOCH.plusDays(index);
  }

  private static List<DailyPrice> dailyBars(Ticker targetTicker) {
    List<DailyPrice> bars = new ArrayList<>(BAR_COUNT);
    for (int i = 0; i < BAR_COUNT; i++) {
      BigDecimal close = BigDecimal.valueOf(10 + i).setScale(4);
      bars.add(
          DailyPrice.builder()
              .ticker(targetTicker)
              .priceDate(date(i))
              .priceOpen(close)
              .priceHigh(close.add(BigDecimal.ONE))
              .priceLow(close.subtract(BigDecimal.ONE))
              .priceClose(close)
              .volume(BigDecimal.valueOf(100L + i))
              .build());
    }
    return bars;
  }

  private static WeeklyPrice weeklyBar(Ticker targetTicker, LocalDate priceDate, int closeVal) {
    BigDecimal close = BigDecimal.valueOf(closeVal);
    return WeeklyPrice.builder()
        .ticker(targetTicker)
        .priceDate(priceDate)
        .priceOpen(close)
        .priceHigh(close)
        .priceLow(close)
        .priceClose(close)
        .volume(BigDecimal.valueOf(100L))
        .build();
  }

  @BeforeEach
  void setUp() {
    dailyRepo = mock(DailyPriceRepository.class);
    weeklyRepo = mock(WeeklyPriceRepository.class);
    dailyIndicatorRepo = mock(DailyIndicatorRepository.class);
    weeklyIndicatorRepo = mock(WeeklyIndicatorRepository.class);
    tickerRepository = mock(TickerRepository.class);
    indicatorDefinitionRepository = mock(IndicatorDefinitionRepository.class);

    IndicatorRegistry registry =
        new IndicatorRegistry(
            List.of(
                new EmaIndicator(),
                new SmaIndicator(),
                new RsiIndicator(),
                new MacdIndicator(),
                new StochasticIndicator()));

    calculator =
        new IndicatorCalculator(
            registry,
            dailyRepo,
            weeklyRepo,
            dailyIndicatorRepo,
            weeklyIndicatorRepo,
            tickerRepository,
            indicatorDefinitionRepository);

    ticker =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("TEST")
            .tickerName("Test")
            .country(Country.US)
            .isActive(true)
            .build();
    emaDefinition = emaDef();
    smaVolumeDefinition = smaVolumeDef();

    when(dailyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(dailyBars(ticker));
    when(weeklyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
  }

  @Test
  void testComputeIndicatorsAllUpToDateAndEmptyDefinitionsShortCircuit() {
    when(dailyIndicatorRepo.hasPendingDailyIndicators()).thenReturn(false);
    when(weeklyIndicatorRepo.hasPendingWeeklyIndicators()).thenReturn(false);

    org.junit.jupiter.api.Assertions.assertFalse(calculator.shouldRun(Timeframe.DAILY));
    org.junit.jupiter.api.Assertions.assertFalse(calculator.shouldRun(Timeframe.WEEKLY));

    verify(dailyIndicatorRepo).hasPendingDailyIndicators();
    verify(weeklyIndicatorRepo).hasPendingWeeklyIndicators();
  }

  @Test
  void testComputeIndicatorsOnlyComputesNewlyAddedOrStaleDefinitions() {
    when(dailyIndicatorRepo.findTickersWithPendingDailyIndicators()).thenReturn(List.of(ticker));
    when(indicatorDefinitionRepository.findAll())
        .thenReturn(List.of(emaDefinition, smaVolumeDefinition));
    when(dailyIndicatorRepo.findLatestDatesGroupedByTickerAndDefinition())
        .thenReturn(List.<Object[]>of(new Object[] {ticker, emaDefinition, date(11)}));

    calculator.execute(Timeframe.DAILY);

    Ticker upToDateTicker =
        Ticker.builder()
            .tickerId(2L)
            .tickerSymbol("MSFT")
            .country(Country.US)
            .isActive(true)
            .build();
    verify(dailyIndicatorRepo).findTickersWithPendingDailyIndicators();
    verify(tickerRepository, never()).findByIsActiveTrue();
    verify(dailyRepo, times(1)).findByTickerOrderByPriceDateAsc(ticker);
    verify(dailyRepo, never()).findByTickerOrderByPriceDateAsc(upToDateTicker);

    List<? extends Indicator> emaValues = capturedValues(IndicatorType.EMA);
    List<? extends Indicator> smaValues = capturedValues(IndicatorType.SMA);
    assertEquals(0, emaValues.size());
    assertEquals(11, smaValues.size());
  }

  @Test
  void testComputeIndicatorsForTimeframeSuccessErrorAndSelfProxy() throws Exception {
    Ticker failingTicker =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("AAPL")
            .country(Country.US)
            .isActive(true)
            .build();
    Ticker healthyTicker =
        Ticker.builder()
            .tickerId(2L)
            .tickerSymbol("MSFT")
            .country(Country.US)
            .isActive(true)
            .build();

    when(dailyIndicatorRepo.findTickersWithPendingDailyIndicators())
        .thenReturn(List.of(failingTicker, healthyTicker));
    when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(failingTicker, healthyTicker));
    when(indicatorDefinitionRepository.findAll()).thenReturn(List.of(emaDefinition));
    when(dailyIndicatorRepo.findLatestDatesGroupedByTickerAndDefinition()).thenReturn(List.of());
    when(weeklyIndicatorRepo.findLatestDatesGroupedByTickerAndDefinition()).thenReturn(List.of());

    IndicatorCalculator calculatorSpy = spy(calculator);
    doThrow(new RuntimeException("Computation error"))
        .when(calculatorSpy)
        .computeIndicatorForTicker(eq(failingTicker), eq(Timeframe.DAILY), any(Map.class));
    doNothing()
        .when(calculatorSpy)
        .computeIndicatorForTicker(eq(healthyTicker), eq(Timeframe.DAILY), any(Map.class));

    calculatorSpy.execute(Timeframe.DAILY);

    verify(calculatorSpy)
        .computeIndicatorForTicker(eq(failingTicker), eq(Timeframe.DAILY), any(Map.class));
    verify(calculatorSpy)
        .computeIndicatorForTicker(eq(healthyTicker), eq(Timeframe.DAILY), any(Map.class));

    IndicatorCalculator selfMock = mock(IndicatorCalculator.class);
    Field selfField = IndicatorCalculator.class.getDeclaredField("indicatorCalculator");
    selfField.setAccessible(true);
    selfField.set(calculator, selfMock);

    calculator.execute(Timeframe.WEEKLY);

    verify(selfMock)
        .computeIndicatorForTicker(eq(failingTicker), eq(Timeframe.WEEKLY), any(Map.class));
    verify(selfMock)
        .computeIndicatorForTicker(eq(healthyTicker), eq(Timeframe.WEEKLY), any(Map.class));
  }

  @Test
  void testComputeIndicatorsUnsupportedTimeframeThrows() {
    when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(ticker));
    when(indicatorDefinitionRepository.findAll()).thenReturn(List.of(emaDefinition));
    assertThrows(IllegalArgumentException.class, () -> calculator.execute(null));
  }

  @Test
  void processTickerBackfillsWholeSeriesWhenLatestCalculatedIsNull() {
    Map<IndicatorDefinition, LocalDate> staleStates = new LinkedHashMap<>();
    staleStates.put(emaDefinition, null);
    staleStates.put(smaVolumeDefinition, null);

    calculator.computeIndicatorForTicker(ticker, Timeframe.DAILY, staleStates);

    List<? extends Indicator> emaValues = capturedValues(IndicatorType.EMA);
    assertEquals(10, emaValues.size());

    List<? extends Indicator> smaValues = capturedValues(IndicatorType.SMA);
    assertEquals(11, smaValues.size());
  }

  @Test
  void processTickerDailyIncrementalOnlySavesDatesAfterLatestCalculated() {
    Map<IndicatorDefinition, LocalDate> staleStates = Map.of(emaDefinition, date(8));

    calculator.computeIndicatorForTicker(ticker, Timeframe.DAILY, staleStates);

    List<? extends Indicator> emaValues = capturedValues(IndicatorType.EMA);
    // Dates 9, 10, 11 are strictly after date(8)
    assertEquals(3, emaValues.size());
    assertEquals(date(9), emaValues.getFirst().getPriceDate());
    assertEquals(date(11), emaValues.getLast().getPriceDate());
  }

  @Test
  void processTimeframeWithEmptyBarsReturnsEarly() {
    when(dailyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());

    calculator.computeIndicatorForTicker(
        ticker,
        Timeframe.DAILY,
        Collections.singletonMap(emaDefinition, null));

    verify(dailyIndicatorRepo, never()).saveAll(any());
    verify(weeklyIndicatorRepo, never()).saveAll(any());
  }

  @Test
  void weeklyTimeframeBackfillAndMidWeekRefreshIdempotency() {
    LocalDate week1 = HISTORICAL_MONDAY;
    LocalDate week2 = HISTORICAL_MONDAY.plusWeeks(1);
    LocalDate week3 = HISTORICAL_MONDAY.plusWeeks(2);

    when(weeklyRepo.findByTickerOrderByPriceDateAsc(ticker))
        .thenReturn(
            List.of(
                weeklyBar(ticker, week1, 10),
                weeklyBar(ticker, week2, 12),
                weeklyBar(ticker, week3, 15)));

    IndicatorDefinition weeklyDef =
        IndicatorDefinition.builder()
            .indicatorDefinitionId(10L)
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 2))
            .build();

    // 1. Initial backfill (latestCalculatedDate == null) -> no delete, inserts week2 & week3
    calculator.computeIndicatorForTicker(
        ticker,
        Timeframe.WEEKLY,
        Collections.singletonMap(weeklyDef, null));

    verify(weeklyIndicatorRepo, never())
        .deleteByTickerAndIndicatorDefinitionAndPriceDateGreaterThanEqual(any(), any(), any());
    assertEquals(2, capturedValues(IndicatorType.EMA).size());

    // 2. Mid-week refresh (latestCalculatedDate == week3) -> atomic delete >= week3, inserts week3
    calculator.computeIndicatorForTicker(
        ticker,
        Timeframe.WEEKLY,
        Map.of(weeklyDef, week3));

    verify(weeklyIndicatorRepo, times(1))
        .deleteByTickerAndIndicatorDefinitionAndPriceDateGreaterThanEqual(ticker, weeklyDef, week3);
    // Total captured across both calls: 2 (backfill) + 1 (week3 refresh) = 3
    assertEquals(3, capturedValues(IndicatorType.EMA).size());
  }

  @Test
  void processTickerWithInsufficientBarsDoesNotSave() {
    IndicatorDefinition longEmaDef =
        IndicatorDefinition.builder()
            .indicatorDefinitionId(99L)
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 10))
            .build();

    Ticker testTicker =
        Ticker.builder()
            .tickerId(99L)
            .tickerSymbol("SHORT")
            .country(Country.US)
            .isActive(true)
            .build();
    BigDecimal close = BigDecimal.TEN;
    List<DailyPrice> bars =
        List.of(
            DailyPrice.builder()
                .ticker(testTicker)
                .priceDate(date(0))
                .priceOpen(close)
                .priceHigh(close)
                .priceLow(close)
                .priceClose(close)
                .volume(close)
                .build(),
            DailyPrice.builder()
                .ticker(testTicker)
                .priceDate(date(1))
                .priceOpen(close)
                .priceHigh(close)
                .priceLow(close)
                .priceClose(close)
                .volume(close)
                .build());

    when(dailyRepo.findByTickerOrderByPriceDateAsc(testTicker)).thenReturn(bars);

    calculator.computeIndicatorForTicker(
        testTicker,
        Timeframe.DAILY,
        Collections.singletonMap(longEmaDef, null));

    verify(dailyIndicatorRepo, never()).saveAll(any());
    verify(weeklyIndicatorRepo, never()).saveAll(any());
  }

  @Test
  void processWeeklyTickerWithInsufficientBarsDoesNotSave() {
    when(weeklyRepo.findByTickerOrderByPriceDateAsc(ticker))
        .thenReturn(List.of(weeklyBar(ticker, HISTORICAL_MONDAY, 10)));

    IndicatorDefinition weeklyDef =
        IndicatorDefinition.builder()
            .indicatorDefinitionId(11L)
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 5))
            .build();

    calculator.computeIndicatorForTicker(
        ticker,
        Timeframe.WEEKLY,
        Collections.singletonMap(weeklyDef, null));

    verify(dailyIndicatorRepo, never()).saveAll(any());
    verify(weeklyIndicatorRepo, never()).saveAll(any());
  }

  @Test
  void testComputeIndicatorsWithNoStaleDefinitionsReturnsEarly() {
    calculator.computeIndicatorForTicker(ticker, Timeframe.DAILY, Map.of());

    verify(dailyRepo, never()).findByTickerOrderByPriceDateAsc(any());
    verify(weeklyRepo, never()).findByTickerOrderByPriceDateAsc(any());
  }

  @Test
  void testUnsupportedTimeframeInLoadBarsThrowsException() throws Exception {
    Method loadBarsMethod =
        IndicatorCalculator.class.getDeclaredMethod("loadBars", Ticker.class, Timeframe.class);
    loadBarsMethod.setAccessible(true);

    InvocationTargetException exception =
        assertThrows(
            InvocationTargetException.class,
            () -> loadBarsMethod.invoke(calculator, ticker, (Timeframe) null));
    assertInstanceOf(IllegalArgumentException.class, exception.getCause());
  }

  @Test
  void testUnsupportedTimeframeInComputeAndSaveIndicatorsThrowsException() throws Exception {
    Method saveMethod =
        IndicatorCalculator.class.getDeclaredMethod(
            "computeAndSaveIndicators", Ticker.class, Timeframe.class, List.class, Map.class);
    saveMethod.setAccessible(true);

    InvocationTargetException exception =
        assertThrows(
            InvocationTargetException.class,
            () -> saveMethod.invoke(calculator, ticker, (Timeframe) null, List.of(), Map.of()));
    assertInstanceOf(IllegalArgumentException.class, exception.getCause());
  }

  @SuppressWarnings("unchecked")
  private List<? extends Indicator> capturedValues(IndicatorType type) {
    ArgumentCaptor<List<DailyIndicator>> dailyCaptor = ArgumentCaptor.forClass(List.class);
    verify(dailyIndicatorRepo, atLeast(0)).saveAll(dailyCaptor.capture());

    ArgumentCaptor<List<WeeklyIndicator>> weeklyCaptor = ArgumentCaptor.forClass(List.class);
    verify(weeklyIndicatorRepo, atLeast(0)).saveAll(weeklyCaptor.capture());

    List<Indicator> allValues = new ArrayList<>();
    dailyCaptor.getAllValues().forEach(allValues::addAll);
    weeklyCaptor.getAllValues().forEach(allValues::addAll);

    return allValues.stream().filter(indicator -> indicator.getIndicatorType() == type).toList();
  }

  @Test
  void testPipelineJobMethods() {
    assertEquals("CALCULATE_INDICATOR", calculator.getJobName());

    when(dailyIndicatorRepo.hasPendingDailyIndicators()).thenReturn(false);
    org.junit.jupiter.api.Assertions.assertFalse(calculator.shouldRun(Timeframe.DAILY));

    when(dailyIndicatorRepo.hasPendingDailyIndicators()).thenReturn(true);
    org.junit.jupiter.api.Assertions.assertTrue(calculator.shouldRun(Timeframe.DAILY));

    when(dailyIndicatorRepo.findTickersWithPendingDailyIndicators()).thenReturn(List.of());
    when(indicatorDefinitionRepository.findAll()).thenReturn(List.of());
    calculator.execute(Timeframe.DAILY);
  }
}
