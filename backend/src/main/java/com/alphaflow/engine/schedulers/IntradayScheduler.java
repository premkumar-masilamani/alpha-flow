package com.alphaflow.engine.schedulers;

import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.persistence.enums.Country;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled coordinator for intraday price syncing, live quote polling, and intraday trading
 * strategy execution.
 */
@Component
@Slf4j
@SuppressWarnings("removal")
public class IntradayScheduler {

  private final AngelOneConfig angelOneConfig;
  private final JobLauncher jobLauncher;
  private final Job intradayCandlePipelineJob;
  private final Job liveQuotePipelineJob;
  private final AtomicBoolean candleSyncRunning = new AtomicBoolean(false);
  private final AtomicBoolean liveQuoteRunning = new AtomicBoolean(false);

  public IntradayScheduler(
      AngelOneConfig angelOneConfig,
      JobLauncher jobLauncher,
      @Qualifier("intradayCandlePipelineJob") Job intradayCandlePipelineJob,
      @Qualifier("liveQuotePipelineJob") Job liveQuotePipelineJob) {
    this.angelOneConfig = angelOneConfig;
    this.jobLauncher = jobLauncher;
    this.intradayCandlePipelineJob = intradayCandlePipelineJob;
    this.liveQuotePipelineJob = liveQuotePipelineJob;
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

  /**
   * Launches the configured Spring Batch 15-minute intraday candle sync pipeline job.
   */
  public void syncIntradayCandles() {
    if (candleSyncRunning.compareAndSet(false, true)) {
      long start = System.currentTimeMillis();
      try {
        log.info("Syncing Angel One 15-minute intraday candles...");
        JobParameters jobParameters =
            new JobParametersBuilder()
                .addLong("runTimestamp", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(intradayCandlePipelineJob, jobParameters);
        log.info(
            "Angel One 15-minute candle sync completed in {}.",
            SchedulerUtils.formatDuration(System.currentTimeMillis() - start));
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
   * Launches the configured Spring Batch 1-minute live quote extraction and strategy evaluation
   * pipeline job when the Indian market session is open.
   */
  public void runLiveQuoteUpdate() {
    runLiveQuoteUpdate(ZonedDateTime.now(MarketConfig.IST_ZONE));
  }

  /**
   * Package-private overload for testing with an explicit market timestamp.
   */
  void runLiveQuoteUpdate(ZonedDateTime currentMarketTime) {
    if (MarketTradingCalendar.isMarketSessionOpen(
        currentMarketTime,
        angelOneConfig.getSessionStartTime(),
        angelOneConfig.getSessionEndTime(),
        Country.IN)) {
      if (liveQuoteRunning.compareAndSet(false, true)) {
        log.info("Starting scheduled Angel One 1-minute live quote update...");
        long cycleStart = System.currentTimeMillis();
        try {
          JobParameters jobParameters =
              new JobParametersBuilder()
                  .addLong("runTimestamp", System.currentTimeMillis())
                  .toJobParameters();
          jobLauncher.run(liveQuotePipelineJob, jobParameters);
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
          "Skipping live quote update: Indian market session is closed at {}.", currentMarketTime);
    }
  }
}
