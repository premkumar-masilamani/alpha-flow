package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.api.dtos.QuoteDto;
import com.alphaflow.api.services.DailyPriceService;
import com.alphaflow.api.services.QuoteService;
import com.alphaflow.api.services.TickerService;
import com.alphaflow.api.services.WeeklyPriceService;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PriceControllerTest {

  @Test
  void testGetPriceDataForTicker() {
    DailyPriceService dailyPriceService = mock(DailyPriceService.class);
    WeeklyPriceService weeklyPriceService = mock(WeeklyPriceService.class);
    QuoteService quoteService = mock(QuoteService.class);
    TickerService tickerService = mock(TickerService.class);

    Ticker ticker = Ticker.builder().tickerSymbol("AAPL").isActive(true).build();

    OffsetDateTime date = LocalDate.of(2026, 5, 29).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

    OhlcvDto dto =
        new OhlcvDto(
            date,
            new BigDecimal("100.00"),
            new BigDecimal("105.00"),
            new BigDecimal("99.00"),
            new BigDecimal("102.00"),
            new BigDecimal("1000.00"));

    when(tickerService.getTicker("AAPL")).thenReturn(ticker);
    when(dailyPriceService.getDailyPrice(ticker, 0, 250)).thenReturn(List.of(dto));
    when(weeklyPriceService.getWeeklyPrice(ticker, 0, 250)).thenReturn(List.of(dto));

    PriceController controller =
        new PriceController(dailyPriceService, weeklyPriceService, quoteService, tickerService);

    // DAILY
    List<OhlcvDto> dailyRes = controller.getPriceDataForTicker("AAPL", Timeframe.DAILY, 0, 250);
    assertEquals(1, dailyRes.size());
    assertEquals(date, dailyRes.getFirst().priceDate());

    // WEEKLY
    List<OhlcvDto> weeklyRes = controller.getPriceDataForTicker("AAPL", Timeframe.WEEKLY, 0, 250);
    assertEquals(1, weeklyRes.size());

    // Quote
    QuoteDto mockQuote =
        QuoteDto.builder()
            .currentPrice(BigDecimal.valueOf(150.0))
            .timestamp("2026-05-29T10:00:00Z")
            .build();
    when(quoteService.getQuote(ticker)).thenReturn(mockQuote);
    QuoteDto quoteRes = controller.getQuoteForTicker("AAPL");
    assertEquals(BigDecimal.valueOf(150.0), quoteRes.currentPrice());
    assertEquals("2026-05-29T10:00:00Z", quoteRes.timestamp());

    // Unsupported timeframe
    assertThrows(
        IllegalArgumentException.class,
        () -> controller.getPriceDataForTicker("AAPL", Timeframe._15M, 0, 250));
    assertThrows(
        IllegalArgumentException.class,
        () -> controller.getPriceDataForTicker("AAPL", null, 0, 250));

    // Unknown ticker
    when(tickerService.getTicker("UNKNOWN"))
        .thenThrow(new ResourceNotFoundException("Ticker not found: UNKNOWN"));
    assertThrows(
        ResourceNotFoundException.class,
        () -> controller.getPriceDataForTicker("UNKNOWN", Timeframe.WEEKLY, 0, 250));
  }

  @Test
  void testGetPriceDataForTickerSizeClamping() {
    DailyPriceService dailyPriceService = mock(DailyPriceService.class);
    WeeklyPriceService weeklyPriceService = mock(WeeklyPriceService.class);
    QuoteService quoteService = mock(QuoteService.class);
    TickerService tickerService = mock(TickerService.class);

    Ticker ticker = Ticker.builder().tickerSymbol("AAPL").isActive(true).build();
    when(tickerService.getTicker("AAPL")).thenReturn(ticker);

    when(dailyPriceService.getDailyPrice(ticker, 0, 1000)).thenReturn(List.of());
    when(dailyPriceService.getDailyPrice(ticker, 0, 1)).thenReturn(List.of());

    PriceController controller =
        new PriceController(dailyPriceService, weeklyPriceService, quoteService, tickerService);

    List<OhlcvDto> clampedMax = controller.getPriceDataForTicker("AAPL", Timeframe.DAILY, 0, 5000);
    assertEquals(0, clampedMax.size());

    List<OhlcvDto> clampedMin = controller.getPriceDataForTicker("AAPL", Timeframe.DAILY, 0, -10);
    assertEquals(0, clampedMin.size());
  }
}
