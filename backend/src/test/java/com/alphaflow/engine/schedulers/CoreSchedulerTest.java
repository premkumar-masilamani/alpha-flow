package com.alphaflow.engine.schedulers;

import static org.mockito.Mockito.*;

import com.alphaflow.engine.calculators.IndicatorCalculator;
import com.alphaflow.engine.calculators.WeeklyPriceCalculator;
import com.alphaflow.engine.downloaders.YahooFinanceDownloader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class CoreSchedulerTest {

  @Test
  void testScheduledUpdateSuccess() {
    YahooFinanceDownloader downloader = mock(YahooFinanceDownloader.class);
    WeeklyPriceCalculator weeklyPriceCalculator = mock(WeeklyPriceCalculator.class);
    IndicatorCalculator indicatorCalculator = mock(IndicatorCalculator.class);

    CoreScheduler scheduler =
        new CoreScheduler(downloader, weeklyPriceCalculator, indicatorCalculator);

    scheduler.runScheduledUpdate();

    verify(downloader, times(1)).downloadDailyPrices();
    verify(weeklyPriceCalculator, times(1)).computeWeeklyPrices();
    verify(indicatorCalculator, times(1)).computeIndicators();
  }

  @Test
  void testRunOnStartupWithException() {
    YahooFinanceDownloader downloader = mock(YahooFinanceDownloader.class);
    WeeklyPriceCalculator weeklyPriceCalculator = mock(WeeklyPriceCalculator.class);
    IndicatorCalculator indicatorCalculator = mock(IndicatorCalculator.class);

    doThrow(new RuntimeException("Simulated Failure")).when(downloader).downloadDailyPrices();

    CoreScheduler scheduler =
        new CoreScheduler(downloader, weeklyPriceCalculator, indicatorCalculator);

    scheduler.runOnStartup();

    verify(downloader, times(1)).downloadDailyPrices();
    // Subsequent steps skipped due to exception
    verify(weeklyPriceCalculator, never()).computeWeeklyPrices();
    verify(indicatorCalculator, never()).computeIndicators();
  }

  @Test
  void testConcurrentExecutionSkipped() throws InterruptedException {
    YahooFinanceDownloader downloader = mock(YahooFinanceDownloader.class);
    WeeklyPriceCalculator weeklyPriceCalculator = mock(WeeklyPriceCalculator.class);
    IndicatorCalculator indicatorCalculator = mock(IndicatorCalculator.class);

    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(1);

    // Block inside the first download call
    doAnswer(
            invocation -> {
              startLatch.countDown();
              finishLatch.await(5, TimeUnit.SECONDS);
              return null;
            })
        .when(downloader)
        .downloadDailyPrices();

    CoreScheduler scheduler =
        new CoreScheduler(downloader, weeklyPriceCalculator, indicatorCalculator);

    // Start thread for first invocation
    Thread t = new Thread(scheduler::runScheduledUpdate);
    t.start();

    // Wait for first invocation to start and block
    startLatch.await(2, TimeUnit.SECONDS);

    // Call scheduler again in main thread — should skip since t is still running
    scheduler.runOnStartup();

    // Release first thread
    finishLatch.countDown();
    t.join(2000);

    // Verify t executed download, but second call skipped it
    verify(downloader, times(1)).downloadDailyPrices();
    verify(weeklyPriceCalculator, times(1)).computeWeeklyPrices();
    verify(indicatorCalculator, times(1)).computeIndicators();
  }
}
