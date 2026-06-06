package com.alphaflow.engine.downloaders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import java.net.URL;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class YahooFinanceDownloaderTest {

  @Test
  void testDownloadUpToDateNoAction() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new java.util.LinkedHashMap<>();
    latestDates.put(ticker, LocalDate.now().plusDays(2));
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceDownloader downloader =
        new YahooFinanceDownloader(config, dailyRepo, new YahooResponseParser());
    downloader.downloadDailyPrices();

    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadValidParseAndSave() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    URL jsonUrl = getClass().getResource("/yahoo_response.json");
    assertNotNull(jsonUrl);
    config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
    config.setDelayMilliseconds(10); // Throttle is run

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);

    Ticker t1 = new Ticker();
    t1.setTickerId(1L);
    t1.setTickerSymbol("AAPL");
    t1.setActive(true);
    Ticker t2 = new Ticker();
    t2.setTickerId(2L);
    t2.setTickerSymbol("MSFT");
    t2.setActive(true);

    Map<Ticker, LocalDate> latestDates = new java.util.LinkedHashMap<>();
    latestDates.put(t1, LocalDate.of(2025, 8, 12));
    latestDates.put(t2, LocalDate.of(2025, 8, 12));
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceDownloader downloader =
        new YahooFinanceDownloader(config, dailyRepo, new YahooResponseParser());
    downloader.downloadDailyPrices();

    // Verifies both AAPL and MSFT processed, and saveAll was called
    verify(dailyRepo, times(2)).saveAll(any());
  }

  @Test
  void testDownloadConnectionExceptionHandled() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    // Invalid protocol triggers connection error
    config.setDownloadUrl("invalidproto://foo?symbol={symbol}&start={start}&end={end}");

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new java.util.LinkedHashMap<>();
    latestDates.put(ticker, LocalDate.of(2025, 8, 12));
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceDownloader downloader =
        new YahooFinanceDownloader(config, dailyRepo, new YahooResponseParser());
    downloader.downloadDailyPrices();

    // Catches and logs the connection exception, does not save anything or crash the run
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testInterruptedDuringThrottle() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    URL jsonUrl = getClass().getResource("/yahoo_response.json");
    assertNotNull(jsonUrl);
    config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
    config.setDelayMilliseconds(2000L); // Large delay to intercept

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);

    Ticker t1 = new Ticker();
    t1.setTickerId(1L);
    t1.setTickerSymbol("AAPL");
    t1.setActive(true);
    Ticker t2 = new Ticker();
    t2.setTickerId(2L);
    t2.setTickerSymbol("MSFT");
    t2.setActive(true);

    Map<Ticker, LocalDate> latestDates = new java.util.LinkedHashMap<>();
    latestDates.put(t1, LocalDate.of(2025, 8, 12));
    latestDates.put(t2, LocalDate.of(2025, 8, 12));
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceDownloader downloader =
        new YahooFinanceDownloader(config, dailyRepo, new YahooResponseParser());

    // Interrupt thread in background
    Thread mainThread = Thread.currentThread();
    new Thread(
            () -> {
              try {
                Thread.sleep(200);
              } catch (InterruptedException ignored) {
              }
              mainThread.interrupt();
            })
        .start();

    downloader.downloadDailyPrices();

    // First was processed, second was skipped because the pause was interrupted and returned false
    verify(dailyRepo, times(1)).saveAll(any());
  }

  @Test
  void testDownloadMergeDuplicateLatestDates() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    URL jsonUrl = getClass().getResource("/yahoo_response.json");
    assertNotNull(jsonUrl);
    config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
    config.setDelayMilliseconds(0);
    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new java.util.LinkedHashMap<>();
    latestDates.put(ticker, LocalDate.of(2025, 8, 12));
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceDownloader downloader =
        new YahooFinanceDownloader(config, dailyRepo, new YahooResponseParser());
    downloader.downloadDailyPrices();
    // Verify it processes normally and saves since the dates don't exist yet
    verify(dailyRepo, times(1)).saveAll(any());
  }

  @Test
  void testDownloadNullLatestSavedDate() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    URL jsonUrl = getClass().getResource("/yahoo_response.json");
    assertNotNull(jsonUrl);
    config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
    config.setDelayMilliseconds(0);

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new java.util.LinkedHashMap<>();
    latestDates.put(ticker, null); // Returns empty -> latestSavedDate is null
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceDownloader downloader =
        new YahooFinanceDownloader(config, dailyRepo, new YahooResponseParser());
    downloader.downloadDailyPrices();

    verify(dailyRepo, times(1)).saveAll(any());
  }

  @Test
  void testDownloadAllDownloadedPointsAlreadyExist() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    URL jsonUrl = getClass().getResource("/yahoo_response.json");
    assertNotNull(jsonUrl);
    config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
    config.setDelayMilliseconds(0);

    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);

    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("AAPL");
    ticker.setActive(true);

    Map<Ticker, LocalDate> latestDates = new java.util.LinkedHashMap<>();
    latestDates.put(ticker, LocalDate.of(2026, 5, 29));
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(latestDates);

    YahooFinanceDownloader downloader =
        new YahooFinanceDownloader(config, dailyRepo, new YahooResponseParser());
    downloader.downloadDailyPrices();

    // saveAll should never be called since all points exist (latest saved date is 2026-05-29)
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadNoActiveTickers() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);

    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(Map.of());

    YahooFinanceDownloader downloader =
        new YahooFinanceDownloader(config, dailyRepo, new YahooResponseParser());
    downloader.downloadDailyPrices();

    verify(dailyRepo, never()).saveAll(any());
  }
}
