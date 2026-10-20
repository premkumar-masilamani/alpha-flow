package com.alphaflow.engine.schedulers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.engine.downloaders.AngelOneDownloader;
import com.alphaflow.engine.strategies.SakthiIntradayStrategy;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.enums.DataProvider;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntradaySchedulerTest {

  private AngelOneConfig config;
  private AngelOneDownloader downloader;
  private SakthiIntradayStrategy strategy;
  private IntradayScheduler scheduler;
  private Ticker niftyTicker;
  private LiveQuote liveQuote;

  @BeforeEach
  void setUp() {
    MarketTradingCalendar.clearHolidays();
    config = new AngelOneConfig();
    config.setSessionStartTime(LocalTime.of(9, 15));
    config.setSessionEndTime(LocalTime.of(15, 30));

    downloader = mock(AngelOneDownloader.class);
    strategy = mock(SakthiIntradayStrategy.class);
    scheduler = new IntradayScheduler(config, downloader, strategy);

    niftyTicker =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("NIFTY50")
            .country(Country.IN)
            .dataProvider(DataProvider.ANGEL_ONE)
            .isActive(true)
            .build();

    liveQuote =
        LiveQuote.builder()
            .liveQuoteId(100L)
            .ticker(niftyTicker)
            .lastPrice(new BigDecimal("25000.00"))
            .quoteTime(OffsetDateTime.now())
            .build();
  }

  @AfterEach
  void tearDown() {
    MarketTradingCalendar.clearHolidays();
  }

  @Test
  void testRunScheduledCandleSync() {
    when(downloader.downloadAndPersistIntradayPrices()).thenReturn(2);

    scheduler.runScheduledCandleSync();

    verify(downloader, times(1)).downloadAndPersistIntradayPrices();
    verify(downloader, never()).downloadLiveQuotes(any());
  }

  @Test
  void testRunScheduledLiveQuoteUpdateAndRunOnStartup() {
    scheduler.runScheduledLiveQuoteUpdate();
    scheduler.runOnStartup();
    verify(downloader, times(1)).downloadAndPersistIntradayPrices();
  }

  @Test
  void testSyncIntradayCandlesExceptionHandled() {
    doThrow(new RuntimeException("Simulated candle sync failure"))
        .when(downloader)
        .downloadAndPersistIntradayPrices();

    scheduler.syncIntradayCandles();
    verify(downloader, times(1)).downloadAndPersistIntradayPrices();
  }

  @Test
  void testSyncIntradayCandlesConcurrentSkipped() throws Exception {
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(1);

    doAnswer(
            invocation -> {
              startLatch.countDown();
              finishLatch.await(5, TimeUnit.SECONDS);
              return 0;
            })
        .when(downloader)
        .downloadAndPersistIntradayPrices();

    Thread thread = new Thread(() -> scheduler.syncIntradayCandles());
    thread.start();

    startLatch.await(2, TimeUnit.SECONDS);
    // Second invocation skipped because first is still running
    scheduler.syncIntradayCandles();

    finishLatch.countDown();
    thread.join(2000);

    verify(downloader, times(1)).downloadAndPersistIntradayPrices();
  }

  @Test
  void testLiveQuoteUpdateExecutesDuringMarketHours() {
    ZonedDateTime marketTime =
        ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, MarketConfig.IST_ZONE);

    when(downloader.downloadLiveQuotes(any(OffsetDateTime.class))).thenReturn(List.of(liveQuote));

    scheduler.runLiveQuoteUpdate(marketTime);

    verify(downloader, never()).downloadAndPersistIntradayPrices();
    verify(downloader, times(1)).downloadLiveQuotes(any(OffsetDateTime.class));
    verify(strategy, times(1)).execute(niftyTicker, liveQuote);
  }

  @Test
  void testMarketSessionClosedSkipsLiveQuoteUpdate() {
    // Saturday
    ZonedDateTime weekend =
        ZonedDateTime.of(2026, 10, 17, 10, 0, 0, 0, MarketConfig.IST_ZONE);

    scheduler.runLiveQuoteUpdate(weekend);

    verify(downloader, never()).downloadAndPersistIntradayPrices();
    verify(downloader, never()).downloadLiveQuotes(any());
    verify(strategy, never()).execute(any(), any());
  }

  @Test
  void testEmptyLiveQuotesSkipsStrategyExecution() {
    ZonedDateTime marketTime =
        ZonedDateTime.of(2026, 10, 16, 11, 0, 0, 0, MarketConfig.IST_ZONE);

    when(downloader.downloadLiveQuotes(any(OffsetDateTime.class))).thenReturn(List.of());

    scheduler.runLiveQuoteUpdate(marketTime);

    verify(downloader, times(1)).downloadLiveQuotes(any(OffsetDateTime.class));
    verify(strategy, never()).execute(any(), any());
  }

  @Test
  void testRunLiveQuoteUpdateHandlesExceptionAndResetsRunningFlag() {
    ZonedDateTime marketTime =
        ZonedDateTime.of(2026, 10, 16, 11, 30, 0, 0, MarketConfig.IST_ZONE);

    doThrow(new RuntimeException("Simulated live quote failure"))
        .when(downloader)
        .downloadLiveQuotes(any(OffsetDateTime.class));

    scheduler.runLiveQuoteUpdate(marketTime);

    verify(downloader, times(1)).downloadLiveQuotes(any(OffsetDateTime.class));
    verify(strategy, never()).execute(any(), any());
  }

  @Test
  void testRunLiveQuoteUpdateConcurrentExecutionSkipped() throws InterruptedException {
    ZonedDateTime marketTime =
        ZonedDateTime.of(2026, 10, 16, 12, 0, 0, 0, MarketConfig.IST_ZONE);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(1);

    doAnswer(
            invocation -> {
              startLatch.countDown();
              finishLatch.await(5, TimeUnit.SECONDS);
              return List.of();
            })
        .when(downloader)
        .downloadLiveQuotes(any(OffsetDateTime.class));

    Thread backgroundThread = new Thread(() -> scheduler.runLiveQuoteUpdate(marketTime));
    backgroundThread.start();

    startLatch.await(2, TimeUnit.SECONDS);

    // Second call skips because backgroundThread is still running
    scheduler.runLiveQuoteUpdate(marketTime);

    finishLatch.countDown();
    backgroundThread.join(2000);

    verify(downloader, times(1)).downloadLiveQuotes(any(OffsetDateTime.class));
  }
}
