package com.alphaflow.engine.downloaders.yahoofinance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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

    byte[] noDataJson =
        ("{\"chart\":{\"result\":null,\"error\":{\"code\":\"Bad Request\","
                + "\"description\":\"Data doesn't exist for startDate = 1795219200, "
                + "endDate = 1795285800\"}}}")
            .getBytes(StandardCharsets.UTF_8);
    server.createContext(
        "/no-data",
        exchange -> {
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(400, noDataJson.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(noDataJson);
          }
        });

    byte[] badRequestJson =
        ("{\"chart\":{\"result\":null,\"error\":{\"code\":\"Bad Request\","
                + "\"description\":\"Invalid symbol\"}}}")
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

  @Test
  void testFetchDailyPricesValid() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    config.setDownloadUrl(baseUrl + "/valid?symbol={symbol}&start={start}&end={end}");

    YahooFinanceClient client = new YahooFinanceClient(config);

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
    YahooFinanceConfig config = new YahooFinanceConfig();
    YahooFinanceClient client = new YahooFinanceClient(config);

    List<DailyPrice> prices = client.fetchDailyPrices(null, 1000L, 2000L);
    assertTrue(prices.isEmpty());
  }

  @Test
  void testFetchDailyPricesHttpError() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    config.setDownloadUrl("http://localhost:1/invalid?symbol={symbol}&start={start}&end={end}");

    YahooFinanceClient client = new YahooFinanceClient(config);

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);

    List<DailyPrice> prices = client.fetchDailyPrices(ticker, 1000L, 2000L);
    assertTrue(prices.isEmpty());
  }

  @Test
  void testFetchDailyPricesHolidayOrWeekendNoDataReturnsEmptyGracefully() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    config.setDownloadUrl(baseUrl + "/no-data?symbol={symbol}&start={start}&end={end}");

    YahooFinanceClient client = new YahooFinanceClient(config);

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("^NSEI");
    ticker.setCountry(Country.IN);

    List<DailyPrice> prices = client.fetchDailyPrices(ticker, 1795219200L, 1795285800L);
    assertTrue(prices.isEmpty());
  }

  @Test
  void testFetchDailyPricesGenericBadRequestReturnsEmpty() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    config.setDownloadUrl(baseUrl + "/bad-request?symbol={symbol}&start={start}&end={end}");

    YahooFinanceClient client = new YahooFinanceClient(config);

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("INVALID");
    ticker.setCountry(Country.US);

    List<DailyPrice> prices = client.fetchDailyPrices(ticker, 1000L, 2000L);
    assertTrue(prices.isEmpty());
  }

  @Test
  void testConstructorWithCustomRestClientBuilder() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    YahooFinanceClient client = new YahooFinanceClient(config, RestClient.builder());
    assertNotNull(client);
  }
}
