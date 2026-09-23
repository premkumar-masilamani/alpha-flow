package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.QuoteDto;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.AngelOneClient;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneQuote;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.DataProvider;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.IntradayPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuoteServiceTest {

  private AngelOneClient client;
  private AngelOneConfig config;
  private IntradayPriceRepository intradayRepo;
  private DailyPriceRepository dailyRepo;
  private QuoteService quoteService;

  @BeforeEach
  void setUp() {
    client = mock(AngelOneClient.class);
    config = new AngelOneConfig();
    intradayRepo = mock(IntradayPriceRepository.class);
    dailyRepo = mock(DailyPriceRepository.class);
    quoteService = new QuoteService(client, config, intradayRepo, dailyRepo);
  }

  @Test
  void testGetQuoteAngelOneLiveSuccess() {
    config.setEnabled(true);
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("NIFTY50")
            .tickerName("NIFTY 50")
            .dataProvider(DataProvider.ANGEL_ONE)
            .instrumentToken("99926000")
            .build();

    AngelOneQuote liveQuote =
        new AngelOneQuote(
            new BigDecimal("25100.00"),
            new BigDecimal("100.00"),
            new BigDecimal("0.4000"),
            new BigDecimal("25000.00"),
            new BigDecimal("25120.00"),
            new BigDecimal("24980.00"),
            new BigDecimal("25000.00"),
            BigDecimal.ZERO,
            "2026-09-23 15:30:00");

    when(client.getMarketQuote("NSE", "99926000")).thenReturn(Optional.of(liveQuote));

    QuoteDto dto = quoteService.getQuote(ticker);

    assertNotNull(dto);
    assertEquals("NIFTY50", dto.symbol());
    assertEquals(new BigDecimal("25100.00"), dto.lastPrice());
    assertEquals("2026-09-23 15:30:00", dto.timestamp());
  }

  @Test
  void testGetQuoteAngelOneFallbackToIntraday() {
    config.setEnabled(false);
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("NIFTY50")
            .tickerName("NIFTY 50")
            .dataProvider(DataProvider.ANGEL_ONE)
            .build();

    OffsetDateTime time = OffsetDateTime.now();
    IntradayPrice ip =
        IntradayPrice.builder()
            .ticker(ticker)
            .priceTime(time)
            .priceOpen(new BigDecimal("25000.0000"))
            .priceHigh(new BigDecimal("25100.0000"))
            .priceLow(new BigDecimal("24950.0000"))
            .priceClose(new BigDecimal("25080.0000"))
            .volume(BigDecimal.ZERO)
            .build();

    when(intradayRepo.findTopByTickerAndTimeframeOrderByPriceTimeDesc(ticker, "FIFTEEN_MINUTE"))
        .thenReturn(Optional.of(ip));

    QuoteDto dto = quoteService.getQuote(ticker);

    assertNotNull(dto);
    assertEquals("NIFTY50", dto.symbol());
    assertEquals(new BigDecimal("25080.0000"), dto.lastPrice());
    assertEquals(new BigDecimal("80.0000"), dto.change());
    assertEquals(time.toString(), dto.timestamp());
  }

  @Test
  void testGetQuoteYahooFinanceSuccess() {
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("AAPL")
            .tickerName("Apple Inc.")
            .dataProvider(DataProvider.YAHOO_FINANCE)
            .build();

    DailyPrice dp =
        DailyPrice.builder()
            .ticker(ticker)
            .priceDate(LocalDate.of(2026, 9, 23))
            .priceOpen(new BigDecimal("220.0000"))
            .priceHigh(new BigDecimal("225.0000"))
            .priceLow(new BigDecimal("219.0000"))
            .priceClose(new BigDecimal("224.0000"))
            .volume(new BigDecimal("5000000.0000"))
            .build();

    when(dailyRepo.findTopByTickerOrderByPriceDateDesc(ticker)).thenReturn(Optional.of(dp));

    QuoteDto dto = quoteService.getQuote(ticker);

    assertNotNull(dto);
    assertEquals("AAPL", dto.symbol());
    assertEquals(new BigDecimal("224.0000"), dto.lastPrice());
    assertEquals(new BigDecimal("4.0000"), dto.change());
    assertEquals("2026-09-23", dto.timestamp());
  }

  @Test
  void testGetQuoteEmptyFallback() {
    Ticker ticker =
        Ticker.builder()
            .tickerSymbol("EMPTY")
            .tickerName("Empty Ticker")
            .dataProvider(DataProvider.YAHOO_FINANCE)
            .build();

    when(dailyRepo.findTopByTickerOrderByPriceDateDesc(ticker)).thenReturn(Optional.empty());

    QuoteDto dto = quoteService.getQuote(ticker);

    assertNotNull(dto);
    assertEquals("EMPTY", dto.symbol());
    assertEquals(BigDecimal.ZERO, dto.lastPrice());
    assertEquals("", dto.timestamp());
  }
}
