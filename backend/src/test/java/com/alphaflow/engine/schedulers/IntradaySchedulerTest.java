package com.alphaflow.engine.schedulers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.persistence.enums.Country;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

@SuppressWarnings("removal")
class IntradaySchedulerTest {

  private AngelOneConfig config;
  private JobLauncher jobLauncher;
  private Job intradayCandlePipelineJob;
  private Job liveQuotePipelineJob;
  private IntradayScheduler scheduler;

  // A fixed trading session time (Wednesday 10:00 AM IST)
  private final ZonedDateTime marketOpenTime =
      ZonedDateTime.of(LocalDate.of(2026, 10, 14), LocalTime.of(10, 0), MarketConfig.IST_ZONE);

  @BeforeEach
  void setUp() {
    MarketTradingCalendar.clearHolidays();
    config = new AngelOneConfig();
    config.setSessionStartTime(LocalTime.MIN);
    config.setSessionEndTime(LocalTime.MAX);

    jobLauncher = mock(JobLauncher.class);
    intradayCandlePipelineJob = mock(Job.class);
    liveQuotePipelineJob = mock(Job.class);
    scheduler =
        new IntradayScheduler(
            config, jobLauncher, intradayCandlePipelineJob, liveQuotePipelineJob);
  }

  @AfterEach
  void tearDown() {
    MarketTradingCalendar.clearHolidays();
  }

  @Test
  void testRunScheduledCandleSync() throws Exception {
    scheduler.runScheduledCandleSync();

    verify(jobLauncher, times(1)).run(eq(intradayCandlePipelineJob), any(JobParameters.class));
    verify(jobLauncher, never()).run(eq(liveQuotePipelineJob), any(JobParameters.class));
  }

  @Test
  void testRunScheduledLiveQuoteUpdateAndRunOnStartup() throws Exception {
    scheduler.runScheduledLiveQuoteUpdate();
    scheduler.runOnStartup();

    verify(jobLauncher, times(1)).run(eq(intradayCandlePipelineJob), any(JobParameters.class));
  }

  @Test
  void testSyncIntradayCandlesExceptionHandled() throws Exception {
    doThrow(new RuntimeException("Simulated candle sync failure"))
        .when(jobLauncher)
        .run(eq(intradayCandlePipelineJob), any(JobParameters.class));

    scheduler.syncIntradayCandles();

    verify(jobLauncher, times(1)).run(eq(intradayCandlePipelineJob), any(JobParameters.class));
  }

  @Test
  void testSyncIntradayCandlesConcurrentSkipped() throws Exception {
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(1);

    doAnswer(
            invocation -> {
              startLatch.countDown();
              finishLatch.await(5, TimeUnit.SECONDS);
              return null;
            })
        .when(jobLauncher)
        .run(eq(intradayCandlePipelineJob), any(JobParameters.class));

    Thread thread = new Thread(() -> scheduler.syncIntradayCandles());
    thread.start();

    startLatch.await(2, TimeUnit.SECONDS);
    scheduler.syncIntradayCandles();

    finishLatch.countDown();
    thread.join(2000);

    verify(jobLauncher, times(1)).run(eq(intradayCandlePipelineJob), any(JobParameters.class));
  }

  @Test
  void testLiveQuoteUpdateExecutesDuringMarketHours() throws Exception {
    scheduler.runLiveQuoteUpdate(marketOpenTime);

    verify(jobLauncher, never()).run(eq(intradayCandlePipelineJob), any(JobParameters.class));
    verify(jobLauncher, times(1)).run(eq(liveQuotePipelineJob), any(JobParameters.class));
  }

  @Test
  void testMarketSessionClosedSkipsLiveQuoteUpdate() throws Exception {
    MarketTradingCalendar.setHolidays(
        Country.IN, Map.of(marketOpenTime.toLocalDate(), "Test Holiday"));

    scheduler.runLiveQuoteUpdate(marketOpenTime);

    verify(jobLauncher, never()).run(any(), any());
  }

  @Test
  void testRunLiveQuoteUpdateHandlesExceptionAndResetsRunningFlag() throws Exception {
    doThrow(new RuntimeException("Simulated live quote failure"))
        .when(jobLauncher)
        .run(eq(liveQuotePipelineJob), any(JobParameters.class));

    scheduler.runLiveQuoteUpdate(marketOpenTime);

    verify(jobLauncher, times(1)).run(eq(liveQuotePipelineJob), any(JobParameters.class));
  }

  @Test
  void testRunLiveQuoteUpdateConcurrentExecutionSkipped() throws Exception {
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(1);

    doAnswer(
            invocation -> {
              startLatch.countDown();
              finishLatch.await(5, TimeUnit.SECONDS);
              return null;
            })
        .when(jobLauncher)
        .run(eq(liveQuotePipelineJob), any(JobParameters.class));

    Thread backgroundThread = new Thread(() -> scheduler.runLiveQuoteUpdate(marketOpenTime));
    backgroundThread.start();

    startLatch.await(2, TimeUnit.SECONDS);

    scheduler.runLiveQuoteUpdate(marketOpenTime);

    finishLatch.countDown();
    backgroundThread.join(2000);

    verify(jobLauncher, times(1)).run(eq(liveQuotePipelineJob), any(JobParameters.class));
  }
}
