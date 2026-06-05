package com.alphaflow.engine.downloaders;

import static org.junit.jupiter.api.Assertions.*;

import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class YahooResponseParserTest {

  private final YahooResponseParser parser = new YahooResponseParser();

  @Test
  void testParseValidJson() throws IOException {
    String json;
    try (var is = getClass().getResourceAsStream("/yahoo_response.json")) {
      json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");

    List<DailyPrice> result = parser.parse(json, ticker);
    assertNotNull(result);
    assertFalse(result.isEmpty());

    // Verify properties of parsed elements
    DailyPrice first = result.get(0);
    assertEquals(ticker, first.getTicker());
    assertNotNull(first.getPriceDate());
    assertNotNull(first.getPriceOpen());
    assertNotNull(first.getPriceHigh());
    assertNotNull(first.getPriceLow());
    assertNotNull(first.getPriceClose());
    assertNotNull(first.getVolume());
  }

  @Test
  void testParseNullOrEmptyJson() throws IOException {
    Ticker ticker = new Ticker();
    assertTrue(parser.parse(null, ticker).isEmpty());
    assertTrue(parser.parse("", ticker).isEmpty());
    assertTrue(parser.parse("   ", ticker).isEmpty());
  }

  @Test
  void testParseMalformedJsonThrowsException() {
    Ticker ticker = new Ticker();
    assertThrows(IOException.class, () -> parser.parse("{malformed: json}", ticker));
  }
}
