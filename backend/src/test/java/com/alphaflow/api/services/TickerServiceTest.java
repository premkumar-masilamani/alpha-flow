package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.TickerDto;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TickerServiceTest {

  @Test
  void testGetTickerSuccess() {
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setTickerName("Apple Inc.");
    ticker.setTickerType(com.alphaflow.persistence.enums.TickerType.STOCK);
    ticker.setCountry(com.alphaflow.persistence.enums.Country.US);
    ticker.setActive(true);

    TickerRepository repo = mock(TickerRepository.class);
    when(repo.findByTickerSymbolIgnoreCase("AAPL")).thenReturn(Optional.of(ticker));

    TickerService service = new TickerService(repo);
    Ticker result = service.getTicker("AAPL");

    assertEquals("AAPL", result.getTickerSymbol());
    assertEquals("Apple Inc.", result.getTickerName());
    assertEquals(com.alphaflow.persistence.enums.TickerType.STOCK, result.getTickerType());
    assertEquals(com.alphaflow.persistence.enums.Country.US, result.getCountry());
  }

  @Test
  void testGetTickerNotFound() {
    TickerRepository repo = mock(TickerRepository.class);
    when(repo.findByTickerSymbolIgnoreCase("MSFT")).thenReturn(Optional.empty());

    TickerService service = new TickerService(repo);

    assertThrows(ResourceNotFoundException.class, () -> service.getTicker("MSFT"));
  }

  @Test
  void testGetTickerBySymbolSuccess() {
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setTickerName("Apple Inc.");
    ticker.setTickerType(com.alphaflow.persistence.enums.TickerType.STOCK);
    ticker.setCountry(com.alphaflow.persistence.enums.Country.US);
    ticker.setActive(true);

    TickerRepository repo = mock(TickerRepository.class);
    when(repo.findByTickerSymbolIgnoreCase("AAPL")).thenReturn(Optional.of(ticker));

    TickerService service = new TickerService(repo);
    TickerDto dto = service.getTickerBySymbol("AAPL");

    assertEquals("AAPL", dto.tickerSymbol());
    assertEquals("Apple Inc.", dto.tickerName());
    assertEquals(com.alphaflow.persistence.enums.TickerType.STOCK, dto.tickerType());
    assertEquals(com.alphaflow.persistence.enums.Country.US, dto.country());
  }

  @Test
  void testGetTickerBySymbolNotFound() {
    TickerRepository repo = mock(TickerRepository.class);
    when(repo.findByTickerSymbolIgnoreCase("MSFT")).thenReturn(Optional.empty());

    TickerService service = new TickerService(repo);

    assertThrows(ResourceNotFoundException.class, () -> service.getTickerBySymbol("MSFT"));
  }

  @Test
  void testGetAllTickers() {
    Ticker tickerStock = new Ticker();
    tickerStock.setTickerId(1L);
    tickerStock.setTickerSymbol("AAPL");
    tickerStock.setTickerName("Apple Inc.");
    tickerStock.setTickerType(com.alphaflow.persistence.enums.TickerType.STOCK);
    tickerStock.setCountry(com.alphaflow.persistence.enums.Country.US);
    tickerStock.setActive(true);

    Ticker tickerIndex = new Ticker();
    tickerIndex.setTickerId(2L);
    tickerIndex.setTickerSymbol("^NSEI");
    tickerIndex.setTickerName("NIFTY 50");
    tickerIndex.setTickerType(com.alphaflow.persistence.enums.TickerType.INDEX);
    tickerIndex.setCountry(com.alphaflow.persistence.enums.Country.IN);
    tickerIndex.setActive(true);

    TickerRepository repo = mock(TickerRepository.class);
    when(repo.findByIsActiveTrue()).thenReturn(List.of(tickerStock, tickerIndex));

    TickerService service = new TickerService(repo);
    List<TickerDto> list = service.getAllTickers();

    assertEquals(2, list.size());
    assertEquals("AAPL", list.getFirst().tickerSymbol());
    assertEquals(com.alphaflow.persistence.enums.TickerType.STOCK, list.getFirst().tickerType());
    assertEquals(com.alphaflow.persistence.enums.Country.US, list.getFirst().country());
    assertEquals("^NSEI", list.getLast().tickerSymbol());
    assertEquals(com.alphaflow.persistence.enums.TickerType.INDEX, list.getLast().tickerType());
    assertEquals(com.alphaflow.persistence.enums.Country.IN, list.getLast().country());
  }
}
