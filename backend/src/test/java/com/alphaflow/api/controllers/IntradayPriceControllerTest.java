package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.api.services.IntradayPriceService;
import com.alphaflow.api.services.TickerService;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.common.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.entities.Ticker;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntradayPriceControllerTest {

  @Test
  void testGetIntradayPriceDataForTickerSuccess() {
    IntradayPriceService intradayPriceService = mock(IntradayPriceService.class);
    TickerService tickerService = mock(TickerService.class);

    Ticker ticker = Ticker.builder().tickerSymbol("NIFTY50").isActive(true).build();
    when(tickerService.getTicker("NIFTY50")).thenReturn(ticker);

    OffsetDateTime candleTime =
        OffsetDateTime.of(2026, 9, 23, 9, 15, 0, 0, ZoneOffset.ofHoursMinutes(5, 30));
    OhlcvDto dto =
        new OhlcvDto(
            candleTime,
            new BigDecimal("25000.00"),
            new BigDecimal("25050.00"),
            new BigDecimal("24990.00"),
            new BigDecimal("25040.00"),
            new BigDecimal("1000.00"));

    LocalDate endDate = LocalDate.of(2026, 9, 23);

    when(intradayPriceService.getIntradayPrice(ticker, Timeframe._15M, null, 0, 250))
        .thenReturn(List.of(dto));
    when(intradayPriceService.getIntradayPrice(ticker, Timeframe._15M, endDate, 0, 250))
        .thenReturn(List.of(dto));

    IntradayPriceController controller =
        new IntradayPriceController(intradayPriceService, tickerService);

    // Call without endDate
    List<OhlcvDto> resultWithoutEndDate =
        controller.getIntradayPriceDataForTicker("NIFTY50", Timeframe._15M, 0, 250, null);
    assertEquals(1, resultWithoutEndDate.size());
    assertEquals(candleTime, resultWithoutEndDate.getFirst().priceDate());

    // Call with endDate
    List<OhlcvDto> resultWithEndDate =
        controller.getIntradayPriceDataForTicker("NIFTY50", Timeframe._15M, 0, 250, endDate);
    assertEquals(1, resultWithEndDate.size());
    assertEquals(candleTime, resultWithEndDate.getFirst().priceDate());
  }

  @Test
  void testGetIntradayPriceDataForTickerSizeClamping() {
    IntradayPriceService intradayPriceService = mock(IntradayPriceService.class);
    TickerService tickerService = mock(TickerService.class);

    Ticker ticker = Ticker.builder().tickerSymbol("NIFTY50").isActive(true).build();
    when(tickerService.getTicker("NIFTY50")).thenReturn(ticker);

    when(intradayPriceService.getIntradayPrice(ticker, Timeframe._15M, null, 0, 1000))
        .thenReturn(List.of());
    when(intradayPriceService.getIntradayPrice(ticker, Timeframe._15M, null, 0, 1))
        .thenReturn(List.of());

    IntradayPriceController controller =
        new IntradayPriceController(intradayPriceService, tickerService);

    List<OhlcvDto> clampedMax =
        controller.getIntradayPriceDataForTicker("NIFTY50", Timeframe._15M, 0, 5000, null);
    assertEquals(0, clampedMax.size());

    List<OhlcvDto> clampedMin =
        controller.getIntradayPriceDataForTicker("NIFTY50", Timeframe._15M, 0, -5, null);
    assertEquals(0, clampedMin.size());
  }

  @Test
  void testGetIntradayPriceDataForTickerUnsupportedTimeframe() {
    IntradayPriceService intradayPriceService = mock(IntradayPriceService.class);
    TickerService tickerService = mock(TickerService.class);

    Ticker ticker = Ticker.builder().tickerSymbol("NIFTY50").isActive(true).build();
    when(tickerService.getTicker("NIFTY50")).thenReturn(ticker);

    IntradayPriceController controller =
        new IntradayPriceController(intradayPriceService, tickerService);

    assertThrows(
        IllegalArgumentException.class,
        () -> controller.getIntradayPriceDataForTicker("NIFTY50", Timeframe.DAILY, 0, 250, null));
  }

  @Test
  void testGetIntradayPriceDataForTickerTickerNotFound() {
    IntradayPriceService intradayPriceService = mock(IntradayPriceService.class);
    TickerService tickerService = mock(TickerService.class);

    when(tickerService.getTicker("UNKNOWN"))
        .thenThrow(new ResourceNotFoundException("Ticker not found: UNKNOWN"));

    IntradayPriceController controller =
        new IntradayPriceController(intradayPriceService, tickerService);

    assertThrows(
        ResourceNotFoundException.class,
        () -> controller.getIntradayPriceDataForTicker("UNKNOWN", Timeframe._15M, 0, 250, null));
  }
}
