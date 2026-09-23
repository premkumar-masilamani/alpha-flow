package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.api.services.DailyPriceService;
import com.alphaflow.api.services.TickerService;
import com.alphaflow.api.services.WeeklyPriceService;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PriceControllerTest {

  @Test
  void testGetPriceDataForTicker() {

    DailyPriceService dailyPriceService = mock(DailyPriceService.class);
    WeeklyPriceService weeklyPriceService = mock(WeeklyPriceService.class);
    com.alphaflow.api.services.IntradayPriceService intradayPriceService =
        mock(com.alphaflow.api.services.IntradayPriceService.class);
    com.alphaflow.api.services.QuoteService quoteService =
        mock(com.alphaflow.api.services.QuoteService.class);
    TickerService tickerService = mock(TickerService.class);

    Ticker ticker = Ticker.builder().tickerSymbol("AAPL").isActive(true).build();

    OhlcvDto dto =
        new OhlcvDto(
            LocalDate.of(2026, 5, 29),
            new BigDecimal("100.00"),
            new BigDecimal("105.00"),
            new BigDecimal("99.00"),
            new BigDecimal("102.00"),
            new BigDecimal("1000.00"));

    when(tickerService.getTicker("AAPL")).thenReturn(ticker);
    when(dailyPriceService.getDailyPrice(ticker, 0, 250)).thenReturn(List.of(dto));

    PriceController controller =
        new PriceController(
            dailyPriceService,
            weeklyPriceService,
            intradayPriceService,
            quoteService,
            tickerService);

    List<OhlcvDto> res = controller.getPriceDataForTicker("AAPL", Timeframe.DAILY, 0, 250);

    assertEquals(1, res.size());
    assertEquals("2026-05-29", res.getFirst().priceDate());

    when(weeklyPriceService.getWeeklyPrice(ticker, 0, 250)).thenReturn(List.of(dto));
    List<OhlcvDto> weeklyRes = controller.getPriceDataForTicker("AAPL", Timeframe.WEEKLY, 0, 250);
    assertEquals(1, weeklyRes.size());

    when(intradayPriceService.getIntradayPrice(ticker, 0, 250)).thenReturn(List.of(dto));
    List<OhlcvDto> intradayRes =
        controller.getPriceDataForTicker("AAPL", Timeframe.FIFTEEN_MINUTE, 0, 250);
    assertEquals(1, intradayRes.size());

    com.alphaflow.api.dtos.QuoteDto mockQuote =
        com.alphaflow.api.dtos.QuoteDto.builder().symbol("AAPL").build();
    when(quoteService.getQuote(ticker)).thenReturn(mockQuote);
    com.alphaflow.api.dtos.QuoteDto quoteRes = controller.getQuoteForTicker("AAPL");
    assertEquals("AAPL", quoteRes.symbol());

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> controller.getPriceDataForTicker("AAPL", null, 0, 250));

    when(tickerService.getTicker("UNKNOWN"))
        .thenThrow(new ResourceNotFoundException("Ticker not found: UNKNOWN"));

    org.junit.jupiter.api.Assertions.assertThrows(
        ResourceNotFoundException.class,
        () -> controller.getPriceDataForTicker("UNKNOWN", Timeframe.WEEKLY, 0, 250));
  }
}
