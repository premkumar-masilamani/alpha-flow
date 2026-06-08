package com.alphaflow.engine.calculators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeeklyPriceCalculatorTest {

  private static final LocalDate MONDAY = LocalDate.of(2024, 1, 1);

  private final Ticker ticker =
      Ticker.builder().tickerId(1L).tickerSymbol("TST").tickerName("Test").build();

  @Mock private TickerRepository tickerRepository;
  @Mock private DailyPriceRepository dailyPriceRepository;
  @Mock private WeeklyPriceRepository weeklyPriceRepository;

  private WeeklyPriceCalculator calculator;

  @BeforeEach
  void setUp() {
    calculator =
        new WeeklyPriceCalculator(tickerRepository, dailyPriceRepository, weeklyPriceRepository);
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
  void testComputeWeeklyPricesSuccessAndErrorHandling() {
    Ticker t1 = Ticker.builder().tickerSymbol("AAPL").build();
    Ticker t2 = Ticker.builder().tickerSymbol("MSFT").build();

    when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(t1, t2));

    WeeklyPriceCalculator calculatorSpy = spy(calculator);
    doThrow(new RuntimeException("Computation error")).when(calculatorSpy).processTicker(t1);
    doNothing().when(calculatorSpy).processTicker(t2);

    calculatorSpy.computeWeeklyPrices();

    verify(calculatorSpy).processTicker(t1);
    verify(calculatorSpy).processTicker(t2);
  }

  @Test
  void rollsDailyPricesIntoWeeklyOhlcv_whenNoExistingWeekly() {
    List<DailyPrice> dailies =
        List.of(
            daily(MONDAY, "10", "12", "9", "11", 100),
            daily(MONDAY.plusDays(1), "11", "15", "8", "14", 200),
            daily(MONDAY.plusDays(2), "14", "16", "13", "12", 150));

    when(weeklyPriceRepository.findTopByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.empty());

    when(dailyPriceRepository.findTopByTickerOrderByPriceDateAsc(ticker))
        .thenReturn(Optional.of(dailies.getFirst()));

    when(dailyPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            eq(ticker), any()))
        .thenReturn(dailies);

    when(weeklyPriceRepository.findByTickerAndPriceDateGreaterThanEqual(eq(ticker), any()))
        .thenReturn(List.of());

    calculator.processTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<WeeklyPrice>> captor = ArgumentCaptor.forClass(List.class);
    verify(weeklyPriceRepository).saveAll(captor.capture());

    List<WeeklyPrice> saved = captor.getValue();
    assertThat(saved).hasSize(1);

    WeeklyPrice wp = saved.getFirst();
    assertThat(wp.getPriceDate()).isEqualTo(MONDAY);
    assertThat(wp.getPriceOpen()).isEqualByComparingTo("10"); // first day's open
    assertThat(wp.getPriceClose()).isEqualByComparingTo("12"); // last day's close
    assertThat(wp.getPriceHigh()).isEqualByComparingTo("16"); // max high
    assertThat(wp.getPriceLow()).isEqualByComparingTo("8"); // min low
    assertThat(wp.getVolume()).isEqualByComparingTo("450"); // summed volume
  }

  @Test
  void reusesExistingWeeklyRow_forUpdateInsteadOfInsert() {
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

    List<DailyPrice> dailies = List.of(daily(MONDAY, "10", "12", "9", "11", 100));

    when(weeklyPriceRepository.findTopByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.of(existing));

    when(dailyPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            eq(ticker), any()))
        .thenReturn(dailies);

    when(weeklyPriceRepository.findByTickerAndPriceDateGreaterThanEqual(eq(ticker), any()))
        .thenReturn(List.of(existing));

    calculator.processTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<WeeklyPrice>> captor = ArgumentCaptor.forClass(List.class);
    verify(weeklyPriceRepository).saveAll(captor.capture());

    List<WeeklyPrice> saved = captor.getValue();
    assertThat(saved).hasSize(1);
    // The same persistent instance must be reused (updated), preserving its id.
    assertThat(saved.getFirst()).isSameAs(existing);
    assertThat(saved.getFirst().getWeeklyPriceId()).isEqualTo(99L);
    assertThat(saved.getFirst().getPriceClose()).isEqualByComparingTo("11");
  }

  @Test
  void doesNothing_whenNoDailyPrices() {
    when(weeklyPriceRepository.findTopByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.empty());

    when(dailyPriceRepository.findTopByTickerOrderByPriceDateAsc(ticker))
        .thenReturn(Optional.empty());

    calculator.processTicker(ticker);

    verify(weeklyPriceRepository, never()).saveAll(any());
  }
}
