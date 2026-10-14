package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.SupportResistanceDto;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.DailySupportResistance;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.entities.WeeklySupportResistance;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.DailySupportResistanceRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import com.alphaflow.persistence.repositories.WeeklySupportResistanceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupportResistanceServiceTest {

  private TickerService tickerService;
  private DailySupportResistanceRepository dailyRepo;
  private WeeklySupportResistanceRepository weeklyRepo;
  private DailyPriceRepository dailyPriceRepo;
  private WeeklyPriceRepository weeklyPriceRepo;
  private SupportResistanceService service;
  private Ticker ticker;

  @BeforeEach
  void setUp() {
    tickerService = mock(TickerService.class);
    dailyRepo = mock(DailySupportResistanceRepository.class);
    weeklyRepo = mock(WeeklySupportResistanceRepository.class);
    dailyPriceRepo = mock(DailyPriceRepository.class);
    weeklyPriceRepo = mock(WeeklyPriceRepository.class);
    service =
        new SupportResistanceService(
            tickerService, dailyRepo, weeklyRepo, dailyPriceRepo, weeklyPriceRepo);
    ticker = Ticker.builder().tickerId(1L).tickerSymbol("AAPL").build();
  }

  @Test
  void testGetSupportResistancesDailyWithPriceCandle() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    LocalDate firstTouch = LocalDate.of(2026, 1, 10);
    LocalDate lastTouch = LocalDate.of(2026, 5, 20);
    DailySupportResistance dsr =
        DailySupportResistance.builder()
            .ticker(ticker)
            .priceDate(date)
            .firstTouchDate(firstTouch)
            .lastTouchDate(lastTouch)
            .zoneBottom(new BigDecimal("100.0000"))
            .zoneTop(new BigDecimal("105.0000"))
            .zoneMidpoint(new BigDecimal("102.5000"))
            .levelType("SUPPORT")
            .touchCount(4)
            .build();

    DailyPrice dailyPrice =
        DailyPrice.builder()
            .ticker(ticker)
            .priceDate(date)
            .priceHigh(new BigDecimal("105.0000"))
            .priceLow(new BigDecimal("95.0000"))
            .priceClose(new BigDecimal("100.0000"))
            .priceOpen(new BigDecimal("98.0000"))
            .volume(BigDecimal.TEN)
            .build();

    when(dailyPriceRepo.findByTickerAndPriceDate(ticker, date)).thenReturn(Optional.of(dailyPrice));
    // TP = (105 + 95 + 100) / 3 = 100.00. Range 25% -> [75.00, 125.00]
    when(dailyRepo.findByTickerAndPriceDateAndZoneMidpointBetween(
            eq(ticker), eq(date), any(BigDecimal.class), any(BigDecimal.class)))
        .thenReturn(List.of(dsr));

    List<SupportResistanceDto> result =
        service.getSupportResistances(ticker, Timeframe.DAILY, date);
    assertEquals(1, result.size());
    assertEquals("SUPPORT", result.getFirst().getLevelType());
    assertEquals(4, result.getFirst().getTouchCount());
    assertEquals(firstTouch, result.getFirst().getFirstTouchDate());
    assertEquals(lastTouch, result.getFirst().getLastTouchDate());
  }

  @Test
  void testGetSupportResistancesDailyWithoutPriceCandleFallback() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    when(dailyPriceRepo.findByTickerAndPriceDate(ticker, date)).thenReturn(Optional.empty());
    when(dailyRepo.findByTickerAndPriceDate(ticker, date)).thenReturn(List.of());

    List<SupportResistanceDto> result =
        service.getSupportResistances(ticker, Timeframe.DAILY, date, 0.25);
    assertEquals(0, result.size());
    verify(dailyRepo).findByTickerAndPriceDate(ticker, date);
  }

  @Test
  void testGetSupportResistancesDailyNullRangePct() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    when(dailyRepo.findByTickerAndPriceDate(ticker, date)).thenReturn(List.of());

    List<SupportResistanceDto> result =
        service.getSupportResistances(ticker, Timeframe.DAILY, date, null);
    assertEquals(0, result.size());
    verify(dailyRepo).findByTickerAndPriceDate(ticker, date);
  }

  @Test
  void testGetSupportResistancesWeeklyWithPriceCandle() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    LocalDate firstTouch = LocalDate.of(2026, 2, 5);
    LocalDate lastTouch = LocalDate.of(2026, 5, 12);
    WeeklySupportResistance wsr =
        WeeklySupportResistance.builder()
            .ticker(ticker)
            .priceDate(date)
            .firstTouchDate(firstTouch)
            .lastTouchDate(lastTouch)
            .zoneBottom(new BigDecimal("90.0000"))
            .zoneTop(new BigDecimal("95.0000"))
            .zoneMidpoint(new BigDecimal("92.5000"))
            .levelType("RESISTANCE")
            .touchCount(2)
            .build();

    WeeklyPrice weeklyPrice =
        WeeklyPrice.builder()
            .ticker(ticker)
            .priceDate(date)
            .priceHigh(new BigDecimal("105.0000"))
            .priceLow(new BigDecimal("95.0000"))
            .priceClose(new BigDecimal("100.0000"))
            .priceOpen(new BigDecimal("98.0000"))
            .volume(BigDecimal.TEN)
            .build();

    when(weeklyPriceRepo.findByTickerAndPriceDate(ticker, date))
        .thenReturn(Optional.of(weeklyPrice));
    when(weeklyRepo.findByTickerAndPriceDateAndZoneMidpointBetween(
            eq(ticker), eq(date), any(BigDecimal.class), any(BigDecimal.class)))
        .thenReturn(List.of(wsr));

    List<SupportResistanceDto> result =
        service.getSupportResistances(ticker, Timeframe.WEEKLY, date);
    assertEquals(1, result.size());
    assertEquals("RESISTANCE", result.getFirst().getLevelType());
    assertEquals(firstTouch, result.getFirst().getFirstTouchDate());
    assertEquals(lastTouch, result.getFirst().getLastTouchDate());
  }

  @Test
  void testGetSupportResistancesWeeklyWithoutPriceCandleFallback() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    when(weeklyPriceRepo.findByTickerAndPriceDate(ticker, date)).thenReturn(Optional.empty());
    when(weeklyRepo.findByTickerAndPriceDate(ticker, date)).thenReturn(List.of());

    List<SupportResistanceDto> result =
        service.getSupportResistances(ticker, Timeframe.WEEKLY, date, 0.25);
    assertEquals(0, result.size());
    verify(weeklyRepo).findByTickerAndPriceDate(ticker, date);
  }

  @Test
  void testGetSupportResistancesWeeklyNullRangePct() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    when(weeklyRepo.findByTickerAndPriceDate(ticker, date)).thenReturn(List.of());

    List<SupportResistanceDto> result =
        service.getSupportResistances(ticker, Timeframe.WEEKLY, date, null);
    assertEquals(0, result.size());
    verify(weeklyRepo).findByTickerAndPriceDate(ticker, date);
  }

  @Test
  void testGetSupportResistancesUnsupportedTimeframe() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    assertThrows(
        IllegalArgumentException.class, () -> service.getSupportResistances(ticker, null, date));
  }

  @Test
  void testGetSupportResistancesInvalidRangePctThrowsException() {
    LocalDate date = LocalDate.of(2026, 5, 29);

    assertThrows(
        IllegalArgumentException.class,
        () -> service.getSupportResistances(ticker, Timeframe.DAILY, date, 0.0));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.getSupportResistances(ticker, Timeframe.DAILY, date, -0.25));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.getSupportResistances(ticker, Timeframe.DAILY, date, 10.0));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.getSupportResistances(ticker, Timeframe.DAILY, date, Double.NaN));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.getSupportResistances(ticker, Timeframe.DAILY, date, Double.POSITIVE_INFINITY));
  }

  @Test
  void testGetDailySupportResistancesConvenienceMethods() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    when(tickerService.getTicker("AAPL")).thenReturn(ticker);
    when(dailyRepo.findByTickerAndPriceDate(ticker, date)).thenReturn(List.of());

    List<SupportResistanceDto> dailyDtoBySymbol = service.getDailySupportResistances("AAPL", date);
    assertEquals(0, dailyDtoBySymbol.size());

    List<SupportResistanceDto> dailyDtoByEntity = service.getDailySupportResistances(ticker, date);
    assertEquals(0, dailyDtoByEntity.size());

    List<SupportResistanceDto> dailyDtoBySymbolWithRange =
        service.getDailySupportResistances("AAPL", date, 0.15);
    assertEquals(0, dailyDtoBySymbolWithRange.size());

    List<SupportResistanceDto> dailyDtoByEntityWithRange =
        service.getDailySupportResistances(ticker, date, 0.15);
    assertEquals(0, dailyDtoByEntityWithRange.size());
  }

  @Test
  void testGetWeeklySupportResistancesConvenienceMethods() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    when(tickerService.getTicker("AAPL")).thenReturn(ticker);
    when(weeklyRepo.findByTickerAndPriceDate(ticker, date)).thenReturn(List.of());

    List<SupportResistanceDto> weeklyDtoBySymbol =
        service.getWeeklySupportResistances("AAPL", date);
    assertEquals(0, weeklyDtoBySymbol.size());

    List<SupportResistanceDto> weeklyDtoByEntity =
        service.getWeeklySupportResistances(ticker, date);
    assertEquals(0, weeklyDtoByEntity.size());

    List<SupportResistanceDto> weeklyDtoBySymbolWithRange =
        service.getWeeklySupportResistances("AAPL", date, 0.15);
    assertEquals(0, weeklyDtoBySymbolWithRange.size());

    List<SupportResistanceDto> weeklyDtoByEntityWithRange =
        service.getWeeklySupportResistances(ticker, date, 0.15);
    assertEquals(0, weeklyDtoByEntityWithRange.size());
  }
}
