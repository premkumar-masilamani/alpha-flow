package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.SupportResistanceDto;
import com.alphaflow.api.services.SupportResistanceService;
import com.alphaflow.api.services.TickerService;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupportResistanceControllerTest {

  private SupportResistanceService service;
  private TickerService tickerService;
  private SupportResistanceController controller;
  private Ticker ticker;

  @BeforeEach
  void setUp() {
    service = mock(SupportResistanceService.class);
    tickerService = mock(TickerService.class);
    controller = new SupportResistanceController(service, tickerService);
    ticker = Ticker.builder().tickerId(1L).tickerSymbol("AAPL").build();
  }

  @Test
  void testGetSupportResistancesDaily() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    when(tickerService.getTicker("AAPL")).thenReturn(ticker);

    SupportResistanceDto dto =
        SupportResistanceDto.builder()
            .priceDate(date)
            .zoneBottom(new BigDecimal("100.00"))
            .zoneTop(new BigDecimal("105.00"))
            .zoneMidpoint(new BigDecimal("102.50"))
            .levelType("SUPPORT")
            .touchCount(3)
            .build();

    when(service.getSupportResistances(ticker, Timeframe.DAILY, date)).thenReturn(List.of(dto));

    List<SupportResistanceDto> result =
        controller.getSupportResistances("AAPL", Timeframe.DAILY, date);
    assertEquals(1, result.size());
    assertEquals("SUPPORT", result.getFirst().getLevelType());
  }

  @Test
  void testGetSupportResistancesWeekly() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    when(tickerService.getTicker("AAPL")).thenReturn(ticker);

    SupportResistanceDto dto =
        SupportResistanceDto.builder()
            .priceDate(date)
            .zoneBottom(new BigDecimal("90.00"))
            .zoneTop(new BigDecimal("95.00"))
            .zoneMidpoint(new BigDecimal("92.50"))
            .levelType("RESISTANCE")
            .touchCount(2)
            .build();

    when(service.getSupportResistances(ticker, Timeframe.WEEKLY, date)).thenReturn(List.of(dto));

    List<SupportResistanceDto> result =
        controller.getSupportResistances("AAPL", Timeframe.WEEKLY, date);
    assertEquals(1, result.size());
    assertEquals("RESISTANCE", result.getFirst().getLevelType());
  }

  @Test
  void testGetSupportResistancesTickerNotFound() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    when(tickerService.getTicker("UNKNOWN"))
        .thenThrow(new ResourceNotFoundException("Ticker not found: UNKNOWN"));

    assertThrows(
        ResourceNotFoundException.class,
        () -> controller.getSupportResistances("UNKNOWN", Timeframe.DAILY, date));
  }
}
