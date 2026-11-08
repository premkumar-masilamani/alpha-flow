package com.alphaflow.engine.schedulers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.DailyPriceFromIntradayCalculator;
import com.alphaflow.engine.calculators.WeeklyPriceCalculator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("removal")
class MarketCloseSchedulerTest {

  @Mock private JobLauncher jobLauncher;
  @Mock private Job technicalAnalysisPipelineJob;
  @Mock private DailyPriceFromIntradayCalculator dailyPriceFromIntradayCalculator;
  @Mock private WeeklyPriceCalculator weeklyPriceCalculator;

  private MarketCloseScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler =
        new MarketCloseScheduler(
            jobLauncher,
            technicalAnalysisPipelineJob,
            dailyPriceFromIntradayCalculator,
            weeklyPriceCalculator);
  }

  @Test
  void testRunIndiaMarketCloseExecutesFullPipelineInSequence() throws Exception {
    scheduler.runIndiaMarketClose();

    InOrder inOrder =
        inOrder(
            dailyPriceFromIntradayCalculator,
            weeklyPriceCalculator,
            jobLauncher);

    inOrder.verify(dailyPriceFromIntradayCalculator).computeDailyPricesFromIntraday();
    inOrder.verify(jobLauncher).run(eq(technicalAnalysisPipelineJob), any(JobParameters.class));
    inOrder.verify(weeklyPriceCalculator).computeWeeklyPrices();
    inOrder.verify(jobLauncher).run(eq(technicalAnalysisPipelineJob), any(JobParameters.class));

    ArgumentCaptor<JobParameters> paramsCaptor = ArgumentCaptor.forClass(JobParameters.class);
    verify(jobLauncher, times(2)).run(eq(technicalAnalysisPipelineJob), paramsCaptor.capture());

    assertEquals(
        Timeframe.DAILY.name(), paramsCaptor.getAllValues().get(0).getString("timeframe"));
    assertEquals(
        Timeframe.WEEKLY.name(), paramsCaptor.getAllValues().get(1).getString("timeframe"));
    assertFalse(scheduler.isRunning());
  }

  @Test
  void testRunUsMarketCloseExecutesWeeklyCalculationsAndPipeline() throws Exception {
    scheduler.runUsMarketClose();

    InOrder inOrder = inOrder(weeklyPriceCalculator, jobLauncher);
    inOrder.verify(weeklyPriceCalculator).computeWeeklyPrices();
    inOrder.verify(jobLauncher).run(eq(technicalAnalysisPipelineJob), any(JobParameters.class));

    verify(dailyPriceFromIntradayCalculator, never()).computeDailyPricesFromIntraday();

    ArgumentCaptor<JobParameters> paramsCaptor = ArgumentCaptor.forClass(JobParameters.class);
    verify(jobLauncher, times(1)).run(eq(technicalAnalysisPipelineJob), paramsCaptor.capture());
    assertEquals(
        Timeframe.WEEKLY.name(), paramsCaptor.getValue().getString("timeframe"));
    assertFalse(scheduler.isRunning());
  }

  @Test
  void testRunOnStartupExecutesFullDailyAndWeeklyPipelines() throws Exception {
    scheduler.runOnStartup();

    verify(dailyPriceFromIntradayCalculator).computeDailyPricesFromIntraday();
    verify(weeklyPriceCalculator).computeWeeklyPrices();
    verify(jobLauncher, times(2)).run(eq(technicalAnalysisPipelineJob), any(JobParameters.class));
    assertFalse(scheduler.isRunning());
  }

  @Test
  void testConcurrentExecutionIsSkippedWhenAlreadyRunning() throws Exception {
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(1);

    doAnswer(
            invocation -> {
              startLatch.countDown();
              finishLatch.await(5, TimeUnit.SECONDS);
              return null;
            })
        .when(dailyPriceFromIntradayCalculator)
        .computeDailyPricesFromIntraday();

    Thread updateThread = new Thread(scheduler::runIndiaMarketClose);
    updateThread.start();

    startLatch.await(2, TimeUnit.SECONDS);

    // Second execution attempt while first is in progress
    scheduler.runOnStartup();

    finishLatch.countDown();
    updateThread.join(2000);

    // Only 1 execution sequence should have run
    verify(dailyPriceFromIntradayCalculator, times(1)).computeDailyPricesFromIntraday();
    assertFalse(scheduler.isRunning());
  }

  @Test
  void testRunWeeklyPipelineSkippedWhenAlreadyRunning() throws Exception {
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(1);

    doAnswer(
            invocation -> {
              startLatch.countDown();
              finishLatch.await(5, TimeUnit.SECONDS);
              return null;
            })
        .when(weeklyPriceCalculator)
        .computeWeeklyPrices();

    Thread updateThread = new Thread(scheduler::runUsMarketClose);
    updateThread.start();

    startLatch.await(2, TimeUnit.SECONDS);

    // Concurrent execution of weekly pipeline
    scheduler.runUsMarketClose();

    finishLatch.countDown();
    updateThread.join(2000);

    verify(weeklyPriceCalculator, times(1)).computeWeeklyPrices();
    assertFalse(scheduler.isRunning());
  }

  @Test
  void testRunWeeklyPipelineHandlesExceptionGracefully() {
    doThrow(new RuntimeException("Simulated failure in weekly calculator"))
        .when(weeklyPriceCalculator)
        .computeWeeklyPrices();

    scheduler.runUsMarketClose();

    assertFalse(scheduler.isRunning());
  }

  @Test
  void testErrorDuringRunResetsRunningFlagAndLogsGracefully() {
    doThrow(new RuntimeException("Simulated failure in calculator"))
        .when(dailyPriceFromIntradayCalculator)
        .computeDailyPricesFromIntraday();

    scheduler.runIndiaMarketClose();

    assertFalse(scheduler.isRunning());
    verify(weeklyPriceCalculator, never()).computeWeeklyPrices();
  }

  @Test
  void testErrorDuringTriggerTechnicalAnalysisPipelineLogsGracefully() throws Exception {
    doThrow(new RuntimeException("Simulated jobLauncher failure"))
        .when(jobLauncher)
        .run(eq(technicalAnalysisPipelineJob), any(JobParameters.class));

    scheduler.runIndiaMarketClose();

    assertFalse(scheduler.isRunning());
  }
}
