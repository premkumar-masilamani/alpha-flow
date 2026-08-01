package com.alphaflow.engine.calculators;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.alphaflow.engine.configs.IndicatorConfig;
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
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import com.alphaflow.persistence.repositories.DailyIndicatorRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.IndicatorDefinitionRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyIndicatorRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IndicatorCalculatorTest {

  private static final LocalDate EPOCH = LocalDate.of(2021, 1, 4); // a Monday
  private static final int N = 12;

  private TickerRepository tickerRepo;
  private DailyPriceRepository dailyRepo;
  private WeeklyPriceRepository weeklyRepo;
  private DailyIndicatorRepository dailyIndicatorRepo;
  private WeeklyIndicatorRepository weeklyIndicatorRepo;
  private IndicatorCalculator calculator;
  private Ticker ticker;

  private static IndicatorDefinition emaDef() {
    return IndicatorDefinition.builder()
        .indicatorType(IndicatorType.EMA)
        .source(PriceSource.CLOSE)
        .params(Map.of("period", 3))
        .build();
  }

  private static IndicatorDefinition smaVolumeDef() {
    return IndicatorDefinition.builder()
        .indicatorType(IndicatorType.SMA)
        .source(PriceSource.VOLUME)
        .params(Map.of("period", 2))
        .build();
  }

  private static LocalDate date(int index) {
    return EPOCH.plusDays(index);
  }

  private static List<DailyPrice> dailyBars(Ticker ticker) {
    List<DailyPrice> bars = new ArrayList<>(N);
    for (int i = 0; i < N; i++) {
      BigDecimal close = BigDecimal.valueOf(10 + i).setScale(4);
      bars.add(
          DailyPrice.builder()
              .ticker(ticker)
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

  @BeforeEach
  void setUp() {
    tickerRepo = mock(TickerRepository.class);
    dailyRepo = mock(DailyPriceRepository.class);
    weeklyRepo = mock(WeeklyPriceRepository.class);
    dailyIndicatorRepo = mock(DailyIndicatorRepository.class);
    weeklyIndicatorRepo = mock(WeeklyIndicatorRepository.class);

    IndicatorRegistry registry =
        new IndicatorRegistry(
            List.of(
                new EmaIndicator(),
                new SmaIndicator(),
                new RsiIndicator(),
                new MacdIndicator(),
                new StochasticIndicator()));

    IndicatorConfig properties = new IndicatorConfig(mock(IndicatorDefinitionRepository.class));
    properties.setCachedDefinitions(List.of(emaDef(), smaVolumeDef()));

    calculator =
        new IndicatorCalculator(
            tickerRepo,
            registry,
            properties,
            dailyRepo,
            weeklyRepo,
            dailyIndicatorRepo,
            weeklyIndicatorRepo);

    ticker =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("TEST")
            .tickerName("Test")
            .isActive(true)
            .build();

    when(dailyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(dailyBars(ticker));
    when(weeklyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
  }

  @Test
  void testComputeIndicatorsSuccessAndErrorHandling() {
    Ticker t1 = Ticker.builder().tickerId(1L).tickerSymbol("AAPL").isActive(true).build();
    Ticker t2 = Ticker.builder().tickerId(2L).tickerSymbol("MSFT").isActive(true).build();

    when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(t1, t2));

    IndicatorCalculator calculatorSpy = spy(calculator);
    doThrow(new RuntimeException("Computation error"))
        .when(calculatorSpy)
        .computeIndicatorForTicker(t1);
    doNothing().when(calculatorSpy).computeIndicatorForTicker(t2);

    calculatorSpy.computeIndicators();

    verify(calculatorSpy).computeIndicatorForTicker(t1);
    verify(calculatorSpy).computeIndicatorForTicker(t2);
  }

  @Test
  void processTickerDeletesAndBackfillsWholeSeries() {
    calculator.computeIndicatorForTicker(ticker);

    // Verify check of last stored indicator date
    verify(dailyIndicatorRepo, times(2))
        .findFirstByTickerAndIndicatorDefinitionOrderByPriceDateDesc(eq(ticker), any());

    // Verify saving of computed indicators
    List<? extends Indicator> emaValues = capturedValues(IndicatorType.EMA);
    // EMA with period 3: first 2 prices are warm up, so N - 2 = 10 points
    assertEquals(10, emaValues.size());

    List<? extends Indicator> smaValues = capturedValues(IndicatorType.SMA);
    // SMA with period 2: first 1 price is warm up, so N - 1 = 11 points
    assertEquals(11, smaValues.size());
  }

  @Test
  void processTimeframeWithEmptyBarsReturnsEarly() {
    when(dailyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());

    calculator.computeIndicatorForTicker(ticker);

    verify(dailyIndicatorRepo, never())
        .findFirstByTickerAndIndicatorDefinitionOrderByPriceDateDesc(any(), any());
    verify(weeklyIndicatorRepo, never())
        .findFirstByTickerAndIndicatorDefinitionOrderByPriceDateDesc(any(), any());
    verify(dailyIndicatorRepo, never()).saveAll(any());
    verify(weeklyIndicatorRepo, never()).saveAll(any());
  }

  @Test
  void weeklyTimeframeProcessing() {
    com.alphaflow.persistence.entities.WeeklyPrice w1 =
        com.alphaflow.persistence.entities.WeeklyPrice.builder()
            .ticker(ticker)
            .priceDate(date(0))
            .priceOpen(BigDecimal.TEN)
            .priceHigh(BigDecimal.TEN)
            .priceLow(BigDecimal.TEN)
            .priceClose(BigDecimal.TEN)
            .volume(BigDecimal.valueOf(100L))
            .build();

    com.alphaflow.persistence.entities.WeeklyPrice w2 =
        com.alphaflow.persistence.entities.WeeklyPrice.builder()
            .ticker(ticker)
            .priceDate(date(1))
            .priceOpen(BigDecimal.TEN)
            .priceHigh(BigDecimal.TEN)
            .priceLow(BigDecimal.TEN)
            .priceClose(BigDecimal.TEN)
            .volume(BigDecimal.valueOf(100L))
            .build();

    when(weeklyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of(w1, w2));

    IndicatorDefinition weeklyDef =
        IndicatorDefinition.builder()
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 2))
            .build();

    IndicatorConfig properties = new IndicatorConfig(mock(IndicatorDefinitionRepository.class));
    properties.setCachedDefinitions(List.of(weeklyDef));

    calculator =
        new IndicatorCalculator(
            tickerRepo,
            new IndicatorRegistry(
                List.of(
                    new EmaIndicator(),
                    new SmaIndicator(),
                    new RsiIndicator(),
                    new MacdIndicator(),
                    new StochasticIndicator())),
            properties,
            dailyRepo,
            weeklyRepo,
            dailyIndicatorRepo,
            weeklyIndicatorRepo);

    when(dailyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
    calculator.computeIndicatorForTicker(ticker);

    verify(weeklyIndicatorRepo)
        .findFirstByTickerAndIndicatorDefinitionOrderByPriceDateDesc(eq(ticker), any());
    List<? extends Indicator> emaValues = capturedValues(IndicatorType.EMA);
    assertEquals(1, emaValues.size()); // EMA period 2 over 2 bars yields 1 value (the second bar)
  }

  @Test
  void processTickerWithInsufficientBarsDoesNotSave() {
    IndicatorDefinition emaDef =
        IndicatorDefinition.builder()
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 10))
            .build();

    IndicatorConfig properties = new IndicatorConfig(mock(IndicatorDefinitionRepository.class));
    properties.setCachedDefinitions(List.of(emaDef));

    Ticker testTicker = Ticker.builder().tickerId(99L).tickerSymbol("TEST").isActive(true).build();
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

    IndicatorCalculator testCalculator =
        new IndicatorCalculator(
            tickerRepo,
            new IndicatorRegistry(List.of(new EmaIndicator())),
            properties,
            dailyRepo,
            weeklyRepo,
            dailyIndicatorRepo,
            weeklyIndicatorRepo);

    testCalculator.computeIndicatorForTicker(testTicker);

    verify(dailyIndicatorRepo)
        .findFirstByTickerAndIndicatorDefinitionOrderByPriceDateDesc(eq(testTicker), any());
    verify(dailyIndicatorRepo, never()).saveAll(any());
    verify(weeklyIndicatorRepo, never()).saveAll(any());
  }

  @Test
  void processWeeklyTickerWithInsufficientBarsDoesNotSave() {
    com.alphaflow.persistence.entities.WeeklyPrice w1 =
        com.alphaflow.persistence.entities.WeeklyPrice.builder()
            .ticker(ticker)
            .priceDate(date(0))
            .priceOpen(BigDecimal.TEN)
            .priceHigh(BigDecimal.TEN)
            .priceLow(BigDecimal.TEN)
            .priceClose(BigDecimal.TEN)
            .volume(BigDecimal.valueOf(100L))
            .build();

    when(weeklyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of(w1));

    IndicatorDefinition weeklyDef =
        IndicatorDefinition.builder()
            .indicatorType(IndicatorType.EMA)
            .source(PriceSource.CLOSE)
            .params(Map.of("period", 5))
            .build();

    IndicatorConfig properties = new IndicatorConfig(mock(IndicatorDefinitionRepository.class));
    properties.setCachedDefinitions(List.of(weeklyDef));

    calculator =
        new IndicatorCalculator(
            tickerRepo,
            new IndicatorRegistry(List.of(new EmaIndicator())),
            properties,
            dailyRepo,
            weeklyRepo,
            dailyIndicatorRepo,
            weeklyIndicatorRepo);

    when(dailyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
    calculator.computeIndicatorForTicker(ticker);

    verify(weeklyIndicatorRepo)
        .findFirstByTickerAndIndicatorDefinitionOrderByPriceDateDesc(eq(ticker), any());
    verify(dailyIndicatorRepo, never()).saveAll(any());
    verify(weeklyIndicatorRepo, never()).saveAll(any());
  }

  @Test
  void computeIndicatorsUsesSelfProxyIfNotNull() throws Exception {
    Ticker t1 = Ticker.builder().tickerId(1L).tickerSymbol("AAPL").isActive(true).build();
    when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(t1));

    IndicatorCalculator selfMock = mock(IndicatorCalculator.class);

    java.lang.reflect.Field selfField =
        IndicatorCalculator.class.getDeclaredField("indicatorCalculator");
    selfField.setAccessible(true);
    selfField.set(calculator, selfMock);

    calculator.computeIndicators();

    verify(selfMock).computeIndicatorForTicker(t1);
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

    return allValues.stream().filter(v -> v.getIndicatorType() == type).toList();
  }
}
