package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.IndicatorConfigDto;
import com.alphaflow.api.dtos.IndicatorSeriesDto;
import com.alphaflow.api.services.IndicatorService;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IndicatorControllerTest {

  @Test
  void testGetConfiguredIndicators() {

    IndicatorService service = mock(IndicatorService.class);
    TickerRepository tickerRepository = mock(TickerRepository.class);

    IndicatorConfigDto configDto =
        new IndicatorConfigDto("DAILY", "EMA", "CLOSE", "period=14", "EMA (14)");

    when(service.getConfiguredIndicators()).thenReturn(List.of(configDto));

    IndicatorController controller = new IndicatorController(service, tickerRepository);

    List<IndicatorConfigDto> res = controller.getConfiguredIndicators();

    assertEquals(1, res.size());

    assertEquals("EMA", res.getFirst().type());
  }

  @Test
  void testGetIndicatorSeriesValid() {

    IndicatorService service = mock(IndicatorService.class);
    TickerRepository tickerRepository = mock(TickerRepository.class);

    Ticker ticker = Ticker.builder().tickerSymbol("AAPL").isActive(true).build();

    IndicatorSeriesDto seriesDto =
        new IndicatorSeriesDto("EMA", "CLOSE", "period=14", "EMA (14)", List.of());

    when(tickerRepository.findByTickerSymbolIgnoreCase("AAPL")).thenReturn(Optional.of(ticker));
    when(service.getIndicatorSeries(ticker, Timeframe.DAILY, 0, 250))
        .thenReturn(List.of(seriesDto));

    IndicatorController controller = new IndicatorController(service, tickerRepository);

    List<IndicatorSeriesDto> res = controller.getIndicatorSeries("AAPL", Timeframe.DAILY, 0, 250);
    assertEquals(1, res.size());
    assertEquals("EMA", res.getFirst().type());

    // Should also trim/lowercase logic (if any handled by Spring, but here we call method directly)
    controller.getIndicatorSeries("AAPL", Timeframe.DAILY, 0, 250);

    assertEquals(1, res.size());
  }
}
