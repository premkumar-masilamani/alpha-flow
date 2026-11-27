package com.alphaflow.engine.downloaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.downloaders.yahoofinance.YahooResponseParser;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class YahooResponseParserTest {

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
    assertEquals(LocalDate.of(2026, 5, 28), first.getPriceDate());
    assertNotNull(first.getPriceOpen());
    assertNotNull(first.getPriceHigh());
    assertNotNull(first.getPriceLow());
    assertNotNull(first.getPriceClose());
    assertNotNull(first.getVolume());
  }

  @Test
  void testParsePreservesUtcDateForUsAndInTickers() throws IOException {
    String json;
    try (var is = getClass().getResourceAsStream("/yahoo_response.json")) {
      json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    Ticker usTicker = new Ticker();
    usTicker.setTickerId(1L);
    usTicker.setTickerSymbol("AAPL");
    usTicker.setCountry(Country.US);

    Ticker inTicker = new Ticker();
    inTicker.setTickerId(2L);
    inTicker.setTickerSymbol("RELIANCE.NS");
    inTicker.setCountry(Country.IN);

    List<DailyPrice> usResult = YahooResponseParser.parse(json, usTicker);
    List<DailyPrice> inResult = YahooResponseParser.parse(json, inTicker);

    assertEquals(2, usResult.size());
    assertEquals(2, inResult.size());
    // Both US and IN tickers preserve the identical UTC date from the Unix epoch timestamp
    assertEquals(LocalDate.of(2026, 5, 28), usResult.getFirst().getPriceDate());
    assertEquals(LocalDate.of(2026, 5, 28), inResult.getFirst().getPriceDate());
    assertEquals(LocalDate.of(2026, 5, 29), usResult.get(1).getPriceDate());
    assertEquals(LocalDate.of(2026, 5, 29), inResult.get(1).getPriceDate());
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

  @Test
  void testParseMissingChartOrResult() throws IOException {
    Ticker ticker = new Ticker();
    assertTrue(YahooResponseParser.parse("null", ticker).isEmpty());
    assertTrue(YahooResponseParser.parse("{\"chart\": null}", ticker).isEmpty());
    assertTrue(YahooResponseParser.parse("{\"chart\": {\"result\": null}}", ticker).isEmpty());
    assertTrue(YahooResponseParser.parse("{\"chart\": {\"result\": []}}", ticker).isEmpty());

    try (var is = getClass().getResourceAsStream("/yahoo_empty_response.json")) {
      String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(YahooResponseParser.parse(json, ticker).isEmpty());
    }

    try (var is = getClass().getResourceAsStream("/yahoo_response_result_null.json")) {
      String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(YahooResponseParser.parse(json, ticker).isEmpty());
    }
  }

  @Test
  void testParseMissingTimestampsOrQuote() throws IOException {
    Ticker ticker = new Ticker();
    assertTrue(
        YahooResponseParser.parse("{\"chart\": {\"result\": [{\"timestamp\": null}]}}", ticker)
            .isEmpty());
    assertTrue(
        YahooResponseParser.parse(
                "{\"chart\": {\"result\": [{\"timestamp\": [1780000000], \"indicators\": null}]}}",
                ticker)
            .isEmpty());
    assertTrue(
        YahooResponseParser.parse(
                "{\"chart\": {\"result\": [{\"timestamp\": [1780000000], \"indicators\": {\"quote\": null}}]}}",
                ticker)
            .isEmpty());
    assertTrue(
        YahooResponseParser.parse(
                "{\"chart\": {\"result\": [{\"timestamp\": [1780000000], \"indicators\": {\"quote\": []}}]}}",
                ticker)
            .isEmpty());

    try (var is = getClass().getResourceAsStream("/yahoo_response_missing.json")) {
      String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(YahooResponseParser.parse(json, ticker).isEmpty());
    }

    try (var is = getClass().getResourceAsStream("/yahoo_response_missing_timestamps.json")) {
      String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(YahooResponseParser.parse(json, ticker).isEmpty());
    }
  }

  @Test
  void testParseMissingQuoteFields() throws IOException {
    Ticker ticker = new Ticker();
    String base =
        "{\"chart\": {\"result\": [{\"timestamp\": [1780000000], \"indicators\": {\"quote\": [%s]}}]}}";

    assertTrue(
        YahooResponseParser.parse(
                String.format(
                    base,
                    "{\"open\": null, \"high\": [1], \"low\": [1], \"close\": [1], \"volume\": [1]}"),
                ticker)
            .isEmpty());
    assertTrue(
        YahooResponseParser.parse(
                String.format(
                    base,
                    "{\"open\": [1], \"high\": null, \"low\": [1], \"close\": [1], \"volume\": [1]}"),
                ticker)
            .isEmpty());
    assertTrue(
        YahooResponseParser.parse(
                String.format(
                    base,
                    "{\"open\": [1], \"high\": [1], \"low\": null, \"close\": [1], \"volume\": [1]}"),
                ticker)
            .isEmpty());
    assertTrue(
        YahooResponseParser.parse(
                String.format(
                    base,
                    "{\"open\": [1], \"high\": [1], \"low\": [1], \"close\": null, \"volume\": [1]}"),
                ticker)
            .isEmpty());
    assertTrue(
        YahooResponseParser.parse(
                String.format(
                    base,
                    "{\"open\": [1], \"high\": [1], \"low\": [1], \"close\": [1], \"volume\": null}"),
                ticker)
            .isEmpty());
  }

  @Test
  void testParseNullAndInvalidBars() throws IOException {
    Ticker ticker = new Ticker();
    try (var is = getClass().getResourceAsStream("/yahoo_response_nulls.json")) {
      String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      List<DailyPrice> list = YahooResponseParser.parse(json, ticker);
      assertNotNull(list);
    }

    String invalidBarsJson =
        """
        {
          "chart": {
            "result": [
              {
                "timestamp": [1780000000, 1780086400, 1780172800, 1780259200, 1780345600],
                "indicators": {
                  "quote": [
                    {
                      "open": [-1, 10, 10, 10, 10],
                      "high": [10, -1, 10, 10, 10],
                      "low": [10, 10, -1, 10, 10],
                      "close": [10, 10, 10, -1, 10],
                      "volume": [100, 100, 100, 100, 100]
                    }
                  ]
                }
              }
            ]
          }
        }
        """;
    List<DailyPrice> list = YahooResponseParser.parse(invalidBarsJson, ticker);
    assertEquals(1, list.size());
  }

  @Test
  void testParseSameDayDuplicateKeepsLaterValidEntry() throws IOException {
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);

    long day1Ts = Instant.parse("2026-11-05T14:30:00Z").getEpochSecond();
    long day2FirstTs = Instant.parse("2026-11-06T14:30:00Z").getEpochSecond();
    long day2SecondTs = Instant.parse("2026-11-06T20:00:00Z").getEpochSecond();

    String duplicateJson =
        """
        {
          "chart": {
            "result": [
              {
                "timestamp": [{ts1}, {ts2}, {ts3}],
                "indicators": {
                  "quote": [
                    {
                      "open": [148.0, 150.0, 150.5],
                      "high": [151.0, 152.0, 154.0],
                      "low": [147.0, 149.0, 149.5],
                      "close": [150.0, 151.0, 153.5],
                      "volume": [900000, 1000000, 1250000]
                    }
                  ]
                }
              }
            ]
          }
        }
        """
            .replace("{ts1}", String.valueOf(day1Ts))
            .replace("{ts2}", String.valueOf(day2FirstTs))
            .replace("{ts3}", String.valueOf(day2SecondTs));

    List<DailyPrice> result = YahooResponseParser.parse(duplicateJson, ticker);
    assertEquals(2, result.size());
    assertEquals(LocalDate.of(2026, 11, 5), result.getFirst().getPriceDate());

    DailyPrice dedupedDay2 = result.get(1);
    assertEquals(LocalDate.of(2026, 11, 6), dedupedDay2.getPriceDate());
    assertEquals(0, new BigDecimal("150.5").compareTo(dedupedDay2.getPriceOpen()));
    assertEquals(0, new BigDecimal("154.0").compareTo(dedupedDay2.getPriceHigh()));
    assertEquals(0, new BigDecimal("149.5").compareTo(dedupedDay2.getPriceLow()));
    assertEquals(0, new BigDecimal("153.5").compareTo(dedupedDay2.getPriceClose()));
    assertEquals(0, new BigDecimal("1250000").compareTo(dedupedDay2.getVolume()));
  }

  @Test
  void testParseSameDayDuplicateDiscardsInvalidSecondEntry() throws IOException {
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);

    long firstTs = Instant.parse("2026-11-06T14:30:00Z").getEpochSecond();
    long nullSecondTs = Instant.parse("2026-11-06T20:00:00Z").getEpochSecond();
    long zeroThirdTs = Instant.parse("2026-11-06T21:00:00Z").getEpochSecond();

    String json =
        """
        {
          "chart": {
            "result": [
              {
                "timestamp": [{ts1}, {ts2}, {ts3}],
                "indicators": {
                  "quote": [
                    {
                      "open": [150.0, 151.0, 151.0],
                      "high": [152.0, 153.0, 153.0],
                      "low": [149.0, 150.0, 150.0],
                      "close": [151.5, null, 0],
                      "volume": [1000000, 1100000, 1200000]
                    }
                  ]
                }
              }
            ]
          }
        }
        """
            .replace("{ts1}", String.valueOf(firstTs))
            .replace("{ts2}", String.valueOf(nullSecondTs))
            .replace("{ts3}", String.valueOf(zeroThirdTs));

    List<DailyPrice> result = YahooResponseParser.parse(json, ticker);
    assertEquals(1, result.size());
    DailyPrice preserved = result.getFirst();
    assertEquals(LocalDate.of(2026, 11, 6), preserved.getPriceDate());
    assertEquals(0, new BigDecimal("150.0").compareTo(preserved.getPriceOpen()));
    assertEquals(0, new BigDecimal("152.0").compareTo(preserved.getPriceHigh()));
    assertEquals(0, new BigDecimal("149.0").compareTo(preserved.getPriceLow()));
    assertEquals(0, new BigDecimal("151.5").compareTo(preserved.getPriceClose()));
    assertEquals(0, new BigDecimal("1000000").compareTo(preserved.getVolume()));
  }
}
