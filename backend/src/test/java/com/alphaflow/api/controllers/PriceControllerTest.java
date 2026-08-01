package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.api.services.DailyPriceService;
import com.alphaflow.api.services.WeeklyPriceService;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PriceControllerTest {

  @Test
  void testGetPriceDataForTicker() {

    DailyPriceService dailyPriceService = mock(DailyPriceService.class);
    WeeklyPriceService weeklyPriceService = mock(WeeklyPriceService.class);
    TickerRepository tickerRepository = mock(TickerRepository.class);

    Ticker ticker = Ticker.builder().tickerSymbol("AAPL").isActive(true).build();

    OhlcvDto dto =
        new OhlcvDto(
            LocalDate.of(2026, 5, 29),
            new BigDecimal("100.00"),
            new BigDecimal("105.00"),
            new BigDecimal("99.00"),
            new BigDecimal("102.00"),
            new BigDecimal("1000.00"));

    when(tickerRepository.findByTickerSymbolIgnoreCase("AAPL")).thenReturn(Optional.of(ticker));
    when(dailyPriceService.getDailyPrice(ticker, 0, 250)).thenReturn(List.of(dto));

    PriceController controller =
        new PriceController(dailyPriceService, weeklyPriceService, tickerRepository);

    List<OhlcvDto> res = controller.getPriceDataForTicker("AAPL", Timeframe.DAILY, 0, 250);

    assertEquals(1, res.size());
    assertEquals(LocalDate.of(2026, 5, 29), res.getFirst().priceDate());

    when(weeklyPriceService.getWeeklyPriceByTickerName("AAPL", 0, 250)).thenReturn(List.of(dto));
    List<OhlcvDto> weeklyRes = controller.getPriceDataForTicker("AAPL", Timeframe.WEEKLY, 0, 250);
    assertEquals(1, weeklyRes.size());
  }
}
