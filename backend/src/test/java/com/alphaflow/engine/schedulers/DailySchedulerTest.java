package com.alphaflow.engine.schedulers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

@SuppressWarnings("removal")
class DailySchedulerTest {

  @Test
  void testScheduledUpdateLaunchesDailyPipelineJob() throws Exception {
    JobLauncher jobLauncher = mock(JobLauncher.class);
    Job dailyPipelineJob = mock(Job.class);

    DailyScheduler scheduler = new DailyScheduler(jobLauncher, dailyPipelineJob);
    scheduler.runScheduledUpdate();

    verify(jobLauncher, times(1)).run(eq(dailyPipelineJob), any(JobParameters.class));
  }

  @Test
  void testRunOnStartupHandlesJobLauncherException() throws Exception {
    JobLauncher jobLauncher = mock(JobLauncher.class);
    Job dailyPipelineJob = mock(Job.class);

    doThrow(new RuntimeException("Simulated JobLauncher Failure"))
        .when(jobLauncher)
        .run(eq(dailyPipelineJob), any(JobParameters.class));

    DailyScheduler scheduler = new DailyScheduler(jobLauncher, dailyPipelineJob);
    scheduler.runOnStartup();

    verify(jobLauncher, times(1)).run(eq(dailyPipelineJob), any(JobParameters.class));
  }

  @Test
  void testConcurrentExecutionSkipped() throws Exception {
    JobLauncher jobLauncher = mock(JobLauncher.class);
    Job dailyPipelineJob = mock(Job.class);

    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(1);

    doAnswer(
            invocation -> {
              startLatch.countDown();
              finishLatch.await(5, TimeUnit.SECONDS);
              return null;
            })
        .when(jobLauncher)
        .run(eq(dailyPipelineJob), any(JobParameters.class));

    DailyScheduler scheduler = new DailyScheduler(jobLauncher, dailyPipelineJob);

    Thread updateThread = new Thread(scheduler::runScheduledUpdate);
    updateThread.start();

    startLatch.await(2, TimeUnit.SECONDS);

    scheduler.runOnStartup();

    finishLatch.countDown();
    updateThread.join(2000);

    verify(jobLauncher, times(1)).run(eq(dailyPipelineJob), any(JobParameters.class));
  }
}
