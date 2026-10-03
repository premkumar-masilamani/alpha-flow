package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.QuoteDto;
import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.engine.downloaders.angelone.AngelOneClient;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.DataProvider;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuoteServiceTest {

  private AngelOneClient client;
  private QuoteService quoteService;

  @BeforeEach
  void setUp() {
    client = mock(AngelOneClient.class);
    quoteService = new QuoteService(client);
  }

  @Test
  void testGetQuoteAngelOneLiveSuccess() {
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("NIFTY50")
            .tickerName("NIFTY 50")
            .dataProvider(DataProvider.ANGEL_ONE)
            .build();

    AngelOneQuote liveQuote = new AngelOneQuote(new BigDecimal("25100.00"), "2026-09-23 15:30:00");

    when(client.getMarketQuote(MarketConstants.EXCHANGE_NSE, "99926000"))
        .thenReturn(Optional.of(liveQuote));

    QuoteDto dto = quoteService.getQuote(ticker);

    assertNotNull(dto);
    assertEquals(new BigDecimal("25100.00"), dto.currentPrice());
    assertEquals("2026-09-23 15:30:00", dto.timestamp());
  }

  @Test
  void testGetQuoteAngelOneEmptyResponse() {
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("NIFTY50")
            .tickerName("NIFTY 50")
            .dataProvider(DataProvider.ANGEL_ONE)
            .build();

    when(client.getMarketQuote(MarketConstants.EXCHANGE_NSE, "99926000"))
        .thenReturn(Optional.empty());

    QuoteDto dto = quoteService.getQuote(ticker);

    assertNotNull(dto);
    assertEquals(BigDecimal.ZERO, dto.currentPrice());
    assertEquals("", dto.timestamp());
  }

  @Test
  void testGetQuoteYahooFinanceReturnsEmpty() {
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("AAPL")
            .tickerName("Apple Inc.")
            .dataProvider(DataProvider.YAHOO_FINANCE)
            .build();

    QuoteDto dto = quoteService.getQuote(ticker);

    assertNotNull(dto);
    assertEquals(BigDecimal.ZERO, dto.currentPrice());
    assertEquals("", dto.timestamp());
  }
}
