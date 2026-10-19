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
    config.setDelayMilliseconds(10);

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
    config.setDelayMilliseconds(1);

    YahooFinanceClient client = new YahooFinanceClient(config);

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);

    List<DailyPrice> prices = client.fetchDailyPrices(ticker, 1000L, 2000L);
    assertTrue(prices.isEmpty());
  }

  @Test
  void testFetchDailyPricesInterruptedDuringDelay() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    config.setDownloadUrl(baseUrl + "/valid?symbol={symbol}&start={start}&end={end}");
    config.setDelayMilliseconds(2000L);

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);

    final YahooFinanceClient client = new YahooFinanceClient(config);

    Thread mainThread = Thread.currentThread();
    new Thread(
            () -> {
              try {
                Thread.sleep(100);
              } catch (InterruptedException ignored) {
                // expected
              }
              mainThread.interrupt();
            })
        .start();

    List<DailyPrice> prices = client.fetchDailyPrices(ticker, 1000L, 2000L);
    assertNotNull(prices);
    assertEquals(2, prices.size());
  }

  @Test
  void testConstructorWithRateLimiter() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    YahooFinanceRateLimiter rateLimiter = new YahooFinanceRateLimiter(config);
    YahooFinanceClient client = new YahooFinanceClient(config, rateLimiter);
    assertNotNull(client);
  }

  @Test
  void testConstructorWithCustomRestClientBuilder() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    YahooFinanceRateLimiter rateLimiter = new YahooFinanceRateLimiter(config);
    YahooFinanceClient client =
        new YahooFinanceClient(config, rateLimiter, RestClient.builder());
    assertNotNull(client);
  }
}
