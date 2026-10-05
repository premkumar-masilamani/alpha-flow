package com.alphaflow.engine.downloaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.YahooFinanceConfig;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class YahooFinanceDownloaderTest {

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
    MarketTradingCalendar calendar = new MarketTradingCalendar(config);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo, calendar);
    int ingestedRows = downloader.downloadDailyPrices();

    assertEquals(0, ingestedRows);
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadValidParseAndSave() {
    YahooFinanceConfig config = createConfig();
    URL jsonUrl = getClass().getResource("/yahoo_response.json");
    assertNotNull(jsonUrl);
    config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
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

    MarketTradingCalendar calendar = new MarketTradingCalendar(config);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo, calendar);
    int ingestedRows = downloader.downloadDailyPrices();

    // Verifies both AAPL and MSFT processed, and saveAll was called twice
    assertEquals(4, ingestedRows);
    verify(dailyRepo, times(2)).saveAll(any());
  }

  @Test
  void testDownloadNullLatestSavedDate() {
    YahooFinanceConfig config = createConfig();
    URL jsonUrl = getClass().getResource("/yahoo_response.json");
    assertNotNull(jsonUrl);
    config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
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

    MarketTradingCalendar calendar = new MarketTradingCalendar(config);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo, calendar);
    int ingestedRows = downloader.downloadDailyPrices();

    assertEquals(2, ingestedRows);
    verify(dailyRepo, times(1)).saveAll(any());
  }

  @Test
  void testDownloadAllDownloadedPointsAlreadyExist() {
    YahooFinanceConfig config = createConfig();
    URL jsonUrl = getClass().getResource("/yahoo_response.json");
    assertNotNull(jsonUrl);
    config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
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

    MarketTradingCalendar calendar = new MarketTradingCalendar(config);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo, calendar);
    int ingestedRows = downloader.downloadDailyPrices();

    assertEquals(0, ingestedRows);
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadEmptyPriceDataReturned() {
    YahooFinanceConfig config = createConfig();
    URL jsonUrl = getClass().getResource("/yahoo_empty_response.json");
    assertNotNull(jsonUrl);
    config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
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

    MarketTradingCalendar calendar = new MarketTradingCalendar(config);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo, calendar);
    int ingestedRows = downloader.downloadDailyPrices();

    assertEquals(0, ingestedRows);
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadNoActiveTickers() {
    YahooFinanceConfig config = createConfig();
    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
    when(dailyRepo.findLatestPriceDatesForActiveTickers()).thenReturn(Map.of());

    MarketTradingCalendar calendar = new MarketTradingCalendar(config);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo, calendar);
    int ingestedRows = downloader.downloadDailyPrices();

    assertEquals(0, ingestedRows);
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testDownloadConnectionExceptionHandled() {
    YahooFinanceConfig config = createConfig();
    config.setDownloadUrl("invalidproto://foo?symbol={symbol}&start={start}&end={end}");
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

    MarketTradingCalendar calendar = new MarketTradingCalendar(config);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo, calendar);
    int ingestedRows = downloader.downloadDailyPrices();

    assertEquals(0, ingestedRows);
    verify(dailyRepo, never()).saveAll(any());
  }

  @Test
  void testInterruptedDuringThrottle() {
    YahooFinanceConfig config = createConfig();
    URL jsonUrl = getClass().getResource("/yahoo_response.json");
    assertNotNull(jsonUrl);
    config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
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

    MarketTradingCalendar calendar = new MarketTradingCalendar(config);
    YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, dailyRepo, calendar);

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
}
