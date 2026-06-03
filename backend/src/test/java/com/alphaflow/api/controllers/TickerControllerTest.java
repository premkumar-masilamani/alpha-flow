package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.TickerDTO;
import com.alphaflow.api.services.TickerService;
import java.util.List;
import org.junit.jupiter.api.Test;

class TickerControllerTest {

  @Test
  void testGetAllTickers() {

    TickerService service = mock(TickerService.class);

    TickerDTO dto = new TickerDTO(1L, "AAPL", "Apple Inc.");

    when(service.getAllTickers()).thenReturn(List.of(dto));

    TickerController controller = new TickerController(service);

    List<TickerDTO> res = controller.getAllTickers();

    assertEquals(1, res.size());

    assertEquals("AAPL", res.get(0).tickerSymbol());
  }

  @Test
  void testGetTickerBySymbol() {

    TickerService service = mock(TickerService.class);

    TickerDTO dto = new TickerDTO(1L, "AAPL", "Apple Inc.");

    when(service.getTickerBySymbol("AAPL")).thenReturn(dto);

    TickerController controller = new TickerController(service);

    TickerDTO res = controller.getTickerBySymbol("AAPL");

    assertEquals("AAPL", res.tickerSymbol());

    assertEquals("Apple Inc.", res.tickerName());
  }
}
