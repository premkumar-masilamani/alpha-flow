package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.IndicatorConfigDto;
import com.alphaflow.api.dtos.IndicatorSeriesDto;
import com.alphaflow.api.services.IndicatorService;
import com.alphaflow.api.services.TickerService;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.Ticker;
import java.util.List;
import org.junit.jupiter.api.Test;

class IndicatorControllerTest {

  @Test
  void testGetConfiguredIndicators() {

    IndicatorService service = mock(IndicatorService.class);
    TickerService tickerService = mock(TickerService.class);

    IndicatorConfigDto configDto =
        new IndicatorConfigDto("DAILY", "EMA", "CLOSE", "period=14", "EMA (14)");

    when(service.getConfiguredIndicators()).thenReturn(List.of(configDto));

    IndicatorController controller = new IndicatorController(service, tickerService);

    List<IndicatorConfigDto> res = controller.getConfiguredIndicators();

    assertEquals(1, res.size());

    assertEquals("EMA", res.getFirst().type());
  }

  @Test
  void testGetIndicatorSeriesValid() {

    IndicatorService service = mock(IndicatorService.class);
    TickerService tickerService = mock(TickerService.class);

    Ticker ticker = Ticker.builder().tickerSymbol("AAPL").isActive(true).build();

    IndicatorSeriesDto seriesDto =
        new IndicatorSeriesDto("EMA", "CLOSE", "period=14", "EMA (14)", List.of());

    when(tickerService.getTicker("AAPL")).thenReturn(ticker);
    when(service.getIndicatorSeries(ticker, Timeframe.DAILY, 0, 250))
        .thenReturn(List.of(seriesDto));

    IndicatorController controller = new IndicatorController(service, tickerService);

    List<IndicatorSeriesDto> res = controller.getIndicatorSeries("AAPL", Timeframe.DAILY, 0, 250);
    assertEquals(1, res.size());
    assertEquals("EMA", res.getFirst().type());

    // Should also trim/lowercase logic (if any handled by Spring, but here we call method directly)
    controller.getIndicatorSeries("AAPL", Timeframe.DAILY, 0, 250);

    assertEquals(1, res.size());
  }
}
