package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.SupportResistanceDto;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.DailySupportResistance;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklySupportResistance;
import com.alphaflow.persistence.repositories.DailySupportResistanceRepository;
import com.alphaflow.persistence.repositories.WeeklySupportResistanceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupportResistanceServiceTest {

  private DailySupportResistanceRepository dailyRepo;
  private WeeklySupportResistanceRepository weeklyRepo;
  private SupportResistanceService service;
  private Ticker ticker;

  @BeforeEach
  void setUp() {
    dailyRepo = mock(DailySupportResistanceRepository.class);
    weeklyRepo = mock(WeeklySupportResistanceRepository.class);
    service = new SupportResistanceService(dailyRepo, weeklyRepo);
    ticker = Ticker.builder().tickerId(1L).tickerSymbol("AAPL").build();
  }

  @Test
  void testGetSupportResistancesDaily() {
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

    when(dailyRepo.findByTickerAndPriceDate(ticker, date)).thenReturn(List.of(dsr));

    List<SupportResistanceDto> result =
        service.getSupportResistances(ticker, Timeframe.DAILY, date);
    assertEquals(1, result.size());
    assertEquals("SUPPORT", result.getFirst().getLevelType());
    assertEquals(4, result.getFirst().getTouchCount());
    assertEquals(firstTouch, result.getFirst().getFirstTouchDate());
    assertEquals(lastTouch, result.getFirst().getLastTouchDate());
    assertEquals(new BigDecimal("100.0000"), result.getFirst().getZoneBottom());
    assertEquals(new BigDecimal("105.0000"), result.getFirst().getZoneTop());
    assertEquals(new BigDecimal("102.5000"), result.getFirst().getZoneMidpoint());
    assertEquals(date, result.getFirst().getPriceDate());
  }

  @Test
  void testGetSupportResistancesWeekly() {
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

    when(weeklyRepo.findByTickerAndPriceDate(ticker, date)).thenReturn(List.of(wsr));

    List<SupportResistanceDto> result =
        service.getSupportResistances(ticker, Timeframe.WEEKLY, date);
    assertEquals(1, result.size());
    assertEquals("RESISTANCE", result.getFirst().getLevelType());
    assertEquals(2, result.getFirst().getTouchCount());
    assertEquals(firstTouch, result.getFirst().getFirstTouchDate());
    assertEquals(lastTouch, result.getFirst().getLastTouchDate());
    assertEquals(new BigDecimal("90.0000"), result.getFirst().getZoneBottom());
    assertEquals(new BigDecimal("95.0000"), result.getFirst().getZoneTop());
    assertEquals(new BigDecimal("92.5000"), result.getFirst().getZoneMidpoint());
    assertEquals(date, result.getFirst().getPriceDate());
  }

  @Test
  void testGetSupportResistancesUnsupportedTimeframe() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    assertThrows(
        IllegalArgumentException.class, () -> service.getSupportResistances(ticker, null, date));
  }
}
