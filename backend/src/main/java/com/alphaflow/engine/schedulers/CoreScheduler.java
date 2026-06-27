package com.alphaflow.engine.schedulers;

import com.alphaflow.engine.calculators.IndicatorCalculator;
import com.alphaflow.engine.calculators.WeeklyPriceCalculator;
import com.alphaflow.engine.downloaders.YahooFinanceDownloader;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * CoreScheduler for periodic data updates.
 *
 * <p>This component automates the download of daily price data and the
 *
 * <p>aggregation of weekly price data on a regular basis.
 */
@Component
@Slf4j
public class CoreScheduler {

  private final YahooFinanceDownloader yahooFinanceDownloader;
  private final WeeklyPriceCalculator weeklyPriceCalculator;
  private final IndicatorCalculator indicatorCalculator;
  private final AtomicBoolean running = new AtomicBoolean(false);

  /**
   * Constructs a CoreScheduler with the required data downloaders and calculators.
   *
   * @param yahooFinanceDownloader the Yahoo Finance downloader
   * @param weeklyPriceCalculator the weekly price calculator
   * @param indicatorCalculator the indicator calculator
   */
  public CoreScheduler(
      YahooFinanceDownloader yahooFinanceDownloader,
      WeeklyPriceCalculator weeklyPriceCalculator,
      IndicatorCalculator indicatorCalculator) {
    this.yahooFinanceDownloader = yahooFinanceDownloader;
    this.weeklyPriceCalculator = weeklyPriceCalculator;
    this.indicatorCalculator = indicatorCalculator;
  }

  /** Runs the data update pipeline every hour on the hour. */
  @Scheduled(cron = "0 0 * * * *")
  public void runScheduledUpdate() {
    log.info("Starting scheduled data update cycle (on the hour)...");
    run();
  }

  /**
   * Runs the data update pipeline asynchronously upon application startup,
   *
   * <p>so it does not block the application's main thread.
   */
  @Async
  @EventListener(ApplicationReadyEvent.class)
  public void runOnStartup() {
    log.info("Starting initial data update cycle upon startup...");
    run();
  }

  private void run() {
    if (!running.compareAndSet(false, true)) {
      log.warn("Data update cycle skipped: a previous run is still in progress.");
      return;
    }

    long cycleStart = System.currentTimeMillis();
    try {
      log.info("Step 1/3: Downloading Yahoo Finance daily data...");
      long start = System.currentTimeMillis();
      yahooFinanceDownloader.downloadDailyPrices();
      log.info("Step 1/3 completed in {}.", formatDuration(System.currentTimeMillis() - start));

      log.info("Step 2/3: Computing weekly candles...");
      start = System.currentTimeMillis();
      weeklyPriceCalculator.computeWeeklyPrices();
      log.info("Step 2/3 completed in {}.", formatDuration(System.currentTimeMillis() - start));

      log.info("Step 3/3: Computing indicators...");
      start = System.currentTimeMillis();
      indicatorCalculator.computeIndicators();
      log.info("Step 3/3 completed in {}.", formatDuration(System.currentTimeMillis() - start));

      log.info(
          "Scheduled data update cycle completed successfully in {}.",
          formatDuration(System.currentTimeMillis() - cycleStart));
    } catch (Exception e) {
      log.error(
          "Error occurred during scheduled data update cycle after {}",
          formatDuration(System.currentTimeMillis() - cycleStart),
          e);
    } finally {
      running.set(false);
    }
  }

  private String formatDuration(long ms) {
    long minutes = ms / 60000;
    long seconds = (ms % 60000) / 1000;
    return String.format("%d m %d s (%d ms)", minutes, seconds, ms);
  }
}
