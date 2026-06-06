package com.alphaflow.engine.calculators;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.alphaflow.engine.calculators.indicators.*;
import com.alphaflow.engine.configs.IndicatorConfig;
import com.alphaflow.engine.configs.IndicatorConfig.IndicatorDefinition;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.IndicatorValue;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import com.alphaflow.persistence.enums.Timeframe;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.IndicatorValueRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
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
  private IndicatorValueRepository valueRepo;
  private IndicatorCalculator calculator;
  private Ticker ticker;

  private static IndicatorDefinition emaDef() {
    IndicatorDefinition d = new IndicatorDefinition();
    d.setType(IndicatorType.EMA);
    d.setSource(PriceSource.CLOSE);
    d.setParams(Map.of("period", 3));
    return d;
  }

  private static IndicatorDefinition smaVolumeDef() {
    IndicatorDefinition d = new IndicatorDefinition();
    d.setType(IndicatorType.SMA);
    d.setSource(PriceSource.VOLUME);
    d.setParams(Map.of("period", 2));
    return d;
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
    valueRepo = mock(IndicatorValueRepository.class);

    IndicatorRegistry registry =
        new IndicatorRegistry(
            List.of(
                new EmaIndicator(),
                new SmaIndicator(),
                new RsiIndicator(),
                new MacdIndicator(),
                new StochasticIndicator()));

    IndicatorConfig properties = new IndicatorConfig();
    properties.setTimeframes(Map.of(Timeframe.DAILY, List.of(emaDef(), smaVolumeDef())));

    calculator =
        new IndicatorCalculator(tickerRepo, registry, properties, dailyRepo, weeklyRepo, valueRepo);

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
    doThrow(new RuntimeException("Computation error")).when(calculatorSpy).processTicker(t1);
    doNothing().when(calculatorSpy).processTicker(t2);

    calculatorSpy.computeIndicators();

    verify(calculatorSpy).processTicker(t1);
    verify(calculatorSpy).processTicker(t2);
  }

  @Test
  void processTickerDeletesAndBackfillsWholeSeries() {
    calculator.processTicker(ticker);

    // Verify deletion of all existing indicator values for the ticker/timeframe
    verify(valueRepo).deleteByTickerAndTimeframe(ticker, Timeframe.DAILY);

    // Verify saving of computed indicators
    List<IndicatorValue> emaValues = capturedValues(IndicatorType.EMA);
    // EMA with period 3: first 2 prices are warm up, so N - 2 = 10 points
    assertEquals(10, emaValues.size());

    List<IndicatorValue> smaValues = capturedValues(IndicatorType.SMA);
    // SMA with period 2: first 1 price is warm up, so N - 1 = 11 points
    assertEquals(11, smaValues.size());
  }

  @Test
  void processTimeframeWithEmptyBarsReturnsEarly() {
    when(dailyRepo.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());

    calculator.processTicker(ticker);

    verify(valueRepo, never()).deleteByTickerAndTimeframe(any(), any());
    verify(valueRepo, never()).saveAll(any());
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

    IndicatorDefinition weeklyDef = new IndicatorDefinition();
    weeklyDef.setType(IndicatorType.EMA);
    weeklyDef.setSource(PriceSource.CLOSE);
    weeklyDef.setParams(Map.of("period", 2));

    IndicatorConfig properties = new IndicatorConfig();
    properties.setTimeframes(Map.of(Timeframe.WEEKLY, List.of(weeklyDef)));

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
            valueRepo);

    calculator.processTicker(ticker);

    verify(valueRepo).deleteByTickerAndTimeframe(ticker, Timeframe.WEEKLY);
    List<IndicatorValue> emaValues = capturedValues(IndicatorType.EMA);
    assertEquals(1, emaValues.size()); // EMA period 2 over 2 bars yields 1 value (the second bar)
  }

  @SuppressWarnings("unchecked")
  private List<IndicatorValue> capturedValues(IndicatorType type) {
    ArgumentCaptor<List<IndicatorValue>> captor = ArgumentCaptor.forClass(List.class);
    verify(valueRepo, atLeast(0)).saveAll(captor.capture());
    return captor.getAllValues().stream()
        .flatMap(List::stream)
        .filter(v -> v.getIndicatorType() == type)
        .toList();
  }
}
