package com.alphaflow.engine.schedulers;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.DailyPriceFromIntradayCalculator;
import com.alphaflow.engine.calculators.WeeklyPriceCalculator;
import com.alphaflow.engine.configs.MarketConfig;
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
 * Scheduled coordinator executing 1 hour after market close for India (16:30 IST) and the US
 * (17:00 America/New_York), as well as upon application startup.
 */
@Component
@Slf4j
@SuppressWarnings("removal")
public class MarketCloseScheduler {

  private final JobLauncher jobLauncher;
  private final Job technicalAnalysisPipelineJob;
  private final DailyPriceFromIntradayCalculator dailyPriceFromIntradayCalculator;
  private final WeeklyPriceCalculator weeklyPriceCalculator;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public MarketCloseScheduler(
      JobLauncher jobLauncher,
      @Qualifier("technicalAnalysisPipelineJob") Job technicalAnalysisPipelineJob,
      DailyPriceFromIntradayCalculator dailyPriceFromIntradayCalculator,
      WeeklyPriceCalculator weeklyPriceCalculator) {
    this.jobLauncher = jobLauncher;
    this.technicalAnalysisPipelineJob = technicalAnalysisPipelineJob;
    this.dailyPriceFromIntradayCalculator = dailyPriceFromIntradayCalculator;
    this.weeklyPriceCalculator = weeklyPriceCalculator;
  }

  /**
   * Executes 1 hour after Indian market close (NSE closes at 15:30 IST; scheduled at 16:30 IST).
   */
  @Scheduled(cron = "0 30 16 * * MON-FRI", zone = MarketConfig.TIMEZONE_KOLKATA)
  public void runIndiaMarketClose() {
    log.info("Starting post-market close cycle for India (16:30 IST)...");
    runDailyAndWeeklyPipelines("India Market Close");
  }

  /**
   * Executes 1 hour after US market close (NYSE closes at 16:00 America/New_York; scheduled at 17:00).
   */
  @Scheduled(cron = "0 0 17 * * MON-FRI", zone = MarketConfig.TIMEZONE_NEW_YORK)
  public void runUsMarketClose() {
    log.info("Starting post-market close cycle for US (17:00 America/New_York)...");
    runWeeklyPipeline("US Market Close");
  }

  /**
   * Executes initial aggregation and technical analysis pipelines upon application startup.
   */
  @Async
  @EventListener(ApplicationReadyEvent.class)
  public void runOnStartup() {
    log.info("Starting initial post-market aggregation and technical analysis cycles upon startup...");
    runDailyAndWeeklyPipelines("Startup");
  }

  /**
   * Executes the full pipeline sequence:
   * 1. Calculate daily prices from 15-minute intraday prices.
   * 2. Trigger technical analysis pipeline with DAILY timeframe.
   * 3. Calculate weekly prices.
   * 4. Trigger technical analysis pipeline with WEEKLY timeframe.
   *
   * @param triggerContext context name for logging
   */
  public void runDailyAndWeeklyPipelines(String triggerContext) {
    if (running.compareAndSet(false, true)) {
      long cycleStart = System.currentTimeMillis();
      try {
        log.info("[{}] Step 1/4: Aggregating 15-minute intraday prices to daily prices...", triggerContext);
        dailyPriceFromIntradayCalculator.computeDailyPricesFromIntraday();

        log.info("[{}] Step 2/4: Triggering technical analysis pipeline for DAILY timeframe...", triggerContext);
        triggerTechnicalAnalysisPipeline(Timeframe.DAILY);

        log.info("[{}] Step 3/4: Calculating weekly prices...", triggerContext);
        weeklyPriceCalculator.computeWeeklyPrices();

        log.info("[{}] Step 4/4: Triggering technical analysis pipeline for WEEKLY timeframe...", triggerContext);
        triggerTechnicalAnalysisPipeline(Timeframe.WEEKLY);

        log.info(
            "[{}] Post-market close cycle completed successfully in {}.",
            triggerContext,
            SchedulerUtils.formatDuration(System.currentTimeMillis() - cycleStart));
      } catch (Exception exception) {
        log.error(
            "[{}] Error occurred during post-market close cycle after {}",
            triggerContext,
            SchedulerUtils.formatDuration(System.currentTimeMillis() - cycleStart),
            exception);
      } finally {
        running.set(false);
      }
    } else {
      log.warn("[{}] Post-market close cycle skipped: a previous run is still in progress.", triggerContext);
    }
  }

  /**
   * Executes weekly price calculation and the technical analysis pipeline for WEEKLY timeframe.
   *
   * @param triggerContext context name for logging
   */
  public void runWeeklyPipeline(String triggerContext) {
    if (running.compareAndSet(false, true)) {
      long cycleStart = System.currentTimeMillis();
      try {
        log.info("[{}] Calculating weekly prices...", triggerContext);
        weeklyPriceCalculator.computeWeeklyPrices();

        log.info("[{}] Triggering technical analysis pipeline for WEEKLY timeframe...", triggerContext);
        triggerTechnicalAnalysisPipeline(Timeframe.WEEKLY);

        log.info(
            "[{}] Weekly post-market cycle completed successfully in {}.",
            triggerContext,
            SchedulerUtils.formatDuration(System.currentTimeMillis() - cycleStart));
      } catch (Exception exception) {
        log.error(
            "[{}] Error occurred during weekly post-market cycle after {}",
            triggerContext,
            SchedulerUtils.formatDuration(System.currentTimeMillis() - cycleStart),
            exception);
      } finally {
        running.set(false);
      }
    } else {
      log.warn("[{}] Weekly post-market cycle skipped: a previous run is still in progress.", triggerContext);
    }
  }

  /**
   * Triggers the technical analysis Spring Batch pipeline for a specific timeframe.
   *
   * @param timeframe the timeframe to process
   */
  public void triggerTechnicalAnalysisPipeline(Timeframe timeframe) {
    try {
      JobParameters jobParameters =
          new JobParametersBuilder()
              .addLong("runTimestamp", System.currentTimeMillis())
              .addString("timeframe", timeframe.name())
              .toJobParameters();
      jobLauncher.run(technicalAnalysisPipelineJob, jobParameters);
    } catch (Exception exception) {
      log.error(
          "Failed to execute technical analysis pipeline for timeframe {}: {}",
          timeframe,
          exception.getMessage(),
          exception);
    }
  }

  public boolean isRunning() {
    return running.get();
  }
}
