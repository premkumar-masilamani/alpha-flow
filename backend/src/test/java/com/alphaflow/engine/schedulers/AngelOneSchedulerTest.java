package com.alphaflow.engine.schedulers;

import static org.mockito.Mockito.*;

import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.AngelOneDownloader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AngelOneSchedulerTest {

  private AngelOneConfig config;
  private AngelOneDownloader downloader;
  private AngelOneScheduler scheduler;

  @BeforeEach
  void setUp() {
    config = new AngelOneConfig();
    downloader = mock(AngelOneDownloader.class);
    scheduler = new AngelOneScheduler(config, downloader);
  }

  @Test
  void testScheduledUpdateSuccessWhenEnabled() {
    config.setEnabled(true);
    scheduler.runScheduledIntradayUpdate();
    verify(downloader, times(1)).downloadIntradayPrices();
  }

  @Test
  void testScheduledUpdateSkippedWhenDisabled() {
    config.setEnabled(false);
    scheduler.runScheduledIntradayUpdate();
    verify(downloader, never()).downloadIntradayPrices();
  }

  @Test
  void testRunOnStartupWhenDisabled() {
    config.setEnabled(false);
    scheduler.runOnStartup();
    verify(downloader, never()).downloadIntradayPrices();
  }

  @Test
  void testRunOnStartupWhenEnabled() {
    config.setEnabled(true);
    scheduler.runOnStartup();
    verify(downloader, times(1)).downloadIntradayPrices();
  }

  @Test
  void testRunCatchesException() {
    config.setEnabled(true);
    doThrow(new RuntimeException("Simulated exception")).when(downloader).downloadIntradayPrices();
    scheduler.runScheduledIntradayUpdate();
    verify(downloader, times(1)).downloadIntradayPrices();
  }

  @Test
  void testConcurrentExecutionSkipped() throws InterruptedException {
    config.setEnabled(true);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(1);

    doAnswer(
            invocation -> {
              startLatch.countDown();
              finishLatch.await(5, TimeUnit.SECONDS);
              return null;
            })
        .when(downloader)
        .downloadIntradayPrices();

    Thread thread = new Thread(scheduler::runScheduledIntradayUpdate);
    thread.start();

    startLatch.await(2, TimeUnit.SECONDS);

    // Second call while thread is running
    scheduler.runScheduledIntradayUpdate();

    finishLatch.countDown();
    thread.join(2000);

    verify(downloader, times(1)).downloadIntradayPrices();
  }
}
