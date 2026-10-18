package com.alphaflow.engine.schedulers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.AngelOneConfig;
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
  void testScheduledUpdateExecutesAllStepsDuringMarketHours() {
    ZonedDateTime marketTime =
        ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, MarketConstants.IST_ZONE);

    when(downloader.downloadIntradayPrices()).thenReturn(2);
    when(downloader.downloadLiveQuotes(any(OffsetDateTime.class))).thenReturn(List.of(liveQuote));

    scheduler.run(marketTime);

    verify(downloader, times(1)).downloadIntradayPrices();
    verify(downloader, times(1)).downloadLiveQuotes(any(OffsetDateTime.class));
    verify(strategy, times(1)).execute(niftyTicker, liveQuote);
  }

  @Test
  void testRunScheduledUpdateAndRunOnStartupDelegateToRun() {
    scheduler.runScheduledUpdate();
    scheduler.runOnStartup();
  }

  @Test
  void testMarketSessionClosedSkipsExecution() {
    // Saturday
    ZonedDateTime weekend =
        ZonedDateTime.of(2026, 10, 17, 10, 0, 0, 0, MarketConstants.IST_ZONE);

    scheduler.run(weekend);

    verify(downloader, never()).downloadIntradayPrices();
    verify(downloader, never()).downloadLiveQuotes(any());
    verify(strategy, never()).execute(any(), any());
  }

  @Test
  void testEmptyLiveQuotesSkipsStrategyExecution() {
    ZonedDateTime marketTime =
        ZonedDateTime.of(2026, 10, 16, 11, 0, 0, 0, MarketConstants.IST_ZONE);

    when(downloader.downloadIntradayPrices()).thenReturn(0);
    when(downloader.downloadLiveQuotes(any(OffsetDateTime.class))).thenReturn(List.of());

    scheduler.run(marketTime);

    verify(downloader, times(1)).downloadIntradayPrices();
    verify(downloader, times(1)).downloadLiveQuotes(any(OffsetDateTime.class));
    verify(strategy, never()).execute(any(), any());
  }

  @Test
  void testRunHandlesExceptionAndResetsRunningFlag() {
    ZonedDateTime marketTime =
        ZonedDateTime.of(2026, 10, 16, 11, 30, 0, 0, MarketConstants.IST_ZONE);

    doThrow(new RuntimeException("Simulated download failure"))
        .when(downloader)
        .downloadIntradayPrices();

    scheduler.run(marketTime);

    verify(downloader, times(1)).downloadIntradayPrices();
    verify(downloader, never()).downloadLiveQuotes(any());
  }

  @Test
  void testConcurrentExecutionSkipped() throws InterruptedException {
    ZonedDateTime marketTime =
        ZonedDateTime.of(2026, 10, 16, 12, 0, 0, 0, MarketConstants.IST_ZONE);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(1);

    doAnswer(
            invocation -> {
              startLatch.countDown();
              finishLatch.await(5, TimeUnit.SECONDS);
              return 0;
            })
        .when(downloader)
        .downloadIntradayPrices();

    Thread backgroundThread = new Thread(() -> scheduler.run(marketTime));
    backgroundThread.start();

    startLatch.await(2, TimeUnit.SECONDS);

    // Second call skips because backgroundThread is still running
    scheduler.run(marketTime);

    finishLatch.countDown();
    backgroundThread.join(2000);

    verify(downloader, times(1)).downloadIntradayPrices();
  }
}
