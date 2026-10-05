package com.alphaflow.engine.downloaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.downloaders.yahoofinance.YahooResponseParser;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class YahooResponseParserTest {

  @Test
  void testConstructorIsPrivate() throws Exception {
    Constructor<YahooResponseParser> constructor =
        YahooResponseParser.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    constructor.setAccessible(true);
    assertNotNull(constructor.newInstance());
  }

  @Test
  void testParseValidJson() throws IOException {
    String json;
    try (var is = getClass().getResourceAsStream("/yahoo_response.json")) {
      json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");

    List<DailyPrice> result = YahooResponseParser.parse(json, ticker);
    assertNotNull(result);
    assertFalse(result.isEmpty());

    // Verify properties of parsed elements
    DailyPrice first = result.getFirst();
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
    assertTrue(YahooResponseParser.parse(null, ticker).isEmpty());
    assertTrue(YahooResponseParser.parse("", ticker).isEmpty());
    assertTrue(YahooResponseParser.parse("   ", ticker).isEmpty());
  }

  @Test
  void testParseMalformedJsonThrowsException() {
    Ticker ticker = new Ticker();
    assertThrows(IOException.class, () -> YahooResponseParser.parse("{malformed: json}", ticker));
  }
}
