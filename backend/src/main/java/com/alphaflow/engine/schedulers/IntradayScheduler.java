package com.alphaflow.engine.schedulers;

import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.AngelOneDownloader;
import com.alphaflow.engine.strategies.SakthiIntradayStrategy;
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
  private final SakthiIntradayStrategy sakthiIntradayStrategy;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public IntradayScheduler(
      AngelOneConfig angelOneConfig,
      AngelOneDownloader angelOneDownloader,
      SakthiIntradayStrategy sakthiIntradayStrategy) {
    this.angelOneConfig = angelOneConfig;
    this.angelOneDownloader = angelOneDownloader;
    this.sakthiIntradayStrategy = sakthiIntradayStrategy;
  }

  @Scheduled(cron = "0 * 9-15 * * MON-FRI", zone = MarketConstants.TIMEZONE_KOLKATA)
  public void runScheduledUpdate() {
    run();
  }

  @Async
  @EventListener(ApplicationReadyEvent.class)
  public void runOnStartup() {
    log.info("Starting initial Angel One intraday sync upon startup...");
    syncIntradayPricesOnStartup();
    run();
  }

  public void syncIntradayPricesOnStartup() {
    if (running.compareAndSet(false, true)) {
      try {
        log.info("Step 1/1: Syncing Angel One 15-minute intraday candles on startup...");
        long start = System.currentTimeMillis();
        int newlyIngestedCandles = angelOneDownloader.downloadIntradayPrices();
        log.info(
            "Startup intraday sync completed in {}. Newly ingested candles: {}",
            SchedulerUtils.formatDuration(System.currentTimeMillis() - start),
            newlyIngestedCandles);
      } catch (Exception exception) {
        log.error("Error occurred during startup intraday candle sync", exception);
      } finally {
        running.set(false);
      }
    } else {
      log.warn("Startup intraday candle sync skipped: an update is already in progress.");
    }
  }

  public void run() {
    run(ZonedDateTime.now(MarketConstants.IST_ZONE));
  }

  /**
   * Executes the multi-step intraday cycle: 15m candle syncing, live quote extraction, and strategy evaluation.
   *
   * @param currentMarketTime current market timestamp for session validation and quote stamping
   */
  public void run(ZonedDateTime currentMarketTime) {
    if (SchedulerUtils.isMarketSessionOpen(
        currentMarketTime,
        angelOneConfig.getSessionStartTime(),
        angelOneConfig.getSessionEndTime(),
        Country.IN)) {
      if (running.compareAndSet(false, true)) {
        log.info("Starting scheduled Angel One intraday update cycle...");
        long cycleStart = System.currentTimeMillis();
        try {
          log.info("Step 1/3: Syncing Angel One 15-minute intraday candles...");
          long start = System.currentTimeMillis();
          int newlyIngestedCandles = angelOneDownloader.downloadIntradayPrices();
          log.info(
              "Step 1/3 completed in {}. Newly ingested candles: {}",
              SchedulerUtils.formatDuration(System.currentTimeMillis() - start),
              newlyIngestedCandles);

          log.info("Step 2/3: Fetching and persisting live quotes...");
          start = System.currentTimeMillis();
          List<LiveQuote> liveQuotes =
              angelOneDownloader.downloadLiveQuotes(currentMarketTime.toOffsetDateTime());
          log.info(
              "Step 2/3 completed in {}. Persisted quotes: {}",
              SchedulerUtils.formatDuration(System.currentTimeMillis() - start),
              liveQuotes.size());

          log.info("Step 3/3: Evaluating intraday trading strategies...");
          start = System.currentTimeMillis();
          for (LiveQuote liveQuote : liveQuotes) {
            sakthiIntradayStrategy.execute(liveQuote.getTicker(), liveQuote);
          }
          log.info(
              "Step 3/3 completed in {}.",
              SchedulerUtils.formatDuration(System.currentTimeMillis() - start));

          log.info(
              "Scheduled intraday update cycle completed successfully in {}.",
              SchedulerUtils.formatDuration(System.currentTimeMillis() - cycleStart));
        } catch (Exception exception) {
          log.error(
              "Error occurred during scheduled intraday update cycle after {}",
              SchedulerUtils.formatDuration(System.currentTimeMillis() - cycleStart),
              exception);
        } finally {
          running.set(false);
        }
      } else {
        log.warn("Intraday update cycle skipped: a previous run is still in progress.");
      }
    } else {
      log.debug(
          "Skipping intraday update cycle: Indian market session is closed at {}.",
          currentMarketTime);
    }
  }
}
