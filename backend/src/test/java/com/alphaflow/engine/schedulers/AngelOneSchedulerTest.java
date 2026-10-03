package com.alphaflow.engine.schedulers;

import static org.mockito.Mockito.*;

import com.alphaflow.engine.downloaders.AngelOneDownloader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AngelOneSchedulerTest {

  private AngelOneDownloader downloader;
  private AngelOneScheduler scheduler;

  @BeforeEach
  void setUp() {
    downloader = mock(AngelOneDownloader.class);
    scheduler = new AngelOneScheduler(downloader);
  }

  @Test
  void testScheduledUpdateSuccess() {
    scheduler.runScheduledIntradayUpdate();
    verify(downloader, times(1)).downloadIntradayPrices();
  }

  @Test
  void testRunOnStartup() {
    scheduler.runOnStartup();
    verify(downloader, times(1)).downloadIntradayPrices();
  }

  @Test
  void testRunCatchesException() {
    doThrow(new RuntimeException("Simulated exception")).when(downloader).downloadIntradayPrices();
    scheduler.runScheduledIntradayUpdate();
    verify(downloader, times(1)).downloadIntradayPrices();
  }

  @Test
  void testConcurrentExecutionSkipped() throws InterruptedException {
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
