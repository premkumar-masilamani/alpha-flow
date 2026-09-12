package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.ChartPatternDto;
import com.alphaflow.api.services.ChartPatternService;
import com.alphaflow.api.services.TickerService;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.ChartPatternStatus;
import com.alphaflow.persistence.enums.ChartPatternType;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChartPatternControllerTest {

  @Test
  void testGetChartPatterns() {
    ChartPatternService service = mock(ChartPatternService.class);
    TickerService tickerService = mock(TickerService.class);

    Ticker ticker = Ticker.builder().tickerId(1L).tickerSymbol("AAPL").build();

    ChartPatternDto dto =
        ChartPatternDto.builder()
            .id(1L)
            .patternType(ChartPatternType.DOUBLE_BOTTOM)
            .shortName("DB")
            .displayName("Double Bottom")
            .sentiment(ChartPatternType.DOUBLE_BOTTOM.getSentiment())
            .status(ChartPatternStatus.IN_PROGRESS)
            .startDate(LocalDate.of(2026, 5, 1))
            .endDate(LocalDate.of(2026, 5, 20))
            .build();

    when(tickerService.getTicker("AAPL")).thenReturn(ticker);
    when(service.getPatterns(ticker, Timeframe.DAILY, ChartPatternStatus.IN_PROGRESS, 0, 20))
        .thenReturn(List.of(dto));

    ChartPatternController controller = new ChartPatternController(service, tickerService);

    List<ChartPatternDto> result =
        controller.getChartPatterns("AAPL", Timeframe.DAILY, ChartPatternStatus.IN_PROGRESS, 0, 20);

    assertEquals(1, result.size());
    assertEquals("DB", result.getFirst().shortName());
    assertEquals(ChartPatternStatus.IN_PROGRESS, result.getFirst().status());
  }

  @Test
  void testGetChartPatternsSizeCapped() {
    ChartPatternService service = mock(ChartPatternService.class);
    TickerService tickerService = mock(TickerService.class);

    Ticker ticker = Ticker.builder().tickerId(1L).tickerSymbol("AAPL").build();

    when(tickerService.getTicker("AAPL")).thenReturn(ticker);
    when(service.getPatterns(ticker, Timeframe.WEEKLY, null, 0, 250)).thenReturn(List.of());

    ChartPatternController controller = new ChartPatternController(service, tickerService);

    List<ChartPatternDto> result =
        controller.getChartPatterns("AAPL", Timeframe.WEEKLY, null, 0, 500);

    assertEquals(0, result.size());
  }

  @Test
  void testGetChartPatternsTickerNotFound() {
    ChartPatternService service = mock(ChartPatternService.class);
    TickerService tickerService = mock(TickerService.class);

    when(tickerService.getTicker("UNKNOWN"))
        .thenThrow(new ResourceNotFoundException("Ticker not found: UNKNOWN"));

    ChartPatternController controller = new ChartPatternController(service, tickerService);

    assertThrows(
        ResourceNotFoundException.class,
        () -> controller.getChartPatterns("UNKNOWN", Timeframe.DAILY, null, 0, 20));
  }
}
