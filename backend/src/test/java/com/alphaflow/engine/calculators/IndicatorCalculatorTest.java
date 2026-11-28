package com.alphaflow.engine.calculators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.alphaflow.engine.indicators.BollingerBandsIndicator;
import com.alphaflow.engine.indicators.EmaIndicator;
import com.alphaflow.engine.indicators.MacdIndicator;
import com.alphaflow.engine.indicators.RsiIndicator;
import com.alphaflow.engine.indicators.SmaIndicator;
import com.alphaflow.engine.indicators.StochasticIndicator;
import com.alphaflow.engine.indicators.dtos.IndicatorComputationPoint;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IndicatorCalculatorTest {

  private static final LocalDate EPOCH = LocalDate.of(2021, 1, 4);
  private static final LocalDate HISTORICAL_MONDAY = LocalDate.of(2025, 1, 6);
  private static final int BAR_COUNT = 12;

  private DailyPriceRepository dailyRepo;
  private WeeklyPriceRepository weeklyRepo;
  private DailyIndicatorRepository dailyIndicatorRepo;
  private WeeklyIndicatorRepository weeklyIndicatorRepo;
  private TickerRepository tickerRepository;
  private IndicatorDefinitionRepository indicatorDefinitionRepository;
  private IndicatorCalculator calculator;
  private EmaIndicator emaIndicator;
  private SmaIndicator smaIndicator;
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

  private static List<DailyPrice> dailyBars(Ticker targetTicker, int count) {
    List<DailyPrice> bars = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
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

    emaIndicator = new EmaIndicator();
    smaIndicator = new SmaIndicator();
    IndicatorRegistry registry =
        new IndicatorRegistry(
            List.of(
                emaIndicator,
                smaIndicator,
                new RsiIndicator(),
                new MacdIndicator(),
                new StochasticIndicator(),
                new BollingerBandsIndicator()));

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

    when(dailyRepo.findByTickerOrderByPriceDateAsc(ticker))
        .thenReturn(dailyBars(ticker, BAR_COUNT));
    when(weeklyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
  }

  @Test
  void testPipelineJobShouldRunAndShortCircuit() {
    assertEquals("CALCULATE_INDICATOR", calculator.getJobName());

    when(dailyIndicatorRepo.hasPendingDailyIndicators()).thenReturn(false);
    when(weeklyIndicatorRepo.hasPendingWeeklyIndicators()).thenReturn(false);
    assertFalse(calculator.shouldRun(Timeframe.DAILY));
    assertFalse(calculator.shouldRun(Timeframe.WEEKLY));

    when(dailyIndicatorRepo.hasPendingDailyIndicators()).thenReturn(true);
    when(weeklyIndicatorRepo.hasPendingWeeklyIndicators()).thenReturn(true);
    assertTrue(calculator.shouldRun(Timeframe.DAILY));
    assertTrue(calculator.shouldRun(Timeframe.WEEKLY));

    assertThrows(IllegalArgumentException.class, () -> calculator.shouldRun(null));

    when(dailyIndicatorRepo.findTickersWithPendingDailyIndicators()).thenReturn(List.of());
    when(indicatorDefinitionRepository.findAll()).thenReturn(List.of());
    calculator.execute(Timeframe.DAILY);

    when(dailyIndicatorRepo.findTickersWithPendingDailyIndicators()).thenReturn(List.of(ticker));
    when(indicatorDefinitionRepository.findAll()).thenReturn(List.of());
    calculator.execute(Timeframe.DAILY);

    verify(dailyPriceRepoSafe(), never()).findMaxPriceDatePerActiveTicker();
  }

  private DailyPriceRepository dailyPriceRepoSafe() {
    return dailyRepo;
  }

  @Test
  void dailySameDayUpToDateTickersLoadZeroBarsAndWriteZeroRows() {
    List<DailyPrice> bars = dailyBars(ticker, BAR_COUNT);
    List<PriceBar> priceBars = bars.stream().map(PriceBar::from).toList();
    IndicatorComputationPoint emaLatest =
        emaIndicator
            .computeStateful(
                priceBars, IndicatorParams.of(emaDefinition.getParams()), PriceSource.CLOSE, null)
            .get(date(11));
    IndicatorComputationPoint smaLatest =
        smaIndicator
            .computeStateful(
                priceBars,
                IndicatorParams.of(smaVolumeDefinition.getParams()),
                PriceSource.VOLUME,
                null)
            .get(date(11));

    DailyIndicator emaRow =
        DailyIndicator.builder()
            .dailyIndicatorId(101L)
            .ticker(ticker)
            .indicatorDefinition(emaDefinition)
            .priceDate(date(11))
            .values(emaLatest.values())
            .state(emaLatest.state())
            .build();
    DailyIndicator smaRow =
        DailyIndicator.builder()
            .dailyIndicatorId(102L)
            .ticker(ticker)
            .indicatorDefinition(smaVolumeDefinition)
            .priceDate(date(11))
            .values(smaLatest.values())
            .state(null)
            .build();

    when(dailyIndicatorRepo.findTickersWithPendingDailyIndicators()).thenReturn(List.of(ticker));
    when(indicatorDefinitionRepository.findAll())
        .thenReturn(List.of(emaDefinition, smaVolumeDefinition));
    when(dailyRepo.findMaxPriceDatePerActiveTicker())
        .thenReturn(List.<Object[]>of(new Object[] {ticker.getTickerId(), date(11)}));
    when(dailyIndicatorRepo.findTop2LatestForActiveTickers()).thenReturn(List.of(emaRow, smaRow));

    calculator.execute(Timeframe.DAILY);

    verify(dailyRepo, never()).findTailBarsForTickers(anyCollection(), anyInt());
    verify(dailyRepo, never()).findByTickerOrderByPriceDateAsc(any());
    verify(dailyIndicatorRepo, never()).saveAll(any());
  }

  @Test
  void dailyIncrementalTailWindowResumesFromStateWithoutFullHistoryLoad() {
    List<DailyPrice> bars = dailyBars(ticker, BAR_COUNT);
    List<PriceBar> priceBars = bars.stream().map(PriceBar::from).toList();
    Map<LocalDate, IndicatorComputationPoint> fullEma =
        emaIndicator.computeStateful(
            priceBars, IndicatorParams.of(emaDefinition.getParams()), PriceSource.CLOSE, null);
    Map<LocalDate, IndicatorComputationPoint> fullSma =
        smaIndicator.computeStateful(
            priceBars,
            IndicatorParams.of(smaVolumeDefinition.getParams()),
            PriceSource.VOLUME,
            null);

    DailyIndicator emaAnchor =
        DailyIndicator.builder()
            .dailyIndicatorId(101L)
            .ticker(ticker)
            .indicatorDefinition(emaDefinition)
            .priceDate(date(8))
            .values(fullEma.get(date(8)).values())
            .state(fullEma.get(date(8)).state())
            .build();
    DailyIndicator smaAnchor =
        DailyIndicator.builder()
            .dailyIndicatorId(102L)
            .ticker(ticker)
            .indicatorDefinition(smaVolumeDefinition)
            .priceDate(date(8))
            .values(fullSma.get(date(8)).values())
            .state(null)
            .build();

    when(dailyIndicatorRepo.findTickersWithPendingDailyIndicators()).thenReturn(List.of(ticker));
    when(indicatorDefinitionRepository.findAll())
        .thenReturn(List.of(emaDefinition, smaVolumeDefinition));
    when(dailyRepo.findMaxPriceDatePerActiveTicker())
        .thenReturn(List.<Object[]>of(new Object[] {ticker.getTickerId(), date(11)}));
    when(dailyIndicatorRepo.findTop2LatestForActiveTickers())
        .thenReturn(List.of(emaAnchor, smaAnchor));
    when(dailyRepo.findTailBarsForTickers(eq(List.of(ticker.getTickerId())), anyInt()))
        .thenReturn(bars.subList(6, BAR_COUNT));

    calculator.execute(Timeframe.DAILY);

    verify(dailyRepo, times(1)).findTailBarsForTickers(eq(List.of(ticker.getTickerId())), eq(32));
    verify(dailyRepo, never()).findByTickerOrderByPriceDateAsc(any());

    List<? extends Indicator> emaSaved = capturedValues(IndicatorType.EMA);
    List<? extends Indicator> smaSaved = capturedValues(IndicatorType.SMA);
    assertEquals(3, emaSaved.size());
    assertEquals(3, smaSaved.size());
    assertEquals(date(9), emaSaved.getFirst().getPriceDate());
    assertEquals(date(11), emaSaved.getLast().getPriceDate());
    assertEquals(fullEma.get(date(11)).values(), emaSaved.getLast().getValues());
    assertEquals(fullEma.get(date(11)).state(), emaSaved.getLast().getState());
    assertEquals(fullSma.get(date(11)).values(), smaSaved.getLast().getValues());
  }

  @Test
  void dailyLegacyNullStateFallsBackToFullHistoryAndBackfillsAnchorState() {
    List<DailyPrice> bars = dailyBars(ticker, BAR_COUNT);
    List<PriceBar> priceBars = bars.stream().map(PriceBar::from).toList();
    Map<LocalDate, IndicatorComputationPoint> fullEma =
        emaIndicator.computeStateful(
            priceBars, IndicatorParams.of(emaDefinition.getParams()), PriceSource.CLOSE, null);

    DailyIndicator legacyAnchor =
        DailyIndicator.builder()
            .dailyIndicatorId(101L)
            .ticker(ticker)
            .indicatorDefinition(emaDefinition)
            .priceDate(date(9))
            .values(fullEma.get(date(9)).values())
            .state(null)
            .build();

    when(dailyIndicatorRepo.findTickersWithPendingDailyIndicators()).thenReturn(List.of(ticker));
    when(indicatorDefinitionRepository.findAll()).thenReturn(List.of(emaDefinition));
    when(dailyRepo.findMaxPriceDatePerActiveTicker())
        .thenReturn(List.<Object[]>of(new Object[] {ticker.getTickerId(), date(11)}));
    when(dailyIndicatorRepo.findTop2LatestForActiveTickers()).thenReturn(List.of(legacyAnchor));

    calculator.execute(Timeframe.DAILY);

    verify(dailyRepo, never()).findTailBarsForTickers(anyCollection(), anyInt());
    verify(dailyRepo, times(1)).findByTickerOrderByPriceDateAsc(ticker);
    assertEquals(fullEma.get(date(9)).state(), legacyAnchor.getState());

    List<? extends Indicator> saved = capturedValues(IndicatorType.EMA);
    // 1 backfilled anchor (date 9) + 2 new rows (date 10, date 11)
    assertEquals(3, saved.size());
  }

  @Test
  void dailyLegacyNullStateOnSameDayBackfillsAnchorStateOnly() {
    List<DailyPrice> bars = dailyBars(ticker, BAR_COUNT);
    List<PriceBar> priceBars = bars.stream().map(PriceBar::from).toList();
    Map<LocalDate, IndicatorComputationPoint> fullEma =
        emaIndicator.computeStateful(
            priceBars, IndicatorParams.of(emaDefinition.getParams()), PriceSource.CLOSE, null);

    DailyIndicator legacyAnchor =
        DailyIndicator.builder()
            .dailyIndicatorId(101L)
            .ticker(ticker)
            .indicatorDefinition(emaDefinition)
            .priceDate(date(11))
            .values(fullEma.get(date(11)).values())
            .state(null)
            .build();

    when(dailyIndicatorRepo.findTickersWithPendingDailyIndicators()).thenReturn(List.of(ticker));
    when(indicatorDefinitionRepository.findAll()).thenReturn(List.of(emaDefinition));
    when(dailyRepo.findMaxPriceDatePerActiveTicker())
        .thenReturn(List.<Object[]>of(new Object[] {ticker.getTickerId(), date(11)}));
    when(dailyIndicatorRepo.findTop2LatestForActiveTickers()).thenReturn(List.of(legacyAnchor));

    calculator.execute(Timeframe.DAILY);

    assertEquals(fullEma.get(date(11)).state(), legacyAnchor.getState());
    assertEquals(1, capturedValues(IndicatorType.EMA).size());
  }

  @Test
  void dailyLargeGapFallsBackToFullHistory() {
    List<DailyPrice> bars = dailyBars(ticker, 40);
    DailyIndicator oldAnchor =
        DailyIndicator.builder()
            .dailyIndicatorId(101L)
            .ticker(ticker)
            .indicatorDefinition(emaDefinition)
            .priceDate(date(2))
            .values(Map.of("value", BigDecimal.TEN))
            .state(Map.of(EmaIndicator.STATE_EMA, BigDecimal.TEN))
            .build();

    when(dailyIndicatorRepo.findTickersWithPendingDailyIndicators()).thenReturn(List.of(ticker));
    when(indicatorDefinitionRepository.findAll()).thenReturn(List.of(emaDefinition));
    when(dailyRepo.findMaxPriceDatePerActiveTicker())
        .thenReturn(List.<Object[]>of(new Object[] {ticker.getTickerId(), date(39)}));
    when(dailyIndicatorRepo.findTop2LatestForActiveTickers()).thenReturn(List.of(oldAnchor));
    when(dailyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(bars);

    calculator.execute(Timeframe.DAILY);

    verify(dailyRepo, never()).findTailBarsForTickers(anyCollection(), anyInt());
    verify(dailyRepo, times(1)).findByTickerOrderByPriceDateAsc(ticker);
  }

  @Test
  void weeklyMidWeekUpdateRecomputesFromPriorWeekAndUnchangedReRunWritesZero() {
    LocalDate week1 = HISTORICAL_MONDAY;
    LocalDate week2 = HISTORICAL_MONDAY.plusWeeks(1);
    LocalDate week3 = HISTORICAL_MONDAY.plusWeeks(2);

    IndicatorDefinition weeklyEmaDef =
        IndicatorDefinition.builder()
            .indicatorDefinitionId(10L)
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 2))
            .build();
    IndicatorDefinition weeklySmaDef =
        IndicatorDefinition.builder()
            .indicatorDefinitionId(11L)
            .indicatorType(IndicatorType.SMA)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 2))
            .build();

    List<WeeklyPrice> mondayBars =
        List.of(
            weeklyBar(ticker, week1, 10),
            weeklyBar(ticker, week2, 12),
            weeklyBar(ticker, week3, 15));
    List<PriceBar> mondayPriceBars = mondayBars.stream().map(PriceBar::from).toList();
    Map<LocalDate, IndicatorComputationPoint> mondayEma =
        emaIndicator.computeStateful(
            mondayPriceBars, IndicatorParams.of(weeklyEmaDef.getParams()), PriceSource.CLOSE, null);
    Map<LocalDate, IndicatorComputationPoint> mondaySma =
        smaIndicator.computeStateful(
            mondayPriceBars, IndicatorParams.of(weeklySmaDef.getParams()), PriceSource.CLOSE, null);

    WeeklyIndicator emaWeek2 =
        WeeklyIndicator.builder()
            .weeklyIndicatorId(1L)
            .ticker(ticker)
            .indicatorDefinition(weeklyEmaDef)
            .priceDate(week2)
            .values(mondayEma.get(week2).values())
            .state(mondayEma.get(week2).state())
            .build();
    WeeklyIndicator emaWeek3 =
        WeeklyIndicator.builder()
            .weeklyIndicatorId(2L)
            .ticker(ticker)
            .indicatorDefinition(weeklyEmaDef)
            .priceDate(week3)
            .values(mondayEma.get(week3).values())
            .state(mondayEma.get(week3).state())
            .build();
    WeeklyIndicator smaWeek3 =
        WeeklyIndicator.builder()
            .weeklyIndicatorId(3L)
            .ticker(ticker)
            .indicatorDefinition(weeklySmaDef)
            .priceDate(week3)
            .values(mondaySma.get(week3).values())
            .state(null)
            .build();

    // Tuesday updates week3 close from 15 -> 21
    List<WeeklyPrice> tuesdayBars =
        List.of(
            weeklyBar(ticker, week1, 10),
            weeklyBar(ticker, week2, 12),
            weeklyBar(ticker, week3, 21));
    List<PriceBar> tuesdayPriceBars = tuesdayBars.stream().map(PriceBar::from).toList();
    final IndicatorComputationPoint expectedTuesdayEma =
        emaIndicator
            .computeStateful(
                tuesdayPriceBars,
                IndicatorParams.of(weeklyEmaDef.getParams()),
                PriceSource.CLOSE,
                null)
            .get(week3);
    final IndicatorComputationPoint expectedTuesdaySma =
        smaIndicator
            .computeStateful(
                tuesdayPriceBars,
                IndicatorParams.of(weeklySmaDef.getParams()),
                PriceSource.CLOSE,
                null)
            .get(week3);

    when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(ticker));
    when(indicatorDefinitionRepository.findAll()).thenReturn(List.of(weeklyEmaDef, weeklySmaDef));
    when(weeklyRepo.findMaxPriceDatePerActiveTicker())
        .thenReturn(List.<Object[]>of(new Object[] {ticker.getTickerId(), week3}));
    when(weeklyIndicatorRepo.findTop2LatestForActiveTickers())
        .thenReturn(List.of(emaWeek3, emaWeek2, smaWeek3));
    when(weeklyRepo.findTailBarsForTickers(eq(List.of(ticker.getTickerId())), anyInt()))
        .thenReturn(tuesdayBars);

    // First Tuesday run: recomputes week3 from week2's state (not compounding week3) and updates
    // in-place
    calculator.execute(Timeframe.WEEKLY);

    verify(weeklyRepo, never()).findByTickerOrderByPriceDateAsc(any());
    verify(weeklyIndicatorRepo, times(1)).saveAll(any());
    assertEquals(expectedTuesdayEma.values(), emaWeek3.getValues());
    assertEquals(expectedTuesdayEma.state(), emaWeek3.getState());
    assertEquals(expectedTuesdaySma.values(), smaWeek3.getValues());

    // Second Tuesday run with unchanged weekly bars: 0 DB writes!
    calculator.execute(Timeframe.WEEKLY);

    verify(weeklyIndicatorRepo, times(1)).saveAll(any());
  }

  @Test
  void weeklyMidWeekMissingPrevStateFallsBackAndBackfillsPrevState() {
    LocalDate week1 = HISTORICAL_MONDAY;
    LocalDate week2 = HISTORICAL_MONDAY.plusWeeks(1);
    LocalDate week3 = HISTORICAL_MONDAY.plusWeeks(2);

    IndicatorDefinition weeklyEmaDef =
        IndicatorDefinition.builder()
            .indicatorDefinitionId(10L)
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 2))
            .build();

    List<WeeklyPrice> bars =
        List.of(
            weeklyBar(ticker, week1, 10),
            weeklyBar(ticker, week2, 12),
            weeklyBar(ticker, week3, 21));

    WeeklyIndicator emaWeek2Legacy =
        WeeklyIndicator.builder()
            .weeklyIndicatorId(1L)
            .ticker(ticker)
            .indicatorDefinition(weeklyEmaDef)
            .priceDate(week2)
            .values(Map.of("value", BigDecimal.valueOf(11)))
            .state(null)
            .build();
    WeeklyIndicator emaWeek3Legacy =
        WeeklyIndicator.builder()
            .weeklyIndicatorId(2L)
            .ticker(ticker)
            .indicatorDefinition(weeklyEmaDef)
            .priceDate(week3)
            .values(Map.of("value", BigDecimal.valueOf(13)))
            .state(null)
            .build();

    when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(ticker));
    when(indicatorDefinitionRepository.findAll()).thenReturn(List.of(weeklyEmaDef));
    when(weeklyRepo.findMaxPriceDatePerActiveTicker())
        .thenReturn(List.<Object[]>of(new Object[] {ticker.getTickerId(), week3}));
    when(weeklyIndicatorRepo.findTop2LatestForActiveTickers())
        .thenReturn(List.of(emaWeek3Legacy, emaWeek2Legacy));
    when(weeklyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(bars);

    calculator.execute(Timeframe.WEEKLY);

    verify(weeklyRepo, never()).findTailBarsForTickers(anyCollection(), anyInt());
    verify(weeklyRepo, times(1)).findByTickerOrderByPriceDateAsc(ticker);
    assertNotNull(emaWeek2Legacy.getState());
    assertNotNull(emaWeek3Legacy.getState());
    assertEquals(2, capturedValues(IndicatorType.EMA).size());
  }

  @Test
  void weeklyNewMondayInsertsFromPreviousMondayState() {
    LocalDate week1 = HISTORICAL_MONDAY;
    LocalDate week2 = HISTORICAL_MONDAY.plusWeeks(1);
    LocalDate week3 = HISTORICAL_MONDAY.plusWeeks(2);

    IndicatorDefinition weeklyEmaDef =
        IndicatorDefinition.builder()
            .indicatorDefinitionId(10L)
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 2))
            .build();

    List<WeeklyPrice> bars =
        List.of(
            weeklyBar(ticker, week1, 10),
            weeklyBar(ticker, week2, 12),
            weeklyBar(ticker, week3, 18));
    List<PriceBar> priceBars = bars.stream().map(PriceBar::from).toList();
    Map<LocalDate, IndicatorComputationPoint> fullEma =
        emaIndicator.computeStateful(
            priceBars, IndicatorParams.of(weeklyEmaDef.getParams()), PriceSource.CLOSE, null);

    WeeklyIndicator emaWeek2 =
        WeeklyIndicator.builder()
            .weeklyIndicatorId(1L)
            .ticker(ticker)
            .indicatorDefinition(weeklyEmaDef)
            .priceDate(week2)
            .values(fullEma.get(week2).values())
            .state(fullEma.get(week2).state())
            .build();

    when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(ticker));
    when(indicatorDefinitionRepository.findAll()).thenReturn(List.of(weeklyEmaDef));
    when(weeklyRepo.findMaxPriceDatePerActiveTicker())
        .thenReturn(List.<Object[]>of(new Object[] {ticker.getTickerId(), week3}));
    when(weeklyIndicatorRepo.findTop2LatestForActiveTickers()).thenReturn(List.of(emaWeek2));
    when(weeklyRepo.findTailBarsForTickers(eq(List.of(ticker.getTickerId())), anyInt()))
        .thenReturn(bars);

    calculator.execute(Timeframe.WEEKLY);

    List<? extends Indicator> saved = capturedValues(IndicatorType.EMA);
    assertEquals(1, saved.size());
    assertEquals(week3, saved.getFirst().getPriceDate());
    assertEquals(fullEma.get(week3).values(), saved.getFirst().getValues());
    assertEquals(fullEma.get(week3).state(), saved.getFirst().getState());
  }

  @Test
  void testExecuteHandlesTickerFailureSelfProxyAndMissingMaxPriceDate() throws Exception {
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
    Ticker noPriceTicker =
        Ticker.builder()
            .tickerId(3L)
            .tickerSymbol("EMPTY")
            .country(Country.US)
            .isActive(true)
            .build();

    when(dailyIndicatorRepo.findTickersWithPendingDailyIndicators())
        .thenReturn(List.of(failingTicker, healthyTicker, noPriceTicker));
    when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(failingTicker, healthyTicker));
    when(indicatorDefinitionRepository.findAll()).thenReturn(List.of(emaDefinition));
    when(dailyRepo.findMaxPriceDatePerActiveTicker())
        .thenReturn(
            List.<Object[]>of(
                new Object[] {failingTicker.getTickerId(), date(11)},
                new Object[] {healthyTicker.getTickerId(), date(11)}));
    when(weeklyRepo.findMaxPriceDatePerActiveTicker())
        .thenReturn(
            List.<Object[]>of(
                new Object[] {failingTicker.getTickerId(), HISTORICAL_MONDAY},
                new Object[] {healthyTicker.getTickerId(), HISTORICAL_MONDAY}));
    when(dailyIndicatorRepo.findTop2LatestForActiveTickers()).thenReturn(List.of());
    when(weeklyIndicatorRepo.findTop2LatestForActiveTickers()).thenReturn(List.of());

    IndicatorCalculator calculatorSpy = spy(calculator);
    doThrow(new RuntimeException("Computation error"))
        .when(calculatorSpy)
        .computeIndicatorForTicker(
            eq(failingTicker), eq(Timeframe.DAILY), any(), any(), any(), any());
    doNothing()
        .when(calculatorSpy)
        .computeIndicatorForTicker(
            eq(healthyTicker), eq(Timeframe.DAILY), any(), any(), any(), any());

    calculatorSpy.execute(Timeframe.DAILY);

    verify(calculatorSpy)
        .computeIndicatorForTicker(
            eq(failingTicker), eq(Timeframe.DAILY), any(), any(), any(), any());
    verify(calculatorSpy)
        .computeIndicatorForTicker(
            eq(healthyTicker), eq(Timeframe.DAILY), any(), any(), any(), any());
    verify(calculatorSpy, never())
        .computeIndicatorForTicker(
            eq(noPriceTicker), eq(Timeframe.DAILY), any(), any(), any(), any());

    IndicatorCalculator selfMock = mock(IndicatorCalculator.class);
    when(selfMock.loadTop2IndicatorsByTickerId(Timeframe.WEEKLY))
        .thenReturn(Collections.emptyMap());
    Field selfField = IndicatorCalculator.class.getDeclaredField("indicatorCalculator");
    selfField.setAccessible(true);
    selfField.set(calculator, selfMock);

    calculator.execute(Timeframe.WEEKLY);

    verify(selfMock)
        .computeIndicatorForTicker(
            eq(failingTicker), eq(Timeframe.WEEKLY), any(), any(), any(), any());
    verify(selfMock)
        .computeIndicatorForTicker(
            eq(healthyTicker), eq(Timeframe.WEEKLY), any(), any(), any(), any());
  }

  @Test
  void testColdStartEmptyBarsAndInsufficientBars() {
    when(dailyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
    calculator.computeIndicatorForTicker(
        ticker, Timeframe.DAILY, List.of(emaDefinition), Map.of(), date(11), null);
    verify(dailyIndicatorRepo, never()).saveAll(any());

    IndicatorDefinition longEmaDef =
        IndicatorDefinition.builder()
            .indicatorDefinitionId(99L)
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 20))
            .build();
    when(dailyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(dailyBars(ticker, 2));
    calculator.computeIndicatorForTicker(
        ticker, Timeframe.DAILY, List.of(longEmaDef), Map.of(), date(1), null);
    verify(dailyIndicatorRepo, never()).saveAll(any());

    when(weeklyRepo.findByTickerOrderByPriceDateAsc(ticker))
        .thenReturn(List.of(weeklyBar(ticker, HISTORICAL_MONDAY, 10)));
    calculator.computeIndicatorForTicker(
        ticker, Timeframe.WEEKLY, List.of(longEmaDef), Map.of(), HISTORICAL_MONDAY, null);
    verify(weeklyIndicatorRepo, never()).saveAll(any());

    // Cold-start backfill saves all points with state
    when(dailyRepo.findByTickerOrderByPriceDateAsc(ticker))
        .thenReturn(dailyBars(ticker, BAR_COUNT));
    calculator.computeIndicatorForTicker(
        ticker,
        Timeframe.DAILY,
        List.of(emaDefinition, smaVolumeDefinition),
        Map.of(),
        date(11),
        null);
    assertEquals(10, capturedValues(IndicatorType.EMA).size());
    assertEquals(11, capturedValues(IndicatorType.SMA).size());
  }

  @Test
  void testUnsupportedTimeframesThrowIllegalArgumentException() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> calculator.execute(null));
    assertThrows(
        IllegalArgumentException.class, () -> calculator.loadTop2IndicatorsByTickerId(null));
    assertThrows(
        IllegalArgumentException.class,
        () -> calculator.loadTailBarsByTickerId(null, List.of(1L), 10));

    Method loadMaxPriceDates =
        IndicatorCalculator.class.getDeclaredMethod("loadMaxPriceDates", Timeframe.class);
    loadMaxPriceDates.setAccessible(true);
    InvocationTargetException maxDatesEx =
        assertThrows(
            InvocationTargetException.class,
            () -> loadMaxPriceDates.invoke(calculator, (Timeframe) null));
    assertInstanceOf(IllegalArgumentException.class, maxDatesEx.getCause());

    Method loadFullBars =
        IndicatorCalculator.class.getDeclaredMethod("loadFullBars", Ticker.class, Timeframe.class);
    loadFullBars.setAccessible(true);
    InvocationTargetException loadBarsEx =
        assertThrows(
            InvocationTargetException.class,
            () -> loadFullBars.invoke(calculator, ticker, (Timeframe) null));
    assertInstanceOf(IllegalArgumentException.class, loadBarsEx.getCause());

    Method computeAndSave =
        IndicatorCalculator.class.getDeclaredMethod(
            "computeAndSaveIndicators",
            Ticker.class,
            Timeframe.class,
            List.class,
            List.class,
            Map.class,
            LocalDate.class);
    computeAndSave.setAccessible(true);
    InvocationTargetException saveEx =
        assertThrows(
            InvocationTargetException.class,
            () ->
                computeAndSave.invoke(
                    calculator, ticker, (Timeframe) null, List.of(), List.of(), Map.of(), date(0)));
    assertInstanceOf(IllegalArgumentException.class, saveEx.getCause());

    Method canUseTailWindow =
        IndicatorCalculator.class.getDeclaredMethod(
            "canUseTailWindow", Timeframe.class, LocalDate.class, List.class, Map.class);
    canUseTailWindow.setAccessible(true);
    DailyIndicator anchor =
        DailyIndicator.builder()
            .dailyIndicatorId(1L)
            .ticker(ticker)
            .indicatorDefinition(smaVolumeDefinition)
            .priceDate(date(1))
            .values(Map.of("value", BigDecimal.ONE))
            .build();
    InvocationTargetException tailEx =
        assertThrows(
            InvocationTargetException.class,
            () ->
                canUseTailWindow.invoke(
                    calculator,
                    null,
                    date(2),
                    List.of(smaVolumeDefinition),
                    Map.of(smaVolumeDefinition.getIndicatorDefinitionId(), List.of(anchor))));
    assertInstanceOf(IllegalArgumentException.class, tailEx.getCause());
  }

  @Test
  void testMapsNumericallyEqualAndHelperEdgeCases() throws Exception {
    Method mapsEqual =
        IndicatorCalculator.class.getDeclaredMethod("mapsNumericallyEqual", Map.class, Map.class);
    mapsEqual.setAccessible(true);

    assertTrue((Boolean) mapsEqual.invoke(calculator, null, null));
    assertFalse((Boolean) mapsEqual.invoke(calculator, null, Map.of()));
    assertFalse((Boolean) mapsEqual.invoke(calculator, Map.of(), null));
    assertFalse(
        (Boolean)
            mapsEqual.invoke(
                calculator,
                Map.of("a", BigDecimal.ONE),
                Map.of("a", BigDecimal.ONE, "b", BigDecimal.TWO)));
    assertFalse(
        (Boolean)
            mapsEqual.invoke(calculator, Map.of("a", BigDecimal.ONE), Map.of("b", BigDecimal.ONE)));
    assertFalse(
        (Boolean)
            mapsEqual.invoke(calculator, Map.of("a", BigDecimal.ONE), Map.of("a", BigDecimal.TWO)));
    assertTrue(
        (Boolean)
            mapsEqual.invoke(
                calculator,
                Map.of("a", new BigDecimal("10.50")),
                Map.of("a", new BigDecimal("10.5000"))));

    Method findFirstIndexAfter =
        IndicatorCalculator.class.getDeclaredMethod(
            "findFirstIndexAfter", List.class, LocalDate.class);
    findFirstIndexAfter.setAccessible(true);
    List<PriceBar> bars = dailyBars(ticker, 3).stream().map(PriceBar::from).toList();
    assertEquals(3, findFirstIndexAfter.invoke(calculator, bars, date(5)));
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
}
