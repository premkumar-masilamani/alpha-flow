package com.alphaflow.engine.schedulers;

import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.engine.downloaders.AngelOneDownloader;
import com.alphaflow.engine.strategies.Intraday70PBounceStrategy;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.enums.Country;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled coordinator for intraday price syncing, live quote polling, and intraday trading strategy execution.
 */
@Component
@Slf4j
public class IntradayScheduler {

  private final AngelOneConfig angelOneConfig;
  private final AngelOneDownloader angelOneDownloader;
  private final Intraday70PBounceStrategy intraday70PBounceStrategy;
  private final AtomicBoolean candleSyncRunning = new AtomicBoolean(false);
  private final AtomicBoolean liveQuoteRunning = new AtomicBoolean(false);

  public IntradayScheduler(
      AngelOneConfig angelOneConfig,
      AngelOneDownloader angelOneDownloader,
      Intraday70PBounceStrategy intraday70PBounceStrategy) {
    this.angelOneConfig = angelOneConfig;
    this.angelOneDownloader = angelOneDownloader;
    this.intraday70PBounceStrategy = intraday70PBounceStrategy;
  }

  @Scheduled(cron = "30 0,15,30,45 9-15 * * MON-FRI", zone = MarketConfig.TIMEZONE_KOLKATA)
  public void runScheduledCandleSync() {
    log.info("Starting scheduled Angel One 15-minute intraday candle sync...");
    syncIntradayCandles();
  }

  @Scheduled(cron = "0 * 9-15 * * MON-FRI", zone = MarketConfig.TIMEZONE_KOLKATA)
  public void runScheduledLiveQuoteUpdate() {
    runLiveQuoteUpdate();
  }

  @Async
  @EventListener(ApplicationReadyEvent.class)
  public void runOnStartup() {
    log.info("Starting initial Angel One intraday sync upon startup...");
    syncIntradayCandles();
    runLiveQuoteUpdate();
  }

  public void syncIntradayCandles() {
    if (candleSyncRunning.compareAndSet(false, true)) {
      long start = System.currentTimeMillis();
      try {
        log.info("Syncing Angel One 15-minute intraday candles...");
        int newlyIngestedCandles = angelOneDownloader.downloadAndPersistIntradayPrices();
        log.info(
            "Angel One 15-minute candle sync completed in {}. Newly ingested candles: {}",
            SchedulerUtils.formatDuration(System.currentTimeMillis() - start),
            newlyIngestedCandles);
      } catch (Exception exception) {
        log.error(
            "Error occurred during Angel One 15-minute candle sync after {}",
            SchedulerUtils.formatDuration(System.currentTimeMillis() - start),
            exception);
      } finally {
        candleSyncRunning.set(false);
      }
    } else {
      log.warn("Angel One 15-minute candle sync skipped: a previous run is still in progress.");
    }
  }

  /**
   * Executes the 1-minute live quote extraction and strategy evaluation pipeline.
   */
  public void runLiveQuoteUpdate() {
    ZonedDateTime currentMarketTime = ZonedDateTime.now(MarketConfig.IST_ZONE);
    if (MarketTradingCalendar.isMarketSessionOpen(
        currentMarketTime,
        angelOneConfig.getSessionStartTime(),
        angelOneConfig.getSessionEndTime(),
        Country.IN)) {
      if (liveQuoteRunning.compareAndSet(false, true)) {
        log.info("Starting scheduled Angel One 1-minute live quote update...");
        long cycleStart = System.currentTimeMillis();
        try {
          log.info("Step 1/2: Fetching and persisting live quotes...");
          long start = System.currentTimeMillis();
          List<LiveQuote> liveQuotes =
              angelOneDownloader.downloadLiveQuotes(currentMarketTime.toOffsetDateTime());
          log.info(
              "Step 1/2 completed in {}. Persisted quotes: {}",
              SchedulerUtils.formatDuration(System.currentTimeMillis() - start),
              liveQuotes.size());

          log.info("Step 2/2: Evaluating intraday trading strategies...");
          start = System.currentTimeMillis();
          for (LiveQuote liveQuote : liveQuotes) {
            intraday70PBounceStrategy.execute(liveQuote.getTicker(), liveQuote);
          }
          log.info(
              "Step 2/2 completed in {}.",
              SchedulerUtils.formatDuration(System.currentTimeMillis() - start));

          log.info(
              "Scheduled 1-minute live quote update completed successfully in {}.",
              SchedulerUtils.formatDuration(System.currentTimeMillis() - cycleStart));
        } catch (Exception exception) {
          log.error(
              "Error occurred during scheduled 1-minute live quote update after {}",
              SchedulerUtils.formatDuration(System.currentTimeMillis() - cycleStart),
              exception);
        } finally {
          liveQuoteRunning.set(false);
        }
      } else {
        log.warn("Live quote update skipped: a previous run is still in progress.");
      }
    } else {
      log.debug(
          "Skipping live quote update: Indian market session is closed at {}.",
          currentMarketTime);
    }
  }
}
