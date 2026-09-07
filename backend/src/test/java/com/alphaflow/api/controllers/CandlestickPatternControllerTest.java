package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.CandlestickPatternDto;
import com.alphaflow.api.services.CandlestickPatternService;
import com.alphaflow.api.services.TickerService;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.PatternSentiment;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CandlestickPatternControllerTest {

  @Test
  void testGetCandlestickPatterns() {
    CandlestickPatternService service = mock(CandlestickPatternService.class);
    TickerService tickerService = mock(TickerService.class);

    Ticker ticker = Ticker.builder().tickerId(1L).tickerSymbol("AAPL").build();

    CandlestickPatternDto dto =
        CandlestickPatternDto.builder()
            .date(LocalDate.of(2026, 5, 29))
            .shortName("HAMMER")
            .longName("Hammer")
            .sentiment(PatternSentiment.BULLISH_REVERSAL)
            .build();

    when(tickerService.getTicker("AAPL")).thenReturn(ticker);
    when(service.getPatterns(ticker, Timeframe.DAILY, 0, 20)).thenReturn(List.of(dto));

    CandlestickPatternController controller =
        new CandlestickPatternController(service, tickerService);

    List<CandlestickPatternDto> result =
        controller.getCandlestickPatterns("AAPL", Timeframe.DAILY, 0, 20);

    assertEquals(1, result.size());
    assertEquals("HAMMER", result.getFirst().shortName());
  }

  @Test
  void testGetCandlestickPatternsTickerNotFound() {
    CandlestickPatternService service = mock(CandlestickPatternService.class);
    TickerService tickerService = mock(TickerService.class);

    when(tickerService.getTicker("UNKNOWN"))
        .thenThrow(new ResourceNotFoundException("Ticker not found: UNKNOWN"));

    CandlestickPatternController controller =
        new CandlestickPatternController(service, tickerService);

    assertThrows(
        ResourceNotFoundException.class,
        () -> controller.getCandlestickPatterns("UNKNOWN", Timeframe.DAILY, 0, 20));
  }
}
