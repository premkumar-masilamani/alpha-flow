package com.alphaflow.engine.downloaders.yahoofinance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class YahooFinanceClientTest {

  private static HttpServer server;
  private static String baseUrl;

  @BeforeAll
  static void startServer() throws Exception {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    byte[] validJson;
    try (InputStream in =
        YahooFinanceClientTest.class.getResourceAsStream("/yahoo_response.json")) {
      validJson = (in != null) ? in.readAllBytes() : new byte[0];
    }

    server.createContext(
        "/valid",
        exchange -> {
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, validJson.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(validJson);
          }
        });

    byte[] badRequestJson =
        ("{\"chart\":{\"result\":null,\"error\":{\"code\":\"Bad Request\","
                + "\"description\":\"Data doesn't exist for startDate = 1795219200, "
                + "endDate = 1795285800\"}}}")
            .getBytes(StandardCharsets.UTF_8);
    server.createContext(
        "/bad-request",
        exchange -> {
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(400, badRequestJson.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(badRequestJson);
          }
        });

    server.start();
    baseUrl = "http://localhost:" + server.getAddress().getPort();
  }

  @AfterAll
  static void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  private RestClient.Builder redirectingBuilder(String targetBaseAndPath) {
    SimpleClientHttpRequestFactory delegate = new SimpleClientHttpRequestFactory();
    return RestClient.builder()
        .requestFactory((uri, httpMethod) ->
            delegate.createRequest(
                URI.create(targetBaseAndPath + "?" + uri.getRawQuery()), httpMethod));
  }

  @Test
  void testFetchDailyPricesValid() {
    YahooFinanceClient client = new YahooFinanceClient(redirectingBuilder(baseUrl + "/valid"));

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);

    List<DailyPrice> prices = client.fetchDailyPrices(ticker, 1000L, 2000L);
    assertNotNull(prices);
    assertEquals(2, prices.size());
  }

  @Test
  void testFetchDailyPricesNullTicker() {
    YahooFinanceClient client = new YahooFinanceClient(null);

    List<DailyPrice> prices = client.fetchDailyPrices(null, 1000L, 2000L);
    assertTrue(prices.isEmpty());
  }

  @Test
  void testFetchDailyPricesHttpError() {
    YahooFinanceClient client =
        new YahooFinanceClient(redirectingBuilder("http://localhost:1/invalid"));

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);

    List<DailyPrice> prices = client.fetchDailyPrices(ticker, 1000L, 2000L);
    assertTrue(prices.isEmpty());
  }

  @Test
  void testFetchDailyPricesBadRequestReturnsEmpty() {
    YahooFinanceClient client =
        new YahooFinanceClient(redirectingBuilder(baseUrl + "/bad-request"));

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("^NSEI");
    ticker.setCountry(Country.IN);

    List<DailyPrice> prices = client.fetchDailyPrices(ticker, 1795219200L, 1795285800L);
    assertTrue(prices.isEmpty());
  }
}
