package com.alphaflow.engine.calculators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.common.enums.IntradayTimeframe;
import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.IntradayPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyPriceFromIntradayCalculatorTest {

  @Mock private TickerRepository tickerRepository;
  @Mock private IntradayPriceRepository intradayPriceRepository;
  @Mock private DailyPriceRepository dailyPriceRepository;

  private DailyPriceFromIntradayCalculator calculator;

  private Ticker indianTicker;
  private Ticker usTicker;

  @BeforeEach
  void setUp() {
    calculator =
        new DailyPriceFromIntradayCalculator(
            tickerRepository, intradayPriceRepository, dailyPriceRepository);

    indianTicker =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("RELIANCE")
            .country(Country.IN)
            .isActive(true)
            .build();

    usTicker =
        Ticker.builder()
            .tickerId(2L)
            .tickerSymbol("AAPL")
            .country(Country.US)
            .isActive(true)
            .build();
  }

  @Test
  void testComputeDailyPricesFromIntradayProcessesOnlyIndianTickers() {
    when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(indianTicker, usTicker));
    when(dailyPriceRepository.findTopByTickerOrderByPriceDateDesc(indianTicker))
        .thenReturn(Optional.empty());
    when(intradayPriceRepository.findByTickerAndTimeframeOrderByPriceTimeAsc(
            eq(indianTicker), eq(IntradayTimeframe._15M.getValue())))
        .thenReturn(Collections.emptyList());

    calculator.computeDailyPricesFromIntraday();

    verify(intradayPriceRepository, times(1))
        .findByTickerAndTimeframeOrderByPriceTimeAsc(eq(indianTicker), eq("15M"));
    verify(intradayPriceRepository, never())
        .findByTickerAndTimeframeOrderByPriceTimeAsc(eq(usTicker), any());
  }

  @Test
  void testProcessTickerWithExistingDailyPriceAggregatesAndUpdates() {
    LocalDate date = LocalDate.of(2026, 11, 6);
    DailyPrice existingDaily =
        DailyPrice.builder()
            .dailyPriceId(10L)
            .ticker(indianTicker)
            .priceDate(date)
            .priceOpen(new BigDecimal("100.0000"))
            .priceHigh(new BigDecimal("100.0000"))
            .priceLow(new BigDecimal("100.0000"))
            .priceClose(new BigDecimal("100.0000"))
            .volume(new BigDecimal("1000.0000"))
            .build();

    when(dailyPriceRepository.findTopByTickerOrderByPriceDateDesc(indianTicker))
        .thenReturn(Optional.of(existingDaily));

    OffsetDateTime time1 = date.atTime(LocalTime.of(9, 15)).atZone(MarketConfig.IST_ZONE).toOffsetDateTime();
    OffsetDateTime time2 = date.atTime(LocalTime.of(9, 30)).atZone(MarketConfig.IST_ZONE).toOffsetDateTime();
    OffsetDateTime time3 = date.atTime(LocalTime.of(15, 15)).atZone(MarketConfig.IST_ZONE).toOffsetDateTime();

    IntradayPrice bar1 =
        IntradayPrice.builder()
            .intradayPriceId(101L)
            .ticker(indianTicker)
            .timeframe("15M")
            .priceTime(time1)
            .priceOpen(new BigDecimal("2500.0000"))
            .priceHigh(new BigDecimal("2520.0000"))
            .priceLow(new BigDecimal("2495.0000"))
            .priceClose(new BigDecimal("2510.0000"))
            .volume(new BigDecimal("50000.0000"))
            .build();

    IntradayPrice bar2 =
        IntradayPrice.builder()
            .intradayPriceId(102L)
            .ticker(indianTicker)
            .timeframe("15M")
            .priceTime(time2)
            .priceOpen(new BigDecimal("2510.0000"))
            .priceHigh(new BigDecimal("2550.0000"))
            .priceLow(new BigDecimal("2505.0000"))
            .priceClose(new BigDecimal("2545.0000"))
            .volume(new BigDecimal("80000.0000"))
            .build();

    IntradayPrice bar3 =
        IntradayPrice.builder()
            .intradayPriceId(103L)
            .ticker(indianTicker)
            .timeframe("15M")
            .priceTime(time3)
            .priceOpen(new BigDecimal("2530.0000"))
            .priceHigh(new BigDecimal("2535.0000"))
            .priceLow(new BigDecimal("2480.0000"))
            .priceClose(new BigDecimal("2490.0000"))
            .volume(new BigDecimal("70000.0000"))
            .build();

    when(intradayPriceRepository.findByTickerAndTimeframeAndPriceTimeGreaterThanEqualOrderByPriceTimeAsc(
            eq(indianTicker), eq("15M"), any(OffsetDateTime.class)))
        .thenReturn(List.of(bar1, bar2, bar3));

    when(dailyPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            indianTicker, date))
        .thenReturn(List.of(existingDaily));

    calculator.processTicker(indianTicker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyPrice>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyPriceRepository).saveAll(captor.capture());

    List<DailyPrice> saved = captor.getValue();
    assertEquals(1, saved.size());
    DailyPrice updated = saved.getFirst();
    assertEquals(10L, updated.getDailyPriceId());
    assertEquals(date, updated.getPriceDate());
    assertEquals(new BigDecimal("2500.0000"), updated.getPriceOpen()); // from bar1
    assertEquals(new BigDecimal("2550.0000"), updated.getPriceHigh()); // max (bar2)
    assertEquals(new BigDecimal("2480.0000"), updated.getPriceLow()); // min (bar3)
    assertEquals(new BigDecimal("2490.0000"), updated.getPriceClose()); // from bar3
    assertEquals(new BigDecimal("200000.0000"), updated.getVolume()); // 50k + 80k + 70k
  }

  @Test
  void testProcessTickerWithoutExistingDailyPriceCreatesNewBars() {
    when(dailyPriceRepository.findTopByTickerOrderByPriceDateDesc(indianTicker))
        .thenReturn(Optional.empty());

    LocalDate date = LocalDate.of(2026, 11, 5);
    OffsetDateTime time = date.atTime(LocalTime.of(10, 0)).atZone(MarketConfig.IST_ZONE).toOffsetDateTime();

    IntradayPrice bar =
        IntradayPrice.builder()
            .intradayPriceId(201L)
            .ticker(indianTicker)
            .timeframe("15M")
            .priceTime(time)
            .priceOpen(new BigDecimal("100.0000"))
            .priceHigh(new BigDecimal("105.0000"))
            .priceLow(new BigDecimal("98.0000"))
            .priceClose(new BigDecimal("102.0000"))
            .volume(new BigDecimal("12000.0000"))
            .build();

    when(intradayPriceRepository.findByTickerAndTimeframeOrderByPriceTimeAsc(indianTicker, "15M"))
        .thenReturn(List.of(bar));
    when(dailyPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            indianTicker, date))
        .thenReturn(Collections.emptyList());

    calculator.processTicker(indianTicker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyPrice>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyPriceRepository).saveAll(captor.capture());

    List<DailyPrice> saved = captor.getValue();
    assertEquals(1, saved.size());
    DailyPrice newDaily = saved.getFirst();
    assertEquals(date, newDaily.getPriceDate());
    assertEquals(indianTicker, newDaily.getTicker());
    assertEquals(new BigDecimal("100.0000"), newDaily.getPriceOpen());
    assertEquals(new BigDecimal("105.0000"), newDaily.getPriceHigh());
    assertEquals(new BigDecimal("98.0000"), newDaily.getPriceLow());
    assertEquals(new BigDecimal("102.0000"), newDaily.getPriceClose());
    assertEquals(new BigDecimal("12000.0000"), newDaily.getVolume());
  }

  @Test
  void testProcessTickerWithNoIntradayCandlesReturnsEarly() {
    when(dailyPriceRepository.findTopByTickerOrderByPriceDateDesc(indianTicker))
        .thenReturn(Optional.empty());
    when(intradayPriceRepository.findByTickerAndTimeframeOrderByPriceTimeAsc(indianTicker, "15M"))
        .thenReturn(Collections.emptyList());

    calculator.processTicker(indianTicker);

    verify(dailyPriceRepository, never()).saveAll(any());
  }

  @Test
  void testComputeDailyPricesFromIntradayHandlesExceptionPerTicker() {
    Ticker ticker2 =
        Ticker.builder()
            .tickerId(3L)
            .tickerSymbol("INFY")
            .country(Country.IN)
            .isActive(true)
            .build();

    when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(indianTicker, ticker2));
    doThrow(new RuntimeException("Simulated error"))
        .when(dailyPriceRepository)
        .findTopByTickerOrderByPriceDateDesc(indianTicker);

    when(dailyPriceRepository.findTopByTickerOrderByPriceDateDesc(ticker2))
        .thenReturn(Optional.empty());
    when(intradayPriceRepository.findByTickerAndTimeframeOrderByPriceTimeAsc(ticker2, "15M"))
        .thenReturn(Collections.emptyList());

    calculator.computeDailyPricesFromIntraday();

    verify(dailyPriceRepository).findTopByTickerOrderByPriceDateDesc(ticker2);
  }
}
