package com.alphaflow.engine.schedulers;

import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.engine.configs.PipelineConfig;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DurationFormatUtils;
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

/** Scheduled coordinator for daily price ingestion and downstream technical analysis pipelines. */
@Component
@Slf4j
@SuppressWarnings("removal")
public class DailyScheduler {

  private final JobLauncher jobLauncher;
  private final Job dailyPipelineJob;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public DailyScheduler(
      JobLauncher jobLauncher, @Qualifier(PipelineConfig.DAILY_PIPELINE_JOB) Job dailyPipelineJob) {
    this.jobLauncher = jobLauncher;
    this.dailyPipelineJob = dailyPipelineJob;
  }

  @Scheduled(cron = "0 0 19 * * MON-FRI", zone = MarketConfig.TIMEZONE_NEW_YORK)
  public void runScheduledUpdate() {
    log.info("Starting scheduled daily data update cycle (3 hours after NYSE close)...");
    run();
  }

  @Async
  @EventListener(ApplicationReadyEvent.class)
  public void runOnStartup() {
    log.info("Starting initial daily data update cycle upon startup...");
    run();
  }

  /** Launches the configured Spring Batch daily and weekly technical analysis pipeline job. */
  public void run() {
    if (running.compareAndSet(false, true)) {
      long cycleStart = System.currentTimeMillis();
      try {
        JobParameters jobParameters =
            new JobParametersBuilder()
                .addLong("runTimestamp", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(dailyPipelineJob, jobParameters);
        log.info(
            "Scheduled daily data update cycle completed successfully in {}.",
            DurationFormatUtils.formatDurationWords(
                System.currentTimeMillis() - cycleStart, true, false));
      } catch (Exception exception) {
        log.error(
            "Error occurred during scheduled daily data update cycle after {}",
            DurationFormatUtils.formatDurationWords(
                System.currentTimeMillis() - cycleStart, true, false),
            exception);
      } finally {
        running.set(false);
      }
    } else {
      log.warn("Daily data update cycle skipped: a previous run is still in progress.");
    }
  }
}
