package com.alphaflow.engine.downloaders.angelone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneCandle;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneQuote;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
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
    config.setApiKey("test-key");
    config.setDelayMilliseconds(0);

    authManager = mock(AngelOneAuthManager.class);
    when(authManager.getValidJwtToken()).thenReturn("mock-bearer-token");

    objectMapper = new ObjectMapper();
    AngelOneNetworkHelper networkHelper = new AngelOneNetworkHelper();
    client =
        new AngelOneClient(config, authManager, objectMapper, networkHelper, RestClient.builder());
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
            MarketConstants.EXCHANGE_NSE,
            "99926000",
            Timeframe._15M,
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
            MarketConstants.EXCHANGE_NSE,
            "99926000",
            Timeframe._15M,
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
            MarketConstants.EXCHANGE_NSE,
            "99926000",
            Timeframe._15M,
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
        client.getMarketQuote(MarketConstants.EXCHANGE_NSE, "99926000");

    assertTrue(quoteOpt.isPresent());
    AngelOneQuote q = quoteOpt.get();
    assertEquals(0, new BigDecimal("25150.25").compareTo(q.lastPrice()));
    assertEquals("23-Sep-2026 15:30:00", q.timestamp());
  }

  @Test
  void testGetMarketQuoteNoJwt() {
    when(authManager.getValidJwtToken()).thenReturn("");
    Optional<AngelOneQuote> quoteOpt =
        client.getMarketQuote(MarketConstants.EXCHANGE_NSE, "99926000");
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
        client.getMarketQuote(MarketConstants.EXCHANGE_NSE, "99926000");
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
    assertEquals("FIFTEEN_MINUTE", AngelOneClient.mapToAngelOneInterval(Timeframe._15M));
    assertEquals("DAILY", AngelOneClient.mapToAngelOneInterval(Timeframe.DAILY));
    assertEquals("WEEKLY", AngelOneClient.mapToAngelOneInterval(Timeframe.WEEKLY));
  }
}
