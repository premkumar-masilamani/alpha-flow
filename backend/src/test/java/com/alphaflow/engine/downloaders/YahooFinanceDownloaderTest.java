package com.alphaflow.engine.downloaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.engine.calculators.PipelineGating;
import com.alphaflow.engine.downloaders.yahoofinance.YahooFinanceClient;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

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
          byte[] empty =
              "{\"chart\":{\"result\":[{\"timestamp\":[],\"indicators\":{\"quote\":[{}]}}],\"error\":null}}"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, empty.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(empty);
          }
        });

    server.createContext(
        "/special-symbol",
        exchange -> {
          String rawPath = exchange.getRequestURI().getRawPath();
          if (rawPath != null && rawPath.contains("%5EGSPC")) {
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

  private YahooFinanceClient createClient(String targetBaseAndPath, boolean preservePath) {
    SimpleClientHttpRequestFactory delegate = new SimpleClientHttpRequestFactory();
    RestClient.Builder builder =
        RestClient.builder()
            .requestFactory((uri, httpMethod) -> {
              String target =
                  preservePath
                      ? targetBaseAndPath + uri.getRawPath() + "?" + uri.getRawQuery()
                      : targetBaseAndPath + "?" + uri.getRawQuery();
              return delegate.createRequest(URI.create(target), httpMethod);
            });
    return new YahooFinanceClient(builder);
  }

  @Test
  void testDownloadUpToDateNoAction() {
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new LinkedHashMap<>();
    latestDates.put(ticker, LocalDate.now().plusDays(2));

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceClient client = new YahooFinanceClient(null);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(client, dailyRepo);
    int ingestedRows = downloader.downloadAndPersistDailyPrices();

    assertEquals(0, ingestedRows);
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadSavedDateIsTodaySkipsApiCall() {
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);
    ticker.setActive(true);

    LocalDate expectedDate =
        PipelineGating.getLatestExpectedTradingDate(ticker.getCountry(), Instant.now());
    Map<Ticker, LocalDate> latestDates = new LinkedHashMap<>();
    latestDates.put(ticker, expectedDate);

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceClient client = mock(YahooFinanceClient.class);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(client, dailyRepo);
    int ingestedRows = downloader.downloadAndPersistDailyPrices();

    assertEquals(0, ingestedRows);
    verify(client, never()).fetchDailyPrices(any(), any(Long.class), any(Long.class));
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadPartialStalenessOnlyFetchesStaleTickers() {
    Ticker upToDateTicker = new Ticker();
    upToDateTicker.setTickerId(1L);
    upToDateTicker.setTickerSymbol("AAPL");
    upToDateTicker.setCountry(Country.US);
    upToDateTicker.setActive(true);

    Ticker staleTicker = new Ticker();
    staleTicker.setTickerId(2L);
    staleTicker.setTickerSymbol("MSFT");
    staleTicker.setCountry(Country.US);
    staleTicker.setActive(true);

    LocalDate expectedDate =
        PipelineGating.getLatestExpectedTradingDate(upToDateTicker.getCountry(), Instant.now());
    Map<Ticker, LocalDate> latestDates = new LinkedHashMap<>();
    latestDates.put(upToDateTicker, expectedDate);
    latestDates.put(staleTicker, LocalDate.of(2025, 8, 12));

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceClient client = createClient(baseUrl + "/valid", false);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(client, dailyRepo);
    int ingestedRows = downloader.downloadAndPersistDailyPrices();

    assertEquals(2, ingestedRows);
    verify(dailyRepo, times(1)).saveAll(any());
  }

  @Test
  void testDownloadValidParseAndSave() {
    Ticker ticker1 = new Ticker();
    ticker1.setTickerId(1L);
    ticker1.setTickerSymbol("AAPL");
    ticker1.setCountry(Country.US);
    ticker1.setActive(true);
    Ticker ticker2 = new Ticker();
    ticker2.setTickerId(2L);
    ticker2.setTickerSymbol("MSFT");
    ticker2.setCountry(Country.US);
    ticker2.setActive(true);

    Map<Ticker, LocalDate> latestDates = new LinkedHashMap<>();
    latestDates.put(ticker1, LocalDate.of(2025, 8, 12));
    latestDates.put(ticker2, LocalDate.of(2025, 8, 12));

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceClient client = createClient(baseUrl + "/valid", false);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(client, dailyRepo);
    int ingestedRows = downloader.downloadAndPersistDailyPrices();

    assertEquals(4, ingestedRows);
    verify(dailyRepo, times(2)).saveAll(any());
  }

  @Test
  void testDownloadNullLatestSavedDate() {
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new LinkedHashMap<>();
    latestDates.put(ticker, null);

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceClient client = createClient(baseUrl + "/valid", false);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(client, dailyRepo);
    int ingestedRows = downloader.downloadAndPersistDailyPrices();

    assertEquals(2, ingestedRows);
    verify(dailyRepo, times(1)).saveAll(any());
  }

  @Test
  void testDownloadAllDownloadedPointsAlreadyExist() {
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new LinkedHashMap<>();
    latestDates.put(ticker, LocalDate.of(2026, 5, 29));

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceClient client = createClient(baseUrl + "/valid", false);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(client, dailyRepo);
    int ingestedRows = downloader.downloadAndPersistDailyPrices();

    assertEquals(0, ingestedRows);
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadEmptyPriceDataReturned() {
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new LinkedHashMap<>();
    latestDates.put(ticker, LocalDate.of(2025, 8, 12));

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceClient client = createClient(baseUrl + "/empty", false);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(client, dailyRepo);
    int ingestedRows = downloader.downloadAndPersistDailyPrices();

    assertEquals(0, ingestedRows);
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadNoActiveTickers() {
    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(Map.of());

    YahooFinanceClient client = new YahooFinanceClient(null);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(client, dailyRepo);
    int ingestedRows = downloader.downloadAndPersistDailyPrices();

    assertEquals(0, ingestedRows);
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadConnectionExceptionHandled() {
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new LinkedHashMap<>();
    latestDates.put(ticker, LocalDate.of(2025, 8, 12));

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceClient client = createClient("http://localhost:1/invalid", false);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(client, dailyRepo);
    int ingestedRows = downloader.downloadAndPersistDailyPrices();

    assertEquals(0, ingestedRows);
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadSpecialSymbolTickerEncoding() {
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("^GSPC");
    ticker.setCountry(Country.US);
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new LinkedHashMap<>();
    latestDates.put(ticker, LocalDate.of(2025, 8, 12));

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceClient client = createClient(baseUrl + "/special-symbol", true);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(client, dailyRepo);
    int ingestedRows = downloader.downloadAndPersistDailyPrices();

    assertEquals(2, ingestedRows);
    verify(dailyRepo, times(1)).saveAll(any());
  }

  @Test
  void testDownloadInterruptedDuringDelay() {
    Ticker ticker1 = new Ticker();
    ticker1.setTickerId(1L);
    ticker1.setTickerSymbol("AAPL");
    ticker1.setCountry(Country.US);
    ticker1.setActive(true);
    Ticker ticker2 = new Ticker();
    ticker2.setTickerId(2L);
    ticker2.setTickerSymbol("MSFT");
    ticker2.setCountry(Country.US);
    ticker2.setActive(true);

    Map<Ticker, LocalDate> latestDates = new LinkedHashMap<>();
    latestDates.put(ticker1, LocalDate.of(2025, 8, 12));
    latestDates.put(ticker2, LocalDate.of(2025, 8, 12));

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceClient client = createClient(baseUrl + "/valid", false);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(client, dailyRepo);

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

    int ingestedRows = downloader.downloadAndPersistDailyPrices();
    assertTrue(Thread.interrupted());
    assertEquals(4, ingestedRows);
  }

  @Test
  void testPipelineJobMethods() {
    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    YahooFinanceClient client = mock(YahooFinanceClient.class);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(client, dailyRepo);

    assertEquals("DOWNLOAD_DAILY_PRICE", downloader.getJobName());

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);
    ticker.setActive(true);

    LocalDate expectedDate =
        PipelineGating.getLatestExpectedTradingDate(ticker.getCountry(), Instant.now());

    // When up to date: shouldRun returns false
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(Map.of(ticker, expectedDate));
    assertEquals(false, downloader.shouldRun(null));

    // When latestSavedDate is null: shouldRun returns true
    Map<Ticker, LocalDate> nullMap = new LinkedHashMap<>();
    nullMap.put(ticker, null);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(nullMap);
    assertEquals(true, downloader.shouldRun(null));

    // When stale: shouldRun returns true
    when(dailyRepo.findLatestPriceDatesForActiveTickers())
        .thenReturn(Map.of(ticker, expectedDate.minusDays(5)));
    assertEquals(true, downloader.shouldRun(null));

    // Test execute delegating to downloadAndPersistDailyPrices
    downloader.execute(null);

    // Back-to-back idempotency: shouldRun returns false on second call because ticker was already checked
    assertEquals(false, downloader.shouldRun(null));
  }

  @Test
  void testDownloadBackToBackSkipsAlreadyCheckedTickersEvenIfNoNewDataSaved() {
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setCountry(Country.US);
    ticker.setActive(true);

    LocalDate staleDate = LocalDate.of(2025, 8, 12);
    Map<Ticker, LocalDate> latestDates = new LinkedHashMap<>();
    latestDates.put(ticker, staleDate);

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceClient client = mock(YahooFinanceClient.class);
    when(client.fetchDailyPrices(any(), any(Long.class), any(Long.class))).thenReturn(List.of());

    YahooFinanceDownloader downloader = new YahooFinanceDownloader(client, dailyRepo);

    // First run finds ticker stale and queries client
    int firstRunIngested = downloader.downloadAndPersistDailyPrices();
    assertEquals(0, firstRunIngested);
    verify(client, times(1)).fetchDailyPrices(any(), any(Long.class), any(Long.class));

    // Second run back-to-back skips because ticker was already checked for this expected date
    assertEquals(false, downloader.shouldRun(null));
    int secondRunIngested = downloader.downloadAndPersistDailyPrices();
    assertEquals(0, secondRunIngested);
    verify(client, times(1)).fetchDailyPrices(any(), any(Long.class), any(Long.class));
  }
}
