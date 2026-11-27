package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.TickerDataResetResponseDto;
import com.alphaflow.api.dtos.TickerDto;
import com.alphaflow.api.services.TickerDataResetService;
import com.alphaflow.api.services.TickerService;
import java.util.List;
import org.junit.jupiter.api.Test;

class TickerControllerTest {

  @Test
  void testGetAllTickers() {

    TickerService service = mock(TickerService.class);
    TickerDataResetService resetService = mock(TickerDataResetService.class);

    TickerDto dto =
        new TickerDto(
            1L,
            "AAPL",
            "Apple Inc.",
            com.alphaflow.persistence.enums.TickerType.STOCK,
            com.alphaflow.persistence.enums.Country.US,
            true);

    when(service.getAllTickers()).thenReturn(List.of(dto));

    TickerController controller = new TickerController(service, resetService);

    List<TickerDto> res = controller.getAllTickers();

    assertEquals(1, res.size());

    assertEquals("AAPL", res.getFirst().tickerSymbol());
    assertEquals(com.alphaflow.persistence.enums.TickerType.STOCK, res.getFirst().tickerType());
    assertEquals(com.alphaflow.persistence.enums.Country.US, res.getFirst().country());
    assertTrue(res.getFirst().isStale());
  }

  @Test
  void testGetTickerBySymbol() {

    TickerService service = mock(TickerService.class);
    TickerDataResetService resetService = mock(TickerDataResetService.class);

    TickerDto dto =
        new TickerDto(
            1L,
            "^NSEI",
            "NIFTY 50",
            com.alphaflow.persistence.enums.TickerType.INDEX,
            com.alphaflow.persistence.enums.Country.IN,
            false);

    when(service.getTickerBySymbol("^NSEI")).thenReturn(dto);

    TickerController controller = new TickerController(service, resetService);

    TickerDto res = controller.getTickerBySymbol("^NSEI");

    assertEquals("^NSEI", res.tickerSymbol());

    assertEquals("NIFTY 50", res.tickerName());
    assertEquals(com.alphaflow.persistence.enums.TickerType.INDEX, res.tickerType());
    assertEquals(com.alphaflow.persistence.enums.Country.IN, res.country());
  }

  @Test
  void testDeleteTickerData() {
    TickerService service = mock(TickerService.class);
    TickerDataResetService resetService = mock(TickerDataResetService.class);
    TickerDataResetResponseDto expected =
        new TickerDataResetResponseDto("AAPL", true, TickerDataResetService.MESSAGE_CLEARED);

    when(resetService.resetTickerData("AAPL")).thenReturn(expected);

    TickerController controller = new TickerController(service, resetService);

    TickerDataResetResponseDto res = controller.deleteTickerData("AAPL");

    assertEquals("AAPL", res.symbol());
    assertTrue(res.cleared());
    assertEquals(TickerDataResetService.MESSAGE_CLEARED, res.message());
  }
}
