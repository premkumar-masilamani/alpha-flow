package com.alphaflow.engine.schedulers;

import static org.mockito.Mockito.*;

import com.alphaflow.engine.calculators.IndicatorCalculator;
import com.alphaflow.engine.calculators.SupportResistanceCalculator;
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
    SupportResistanceCalculator supportResistanceCalculator = mock(SupportResistanceCalculator.class);

    CoreScheduler scheduler =
        new CoreScheduler(downloader, weeklyPriceCalculator, indicatorCalculator, supportResistanceCalculator);

    scheduler.runScheduledUpdate();

    verify(downloader, times(1)).downloadDailyPrices();
    verify(weeklyPriceCalculator, times(1)).computeWeeklyPrices();
    verify(indicatorCalculator, times(1)).computeIndicators();
    verify(supportResistanceCalculator, times(1)).computeAll();
  }

  @Test
  void testRunOnStartupWithException() {
    YahooFinanceDownloader downloader = mock(YahooFinanceDownloader.class);
    WeeklyPriceCalculator weeklyPriceCalculator = mock(WeeklyPriceCalculator.class);
    IndicatorCalculator indicatorCalculator = mock(IndicatorCalculator.class);
    SupportResistanceCalculator supportResistanceCalculator = mock(SupportResistanceCalculator.class);

    doThrow(new RuntimeException("Simulated Failure"))
        .when(downloader)
        .downloadDailyPrices();

    CoreScheduler scheduler =
        new CoreScheduler(downloader, weeklyPriceCalculator, indicatorCalculator, supportResistanceCalculator);

    scheduler.runOnStartup();

    verify(downloader, times(1)).downloadDailyPrices();
    // Subsequent steps skipped due to exception
    verify(weeklyPriceCalculator, never()).computeWeeklyPrices();
    verify(indicatorCalculator, never()).computeIndicators();
    verify(supportResistanceCalculator, never()).computeAll();
  }

  @Test
  void testConcurrentExecutionSkipped() throws InterruptedException {
    YahooFinanceDownloader downloader = mock(YahooFinanceDownloader.class);
    WeeklyPriceCalculator weeklyPriceCalculator = mock(WeeklyPriceCalculator.class);
    IndicatorCalculator indicatorCalculator = mock(IndicatorCalculator.class);
    SupportResistanceCalculator supportResistanceCalculator = mock(SupportResistanceCalculator.class);

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

    CoreScheduler scheduler = new CoreScheduler(downloader, weeklyPriceCalculator, indicatorCalculator, supportResistanceCalculator);

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
    verify(supportResistanceCalculator, times(1)).computeAll();
  }
}
