package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.TickerDTO;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TickerServiceTest {

  @Test
  void testGetTickerBySymbolSuccess() {
    TickerRepository repo = mock(TickerRepository.class);
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setTickerName("Apple Inc.");
    ticker.setActive(true);

    when(repo.findByTickerSymbol("AAPL")).thenReturn(Optional.of(ticker));

    TickerService service = new TickerService(repo);
    TickerDTO dto = service.getTickerBySymbol("AAPL");

    assertEquals("AAPL", dto.tickerSymbol());
    assertEquals("Apple Inc.", dto.tickerName());
  }

  @Test
  void testGetTickerBySymbolNotFound() {
    TickerRepository repo = mock(TickerRepository.class);
    when(repo.findByTickerSymbol("MSFT")).thenReturn(Optional.empty());

    TickerService service = new TickerService(repo);
    assertThrows(ResourceNotFoundException.class, () -> service.getTickerBySymbol("MSFT"));
  }

  @Test
  void testGetAllTickers() {
    TickerRepository repo = mock(TickerRepository.class);
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setTickerName("Apple Inc.");
    ticker.setActive(true);

    when(repo.findByIsActiveTrue()).thenReturn(List.of(ticker));

    TickerService service = new TickerService(repo);
    List<TickerDTO> list = service.getAllTickers();

    assertEquals(1, list.size());
    assertEquals("AAPL", list.get(0).tickerSymbol());
  }
}
