package com.alphaflow.engine.calculators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeeklyPriceCalculatorTest {

  private static final LocalDate MONDAY = LocalDate.of(2024, 1, 1);
  private static final LocalDate HISTORICAL_MONDAY = LocalDate.of(2025, 1, 6);
  private static final LocalDate HISTORICAL_FRIDAY = LocalDate.of(2025, 1, 10);

  private final Ticker ticker =
      Ticker.builder()
          .tickerId(1L)
          .tickerSymbol("TST")
          .tickerName("Test")
          .country(Country.US)
          .build();

  @Mock private DailyPriceRepository dailyPriceRepository;
  @Mock private WeeklyPriceRepository weeklyPriceRepository;
  @Mock private TickerRepository tickerRepository;

  private WeeklyPriceCalculator calculator;

  @BeforeEach
  void setUp() {
    calculator =
        new WeeklyPriceCalculator(dailyPriceRepository, weeklyPriceRepository, tickerRepository);
  }

  private DailyPrice daily(
      LocalDate date, String open, String high, String low, String close, double vol) {
    return DailyPrice.builder()
        .ticker(ticker)
        .priceDate(date)
        .priceOpen(new BigDecimal(open))
        .priceHigh(new BigDecimal(high))
        .priceLow(new BigDecimal(low))
        .priceClose(new BigDecimal(close))
        .volume(BigDecimal.valueOf(vol))
        .build();
  }

  @Test
  void testComputeWeeklyPricesAllUpToDateShortCircuits() {
    when(weeklyPriceRepository.hasPendingWeeklyPrices()).thenReturn(false);

    assertThat(calculator.shouldRun(null)).isFalse();
    verify(weeklyPriceRepository).hasPendingWeeklyPrices();
  }

  @Test
  void testComputeWeeklyPricesPartialStalenessErrorIsolationAndSelfProxy() throws Exception {
    Ticker failingTicker =
        Ticker.builder().tickerId(1L).tickerSymbol("AAPL").country(Country.US).build();
    Ticker staleTicker =
        Ticker.builder().tickerId(2L).tickerSymbol("MSFT").country(Country.US).build();
    Ticker upToDateTicker =
        Ticker.builder().tickerId(3L).tickerSymbol("GOOG").country(Country.US).build();

    when(tickerRepository.findByIsActiveTrue())
        .thenReturn(List.of(failingTicker, staleTicker, upToDateTicker));

    WeeklyPriceCalculator calculatorSpy = spy(calculator);
    doThrow(new RuntimeException("Computation error"))
        .when(calculatorSpy)
        .processTicker(failingTicker);
    doNothing().when(calculatorSpy).processTicker(staleTicker);
    doNothing().when(calculatorSpy).processTicker(upToDateTicker);

    calculatorSpy.computeWeeklyPrices();

    verify(calculatorSpy).processTicker(failingTicker);
    verify(calculatorSpy).processTicker(staleTicker);
    verify(calculatorSpy).processTicker(upToDateTicker);

    WeeklyPriceCalculator selfMock = mock(WeeklyPriceCalculator.class);
    Field selfField = WeeklyPriceCalculator.class.getDeclaredField("weeklyPriceCalculator");
    selfField.setAccessible(true);
    selfField.set(calculator, selfMock);

    calculator.computeWeeklyPrices();

    verify(selfMock).processTicker(failingTicker);
    verify(selfMock).processTicker(staleTicker);
    verify(selfMock).processTicker(upToDateTicker);
  }

  @Test
  void rollsDailyPricesIntoWeeklyOhlcv_whenNoExistingWeekly() {
    List<DailyPrice> dailies =
        List.of(
            daily(MONDAY, "10", "12", "9", "11", 100),
            daily(MONDAY.plusDays(1), "11", "15", "8", "14", 200),
            daily(MONDAY.plusDays(2), "14", "16", "13", "12", 150));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(dailies);
    when(weeklyPriceRepository.findByTickerAndPriceDateGreaterThanEqual(eq(ticker), eq(MONDAY)))
        .thenReturn(List.of());

    calculator.processTicker(ticker, null);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<WeeklyPrice>> captor = ArgumentCaptor.forClass(List.class);
    verify(weeklyPriceRepository).saveAll(captor.capture());

    List<WeeklyPrice> saved = captor.getValue();
    assertThat(saved).hasSize(1);

    WeeklyPrice weeklyPrice = saved.getFirst();
    assertThat(weeklyPrice.getPriceDate()).isEqualTo(MONDAY);
    assertThat(weeklyPrice.getPriceOpen()).isEqualByComparingTo("10");
    assertThat(weeklyPrice.getPriceClose()).isEqualByComparingTo("12");
    assertThat(weeklyPrice.getPriceHigh()).isEqualByComparingTo("16");
    assertThat(weeklyPrice.getPriceLow()).isEqualByComparingTo("8");
    assertThat(weeklyPrice.getVolume()).isEqualByComparingTo("450");
  }

  @Test
  void reusesExistingWeeklyRow_forMidWeekUpdateIdempotently() {
    WeeklyPrice existing =
        WeeklyPrice.builder()
            .weeklyPriceId(99L)
            .ticker(ticker)
            .priceDate(MONDAY)
            .priceOpen(new BigDecimal("1"))
            .priceHigh(new BigDecimal("1"))
            .priceLow(new BigDecimal("1"))
            .priceClose(new BigDecimal("1"))
            .volume(BigDecimal.ONE)
            .build();

    List<DailyPrice> dailies =
        List.of(
            daily(MONDAY, "10", "12", "9", "11", 100),
            daily(MONDAY.plusDays(2), "11", "14", "10", "13", 200));

    when(dailyPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            eq(ticker), eq(MONDAY)))
        .thenReturn(dailies);
    when(weeklyPriceRepository.findByTickerAndPriceDateGreaterThanEqual(eq(ticker), eq(MONDAY)))
        .thenReturn(List.of(existing));

    calculator.processTicker(ticker, MONDAY);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<WeeklyPrice>> captor = ArgumentCaptor.forClass(List.class);
    verify(weeklyPriceRepository).saveAll(captor.capture());

    List<WeeklyPrice> saved = captor.getValue();
    assertThat(saved).hasSize(1);
    assertThat(saved.getFirst()).isSameAs(existing);
    assertThat(saved.getFirst().getWeeklyPriceId()).isEqualTo(99L);
    assertThat(saved.getFirst().getPriceClose()).isEqualByComparingTo("13");
    assertThat(saved.getFirst().getVolume()).isEqualByComparingTo("300");
  }

  @Test
  void doesNothing_whenNoDailyPricesOnFullBackfill() {
    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());

    calculator.processTicker(ticker, null);

    verify(weeklyPriceRepository, never()).saveAll(any());
  }

  @Test
  void doesNothing_whenNoNewDailyPricesFoundOnIncrementalRun() {
    when(dailyPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            eq(ticker), eq(MONDAY)))
        .thenReturn(List.of());

    calculator.processTicker(ticker, MONDAY);

    verify(weeklyPriceRepository, never()).saveAll(any());
  }

  @Test
  void rollsDailyPricesIntoMultipleWeeks_whenSpanningWeeks() {
    LocalDate nextMonday = MONDAY.plusWeeks(1);
    List<DailyPrice> dailies =
        List.of(
            daily(MONDAY, "10", "12", "9", "11", 100),
            daily(nextMonday, "12", "18", "11", "17", 250));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(dailies);
    when(weeklyPriceRepository.findByTickerAndPriceDateGreaterThanEqual(eq(ticker), eq(MONDAY)))
        .thenReturn(List.of());

    calculator.processTicker(ticker, null);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<WeeklyPrice>> captor = ArgumentCaptor.forClass(List.class);
    verify(weeklyPriceRepository).saveAll(captor.capture());

    List<WeeklyPrice> saved = captor.getValue();
    assertThat(saved).hasSize(2);
    assertThat(saved.getFirst().getPriceDate()).isEqualTo(MONDAY);
    assertThat(saved.getLast().getPriceDate()).isEqualTo(nextMonday);
  }

  @Test
  void testPipelineJobMethods() {
    assertThat(calculator.getJobName()).isEqualTo("CALCULATE_WEEKLY_PRICE");

    // When up to date: shouldRun returns false
    when(weeklyPriceRepository.hasPendingWeeklyPrices()).thenReturn(false);
    assertThat(calculator.shouldRun(null)).isFalse();

    // When stale: shouldRun returns true
    when(weeklyPriceRepository.hasPendingWeeklyPrices()).thenReturn(true);
    assertThat(calculator.shouldRun(null)).isTrue();

    // Test execute delegating to computeWeeklyPrices
    when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of());
    calculator.execute(null);
  }
}
