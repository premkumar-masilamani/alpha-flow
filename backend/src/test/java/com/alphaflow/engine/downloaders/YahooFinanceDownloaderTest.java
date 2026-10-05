package com.alphaflow.engine.downloaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class YahooFinanceDownloaderTest {

  private static HttpServer server;
  private static String baseUrl;

  @BeforeAll
  static void startServer() throws Exception {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    byte[] validJson;
    try (InputStream in =
        YahooFinanceDownloaderTest.class.getResourceAsStream("/yahoo_response.json")) {
      validJson = (in != null) ? in.readAllBytes() : new byte[0];
    }
    byte[] emptyJson;
    try (InputStream in =
        YahooFinanceDownloaderTest.class.getResourceAsStream("/yahoo_empty_response.json")) {
      emptyJson = (in != null) ? in.readAllBytes() : new byte[0];
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

    server.createContext(
        "/empty",
        exchange -> {
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, emptyJson.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(emptyJson);
          }
        });

    server.createContext(
        "/special-symbol",
        exchange -> {
          String rawQuery = exchange.getRequestURI().getRawQuery();
          if (rawQuery != null && rawQuery.contains("%5EGSPC") && !rawQuery.contains("%255EGSPC")) {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, validJson.length);
            try (OutputStream os = exchange.getResponseBody()) {
              os.write(validJson);
            }
          } else {
            exchange.sendResponseHeaders(404, 0);
            exchange.close();
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

  private YahooFinanceConfig createConfig() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    config.setMarketCutoffTime(LocalTime.of(17, 0));
    return config;
  }

  @Test
  void testDownloadUpToDateNoAction() {
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new java.util.LinkedHashMap<>();
    latestDates.put(ticker, LocalDate.now().plusDays(2));

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceConfig config = createConfig();
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo);
    int ingestedRows = downloader.downloadDailyPrices();

    assertEquals(0, ingestedRows);
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadValidParseAndSave() {
    YahooFinanceConfig config = createConfig();
    config.setDownloadUrl(baseUrl + "/valid?symbol={symbol}&start={start}&end={end}");
    config.setDelayMilliseconds(10);

    Ticker t1 = new Ticker();
    t1.setTickerId(1L);
    t1.setTickerSymbol("AAPL");
    t1.setCountry(Country.US);
    t1.setActive(true);
    Ticker t2 = new Ticker();
    t2.setTickerId(2L);
    t2.setTickerSymbol("MSFT");
    t2.setCountry(Country.US);
    t2.setActive(true);

    Map<Ticker, LocalDate> latestDates = new java.util.LinkedHashMap<>();
    latestDates.put(t1, LocalDate.of(2025, 8, 12));
    latestDates.put(t2, LocalDate.of(2025, 8, 12));

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo);
    int ingestedRows = downloader.downloadDailyPrices();

    // Verifies both AAPL and MSFT processed, and saveAll was called twice
    assertEquals(4, ingestedRows);
    verify(dailyRepo, times(2)).saveAll(any());
  }

  @Test
  void testDownloadNullLatestSavedDate() {
    YahooFinanceConfig config = createConfig();
    config.setDownloadUrl(baseUrl + "/valid?symbol={symbol}&start={start}&end={end}");
    config.setDelayMilliseconds(0);

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new java.util.LinkedHashMap<>();
    latestDates.put(ticker, null);

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo);
    int ingestedRows = downloader.downloadDailyPrices();

    assertEquals(2, ingestedRows);
    verify(dailyRepo, times(1)).saveAll(any());
  }

  @Test
  void testDownloadAllDownloadedPointsAlreadyExist() {
    YahooFinanceConfig config = createConfig();
    config.setDownloadUrl(baseUrl + "/valid?symbol={symbol}&start={start}&end={end}");
    config.setDelayMilliseconds(0);

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new java.util.LinkedHashMap<>();
    latestDates.put(ticker, LocalDate.of(2026, 5, 29));

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo);
    int ingestedRows = downloader.downloadDailyPrices();

    assertEquals(0, ingestedRows);
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadEmptyPriceDataReturned() {
    YahooFinanceConfig config = createConfig();
    config.setDownloadUrl(baseUrl + "/empty?symbol={symbol}&start={start}&end={end}");
    config.setDelayMilliseconds(0);

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new java.util.LinkedHashMap<>();
    latestDates.put(ticker, LocalDate.of(2025, 8, 12));

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo);
    int ingestedRows = downloader.downloadDailyPrices();

    assertEquals(0, ingestedRows);
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadNoActiveTickers() {
    YahooFinanceConfig config = createConfig();
    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(Map.of());

    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo);
    int ingestedRows = downloader.downloadDailyPrices();

    assertEquals(0, ingestedRows);
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadConnectionExceptionHandled() {
    YahooFinanceConfig config = createConfig();
    config.setDownloadUrl("http://localhost:1/invalid?symbol={symbol}&start={start}&end={end}");
    config.setDelayMilliseconds(1);

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new java.util.LinkedHashMap<>();
    latestDates.put(ticker, LocalDate.of(2025, 8, 12));

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo);
    int ingestedRows = downloader.downloadDailyPrices();

    assertEquals(0, ingestedRows);
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testInterruptedDuringThrottle() {
    YahooFinanceConfig config = createConfig();
    config.setDownloadUrl(baseUrl + "/valid?symbol={symbol}&start={start}&end={end}");
    config.setDelayMilliseconds(2000L);

    Ticker t1 = new Ticker();
    t1.setTickerId(1L);
    t1.setTickerSymbol("AAPL");
    t1.setCountry(Country.US);
    t1.setActive(true);
    Ticker t2 = new Ticker();
    t2.setTickerId(2L);
    t2.setTickerSymbol("MSFT");
    t2.setCountry(Country.US);
    t2.setActive(true);

    Map<Ticker, LocalDate> latestDates = new java.util.LinkedHashMap<>();
    latestDates.put(t1, LocalDate.of(2025, 8, 12));
    latestDates.put(t2, LocalDate.of(2025, 8, 12));

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo);

    Thread mainThread = Thread.currentThread();
    new Thread(
            () -> {
              try {
                Thread.sleep(200);
              } catch (InterruptedException ignored) {
                // expected interrupt during sleep
              }
              mainThread.interrupt();
            })
        .start();

    downloader.downloadDailyPrices();

    verify(dailyRepo, times(1)).saveAll(any());
  }

  @Test
  void testDownloadSpecialSymbolTickerEncoding() {
    YahooFinanceConfig config = createConfig();
    config.setDownloadUrl(baseUrl + "/special-symbol?symbol={symbol}&start={start}&end={end}");
    config.setDelayMilliseconds(0);

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("^GSPC");
    ticker.setCountry(Country.US);
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new java.util.LinkedHashMap<>();
    latestDates.put(ticker, LocalDate.of(2025, 8, 12));

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo);
    int ingestedRows = downloader.downloadDailyPrices();

    assertEquals(2, ingestedRows);
    verify(dailyRepo, times(1)).saveAll(any());
  }

  @Test
  void testConstructorWithCustomRestClientBuilder() {
    YahooFinanceConfig config = createConfig();
    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(Map.of());

    YahooFinanceDownloader downloader =
        new YahooFinanceDownloader(
            config, dailyRepo, org.springframework.web.client.RestClient.builder());
    int ingestedRows = downloader.downloadDailyPrices();

    assertEquals(0, ingestedRows);
  }
}
