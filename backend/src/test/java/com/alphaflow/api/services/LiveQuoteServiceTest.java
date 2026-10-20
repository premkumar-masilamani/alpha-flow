package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.QuoteDto;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.DataProvider;
import com.alphaflow.persistence.repositories.LiveQuoteRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LiveQuoteServiceTest {

  private LiveQuoteRepository liveQuoteRepository;
  private LiveQuoteService liveQuoteService;

  @BeforeEach
  void setUp() {
    liveQuoteRepository = mock(LiveQuoteRepository.class);
    liveQuoteService = new LiveQuoteService(liveQuoteRepository);
  }

  @Test
  void testGetQuoteReturnsPersistedLiveQuoteWhenPresent() {
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("NIFTY50")
            .tickerName("NIFTY 50")
            .dataProvider(DataProvider.ANGEL_ONE)
            .build();

    OffsetDateTime quoteTime =
        OffsetDateTime.of(2026, 9, 23, 15, 30, 0, 0, ZoneOffset.ofHoursMinutes(5, 30));
    LiveQuote liveQuote =
        LiveQuote.builder()
            .ticker(ticker)
            .lastPrice(new BigDecimal("25100.0000"))
            .volume(BigDecimal.ZERO)
            .quoteTime(quoteTime)
            .build();

    when(liveQuoteRepository.findByTicker(ticker)).thenReturn(Optional.of(liveQuote));

    QuoteDto quoteDto = liveQuoteService.getQuote(ticker);

    assertNotNull(quoteDto);
    assertEquals(new BigDecimal("25100.0000"), quoteDto.currentPrice());
    assertEquals(quoteTime.toString(), quoteDto.timestamp());
  }

  @Test
  void testGetQuoteReturnsZeroWhenNotPresentInDatabase() {
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("NIFTY50")
            .tickerName("NIFTY 50")
            .dataProvider(DataProvider.ANGEL_ONE)
            .build();

    when(liveQuoteRepository.findByTicker(ticker)).thenReturn(Optional.empty());

    QuoteDto quoteDto = liveQuoteService.getQuote(ticker);

    assertNotNull(quoteDto);
    assertEquals(BigDecimal.ZERO, quoteDto.currentPrice());
    assertEquals("", quoteDto.timestamp());
  }
}
