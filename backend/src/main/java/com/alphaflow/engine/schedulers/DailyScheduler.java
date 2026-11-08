package com.alphaflow.engine.schedulers;

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
 * Scheduled coordinator for daily price ingestion and downstream technical analysis pipelines.
 */
@Component
@Slf4j
@SuppressWarnings("removal")
public class DailyScheduler {

  private final JobLauncher jobLauncher;
  private final Job dailyPipelineJob;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public DailyScheduler(
      JobLauncher jobLauncher, @Qualifier("dailyPipelineJob") Job dailyPipelineJob) {
    this.jobLauncher = jobLauncher;
    this.dailyPipelineJob = dailyPipelineJob;
  }

  @Scheduled(cron = "0 0 * * * *", zone = MarketConfig.TIMEZONE_KOLKATA)
  public void runScheduledUpdate() {
    log.info("Starting scheduled daily data update cycle (on the hour)...");
    run();
  }

  @Async
  @EventListener(ApplicationReadyEvent.class)
  public void runOnStartup() {
    log.info("Starting initial daily data update cycle upon startup...");
    run();
  }

  /**
   * Launches the configured Spring Batch daily and weekly technical analysis pipeline job.
   */
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
            SchedulerUtils.formatDuration(System.currentTimeMillis() - cycleStart));
      } catch (Exception exception) {
        log.error(
            "Error occurred during scheduled daily data update cycle after {}",
            SchedulerUtils.formatDuration(System.currentTimeMillis() - cycleStart),
            exception);
      } finally {
        running.set(false);
      }
    } else {
      log.warn("Daily data update cycle skipped: a previous run is still in progress.");
    }
  }
}
