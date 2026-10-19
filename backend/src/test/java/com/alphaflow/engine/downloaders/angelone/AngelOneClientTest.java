package com.alphaflow.engine.downloaders.angelone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.common.enums.IntradayTimeframe;
import com.alphaflow.engine.strategies.options.enums.OptionType;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneCandle;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneOptionGreek;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneQuote;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class AngelOneClientTest {

  private HttpServer server;
  private AngelOneConfig config;
  private AngelOneAuthManager authManager;
  private ObjectMapper objectMapper;
  private AngelOneClient client;

  @BeforeEach
  void setUp() throws Exception {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.start();

    int port = server.getAddress().getPort();
    config = new AngelOneConfig();
    config.setCandleUrl(
        "http://localhost:" + port + "/rest/secure/angelbroking/historical/v1/getCandleData");
    config.setQuoteUrl("http://localhost:" + port + "/rest/secure/angelbroking/market/v1/quote");
    config.setOptionGreeksUrl(
        "http://localhost:" + port + "/rest/secure/angelbroking/marketData/v1/optionGreek");
    config.setApiKey("test-key");
    config.setDelayMilliseconds(0);

    authManager = mock(AngelOneAuthManager.class);
    when(authManager.getValidJwtToken()).thenReturn("mock-bearer-token");

    objectMapper = new ObjectMapper();
    AngelOneNetworkHelper networkHelper = new AngelOneNetworkHelper();
    AngelOneRateLimiter rateLimiter = new AngelOneRateLimiter(config);
    client =
        new AngelOneClient(
            config, authManager, objectMapper, networkHelper, rateLimiter, RestClient.builder());
  }

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void testGetCandleDataSuccess() {
    String jsonResponse =
        """
        {
          "status": true,
          "message": "SUCCESS",
          "errorcode": "",
          "data": [
            ["2026-09-23T09:15:00+05:30", 25000.5, 25050.0, 24980.25, 25040.0, 15000],
            ["2026-09-23 09:30:00", 25040.0, 25080.0, 25020.0, 25070.0, 0]
          ]
        }
        """;

    server.createContext(
        "/rest/secure/angelbroking/historical/v1/getCandleData",
        exchange -> {
          assertEquals(
              "Bearer mock-bearer-token", exchange.getRequestHeaders().getFirst("Authorization"));
          assertEquals("test-key", exchange.getRequestHeaders().getFirst("X-PrivateKey"));
          assertNotNull(exchange.getRequestHeaders().getFirst("X-MACaddress"));
          assertNotNull(exchange.getRequestHeaders().getFirst("X-ClientLocalIP"));
          assertNotNull(exchange.getRequestHeaders().getFirst("X-ClientPublicIP"));

          byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });

    List<AngelOneCandle> candles =
        client.getCandleData(
            MarketConfig.EXCHANGE_NSE,
            "99926000",
            IntradayTimeframe._15M,
            "2026-09-23 09:15",
            "2026-09-23 15:30");

    assertNotNull(candles);
    assertEquals(2, candles.size());
    assertEquals(new BigDecimal("25000.5"), candles.getFirst().open());
    assertEquals(new BigDecimal("25040.0"), candles.getFirst().close());
    assertEquals(new BigDecimal("15000"), candles.getFirst().volume());
    assertEquals(new BigDecimal("25070.0"), candles.get(1).close());
    assertEquals(BigDecimal.ZERO, candles.get(1).volume());
  }

  @Test
  void testGetCandleDataNoJwtToken() {
    when(authManager.getValidJwtToken()).thenReturn(null);
    List<AngelOneCandle> candles =
        client.getCandleData(
            MarketConfig.EXCHANGE_NSE,
            "99926000",
            IntradayTimeframe._15M,
            "2026-09-23 09:15",
            "2026-09-23 15:30");
    assertTrue(candles.isEmpty());
  }

  @Test
  void testGetCandleDataStatusFalseTokenError() {
    String failJson =
        """
        {
          "status": false,
          "message": "Invalid Token",
          "errorcode": "AG8001",
          "data": null
        }
        """;

    server.createContext(
        "/rest/secure/angelbroking/historical/v1/getCandleData",
        exchange -> {
          byte[] bytes = failJson.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });

    List<AngelOneCandle> candles =
        client.getCandleData(
            MarketConfig.EXCHANGE_NSE,
            "99926000",
            IntradayTimeframe._15M,
            "2026-09-23 09:15",
            "2026-09-23 15:30");

    assertTrue(candles.isEmpty());
    verify(authManager, atLeastOnce()).invalidateSession();
  }

  @Test
  void testGetMarketQuoteSuccess() {
    String quoteJson =
        """
        {
          "status": true,
          "message": "SUCCESS",
          "errorcode": "",
          "data": {
            "fetched": [
              {
                "exchange": "NSE",
                "tradingSymbol": "NIFTY",
                "symbolToken": "99926000",
                "ltp": 25150.25,
                "exchFeedTime": "23-Sep-2026 15:30:00"
              }
            ]
          }
        }
        """;

    server.createContext(
        "/rest/secure/angelbroking/market/v1/quote",
        exchange -> {
          byte[] bytes = quoteJson.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });

    Optional<AngelOneQuote> quoteOpt =
        client.getMarketQuote(MarketConfig.EXCHANGE_NSE, "99926000");

    assertTrue(quoteOpt.isPresent());
    AngelOneQuote q = quoteOpt.get();
    assertEquals(0, new BigDecimal("25150.25").compareTo(q.lastPrice()));
    assertEquals("23-Sep-2026 15:30:00", q.timestamp());
  }

  @Test
  void testGetMarketQuoteNoJwt() {
    when(authManager.getValidJwtToken()).thenReturn("");
    Optional<AngelOneQuote> quoteOpt =
        client.getMarketQuote(MarketConfig.EXCHANGE_NSE, "99926000");
    assertTrue(quoteOpt.isEmpty());
  }

  @Test
  void testGetMarketQuoteErrorResponse() {
    String errorJson =
        """
        {
          "status": false,
          "message": "Session expired",
          "errorcode": "AG8001"
        }
        """;

    server.createContext(
        "/rest/secure/angelbroking/market/v1/quote",
        exchange -> {
          byte[] bytes = errorJson.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });

    Optional<AngelOneQuote> quoteOpt =
        client.getMarketQuote(MarketConfig.EXCHANGE_NSE, "99926000");
    assertTrue(quoteOpt.isEmpty());
    verify(authManager, atLeastOnce()).invalidateSession();
  }

  @Test
  void testResolveToken() {
    assertEquals("99926000", AngelOneClient.resolveToken("NIFTY50"));
    assertEquals("99926000", AngelOneClient.resolveToken("nifty50"));
    assertEquals("99926000", AngelOneClient.resolveToken("NIFTY 50"));
    assertEquals("UNKNOWN", AngelOneClient.resolveToken("UNKNOWN"));
  }

  @Test
  void testMapToAngelOneInterval() {
    assertEquals("FIFTEEN_MINUTE", AngelOneClient.mapToAngelOneInterval(IntradayTimeframe._15M));
    assertEquals("", AngelOneClient.mapToAngelOneInterval(null));
  }

  @Test
  void testGetMarketQuotesSuccess() {
    String jsonResponse =
        """
        {
          "status": true,
          "message": "SUCCESS",
          "errorcode": "",
          "data": {
            "fetched": [
              {
                "exchange": "NFO",
                "symbolToken": "12345",
                "ltp": 620.50
              },
              {
                "exchange": "NFO",
                "symbolToken": "12346",
                "ltp": 710.00
              }
            ]
          }
        }
        """;

    server.createContext(
        "/rest/secure/angelbroking/market/v1/quote/bulk",
        exchange -> {
          byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });
    config.setQuoteUrl(
        "http://localhost:" + server.getAddress().getPort() + "/rest/secure/angelbroking/market/v1/quote/bulk");

    Map<String, BigDecimal> quotes =
        client.getMarketQuotes(MarketConfig.EXCHANGE_SEGMENT_NFO, List.of("12345", "12346"));

    assertEquals(2, quotes.size());
    assertEquals(0, new BigDecimal("620.50").compareTo(quotes.get("12345")));
    assertEquals(0, new BigDecimal("710.00").compareTo(quotes.get("12346")));
  }

  @Test
  void testGetMarketQuotesNullOrEmpty() {
    Map<String, BigDecimal> nullQuotes =
        client.getMarketQuotes(MarketConfig.EXCHANGE_SEGMENT_NFO, null);
    assertTrue(nullQuotes.isEmpty());

    Map<String, BigDecimal> emptyQuotes =
        client.getMarketQuotes(MarketConfig.EXCHANGE_SEGMENT_NFO, List.of());
    assertTrue(emptyQuotes.isEmpty());
  }

  @Test
  void testGetMarketQuotesNoJwt() {
    when(authManager.getValidJwtToken()).thenReturn(null);
    Map<String, BigDecimal> quotes =
        client.getMarketQuotes(MarketConfig.EXCHANGE_SEGMENT_NFO, List.of("12345"));
    assertTrue(quotes.isEmpty());
  }

  @Test
  void testGetMarketQuotesErrorResponseAndInvalidToken() {
    String errorJson =
        """
        {
          "status": false,
          "message": "Invalid token",
          "errorcode": "AG8001"
        }
        """;

    server.createContext(
        "/rest/secure/angelbroking/market/v1/quote/error",
        exchange -> {
          byte[] bytes = errorJson.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });
    config.setQuoteUrl(
        "http://localhost:" + server.getAddress().getPort() + "/rest/secure/angelbroking/market/v1/quote/error");

    Map<String, BigDecimal> quotes =
        client.getMarketQuotes(MarketConfig.EXCHANGE_SEGMENT_NFO, List.of("12345"));
    assertTrue(quotes.isEmpty());
    verify(authManager, atLeastOnce()).invalidateSession();
  }

  @Test
  void testGetOptionGreeksSuccess() {
    String jsonResponse =
        """
        {
          "status": true,
          "message": "SUCCESS",
          "errorcode": "",
          "data": [
            {
              "name": "NIFTY",
              "expiry": "31OCT2024",
              "strikePrice": "25400.000000",
              "optionType": "PE",
              "delta": "-0.720000",
              "gamma": "0.000100",
              "theta": "-8.500000",
              "vega": "1.250000",
              "impliedVolatility": "14.200000",
              "tradeVolume": "150000"
            }
          ]
        }
        """;

    server.createContext(
        "/rest/secure/angelbroking/marketData/v1/optionGreek",
        exchange -> {
          byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });

    List<AngelOneOptionGreek> greeks = client.getOptionGreeks("NIFTY", "31OCT2024");
    assertEquals(1, greeks.size());
    AngelOneOptionGreek greek = greeks.get(0);
    assertEquals(new BigDecimal("25400.000000"), greek.strikePrice());
    assertEquals(OptionType.PUT, greek.optionType());
    assertEquals(new BigDecimal("-0.720000"), greek.delta());
    assertEquals(new BigDecimal("14.200000"), greek.impliedVolatility());
  }

  @Test
  void testGetOptionGreeksNullOrBlankInput() {
    assertTrue(client.getOptionGreeks(null, "31OCT2024").isEmpty());
    assertTrue(client.getOptionGreeks("", "31OCT2024").isEmpty());
    assertTrue(client.getOptionGreeks("NIFTY", null).isEmpty());
    assertTrue(client.getOptionGreeks("NIFTY", "").isEmpty());
  }

  @Test
  void testGetOptionGreeksNoJwt() {
    when(authManager.getValidJwtToken()).thenReturn("");
    List<AngelOneOptionGreek> greeks = client.getOptionGreeks("NIFTY", "31OCT2024");
    assertTrue(greeks.isEmpty());
  }

  @Test
  void testGetOptionGreeksErrorResponseAndInvalidToken() {
    String errorJson =
        """
        {
          "status": false,
          "message": "Token expired",
          "errorcode": "AG8001"
        }
        """;

    server.createContext(
        "/rest/secure/angelbroking/marketData/v1/optionGreek/error",
        exchange -> {
          byte[] bytes = errorJson.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });
    config.setOptionGreeksUrl(
        "http://localhost:" + server.getAddress().getPort() + "/rest/secure/angelbroking/marketData/v1/optionGreek/error");

    List<AngelOneOptionGreek> greeks = client.getOptionGreeks("NIFTY", "31OCT2024");
    assertTrue(greeks.isEmpty());
    verify(authManager, atLeastOnce()).invalidateSession();
  }

  @Test
  void testFetch15mCandlesSuccess() {
    String jsonResponse =
        """
        {
          "status": true,
          "message": "SUCCESS",
          "errorcode": "",
          "data": [
            ["2026-09-23T09:15:00+05:30", 25000.5, 25050.0, 24980.25, 25040.0, 15000]
          ]
        }
        """;

    server.createContext(
        "/rest/secure/angelbroking/historical/v1/getCandleData/fetch15m",
        exchange -> {
          byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });
    config.setCandleUrl(
        "http://localhost:" + server.getAddress().getPort() + "/rest/secure/angelbroking/historical/v1/getCandleData/fetch15m");

    OffsetDateTime start = OffsetDateTime.now(MarketConfig.IST_ZONE).minusDays(10);
    OffsetDateTime end = OffsetDateTime.now(MarketConfig.IST_ZONE);

    List<AngelOneCandle> candles =
        client.fetch15mCandles(MarketConfig.EXCHANGE_NSE, "99926000", start, end);
    assertFalse(candles.isEmpty());
    assertEquals(1, candles.size());
  }

  @Test
  void testFetch15mCandlesMultiChunk() {
    AtomicInteger callCount = new AtomicInteger(0);

    server.createContext(
        "/rest/secure/angelbroking/historical/v1/getCandleData/multichunk",
        exchange -> {
          int call = callCount.incrementAndGet();
          String ts = (call == 1) ? "2026-09-23T09:15:00+05:30" : "2026-09-24T09:15:00+05:30";
          String jsonResponse =
              "{\"status\": true, \"message\": \"SUCCESS\", \"errorcode\": \"\", \"data\": [[\""
                  + ts
                  + "\", 25000.5, 25050.0, 24980.25, 25040.0, 15000]]}";
          byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });
    config.setCandleUrl(
        "http://localhost:" + server.getAddress().getPort() + "/rest/secure/angelbroking/historical/v1/getCandleData/multichunk");

    OffsetDateTime start = OffsetDateTime.now(MarketConfig.IST_ZONE).minusDays(250);
    OffsetDateTime end = OffsetDateTime.now(MarketConfig.IST_ZONE);

    List<AngelOneCandle> candles =
        client.fetch15mCandles(MarketConfig.EXCHANGE_NSE, "99926000", start, end);
    assertEquals(2, candles.size());
  }

  @Test
  void testFetch15mCandlesMultiChunkDeduplicatesOverlappingCandles() {
    String jsonResponse =
        """
        {
          "status": true,
          "message": "SUCCESS",
          "errorcode": "",
          "data": [
            ["2026-09-23T09:15:00+05:30", 25000.5, 25050.0, 24980.25, 25040.0, 15000]
          ]
        }
        """;

    server.createContext(
        "/rest/secure/angelbroking/historical/v1/getCandleData/dedup",
        exchange -> {
          byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });
    config.setCandleUrl(
        "http://localhost:" + server.getAddress().getPort() + "/rest/secure/angelbroking/historical/v1/getCandleData/dedup");

    OffsetDateTime start = OffsetDateTime.now(MarketConfig.IST_ZONE).minusDays(250);
    OffsetDateTime end = OffsetDateTime.now(MarketConfig.IST_ZONE);

    List<AngelOneCandle> candles =
        client.fetch15mCandles(MarketConfig.EXCHANGE_NSE, "99926000", start, end);
    assertEquals(1, candles.size(), "Expected overlapping identical candles across chunks to be deduplicated");
  }

  @Test
  void testFetch15mCandlesMissingParams() {
    assertTrue(client.fetch15mCandles(null, "99926000", OffsetDateTime.now(), OffsetDateTime.now()).isEmpty());
    assertTrue(client.fetch15mCandles("NSE", null, OffsetDateTime.now(), OffsetDateTime.now()).isEmpty());
    assertTrue(client.fetch15mCandles("NSE", "99926000", null, OffsetDateTime.now()).isEmpty());
    assertTrue(client.fetch15mCandles("NSE", "99926000", OffsetDateTime.now(), null).isEmpty());
  }
}
