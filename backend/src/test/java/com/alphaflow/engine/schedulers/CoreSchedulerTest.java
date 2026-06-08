package com.alphaflow.engine.schedulers;

import static org.mockito.Mockito.*;

import com.alphaflow.engine.calculators.IndicatorCalculator;
import com.alphaflow.engine.calculators.WeeklyPriceCalculator;
import com.alphaflow.engine.downloaders.YahooFinanceDownloader;
import com.alphaflow.engine.strategies.ASTAStrategy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class CoreSchedulerTest {

  @Test
  void testScheduledUpdateSuccess() {
    YahooFinanceDownloader downloader = mock(YahooFinanceDownloader.class);

    WeeklyPriceCalculator weeklyCalculator = mock(WeeklyPriceCalculator.class);

    IndicatorCalculator indicatorCalculator = mock(IndicatorCalculator.class);

    ASTAStrategy astaStrategy = mock(ASTAStrategy.class);

    CoreScheduler scheduler =
        new CoreScheduler(downloader, weeklyCalculator, indicatorCalculator, astaStrategy);

    scheduler.runScheduledUpdate();

    verify(downloader, times(1)).downloadDailyPrices();

    verify(weeklyCalculator, times(1)).computeWeeklyPrices();

    verify(indicatorCalculator, times(1)).computeIndicators();

    verify(astaStrategy, times(1)).doTechnicalAnalysis();
  }

  @Test
  void testRunOnStartupWithException() {
    YahooFinanceDownloader downloader = mock(YahooFinanceDownloader.class);

    WeeklyPriceCalculator weeklyCalculator = mock(WeeklyPriceCalculator.class);

    IndicatorCalculator indicatorCalculator = mock(IndicatorCalculator.class);

    ASTAStrategy astaStrategy = mock(ASTAStrategy.class);

    doThrow(new RuntimeException("Injected download error")).when(downloader).downloadDailyPrices();

    CoreScheduler scheduler =
        new CoreScheduler(downloader, weeklyCalculator, indicatorCalculator, astaStrategy);

    scheduler.runOnStartup();

    verify(downloader, times(1)).downloadDailyPrices();

    // Subsequent steps skipped due to exception

    verify(weeklyCalculator, never()).computeWeeklyPrices();

    verify(indicatorCalculator, never()).computeIndicators();

    verify(astaStrategy, never()).doTechnicalAnalysis();
  }

  @Test
  void testConcurrentExecutionSkipped() throws InterruptedException {
    YahooFinanceDownloader downloader = mock(YahooFinanceDownloader.class);

    WeeklyPriceCalculator weeklyCalculator = mock(WeeklyPriceCalculator.class);

    IndicatorCalculator indicatorCalculator = mock(IndicatorCalculator.class);

    ASTAStrategy astaStrategy = mock(ASTAStrategy.class);

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
        new CoreScheduler(downloader, weeklyCalculator, indicatorCalculator, astaStrategy);

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

    verify(weeklyCalculator, times(1)).computeWeeklyPrices();

    verify(indicatorCalculator, times(1)).computeIndicators();

    verify(astaStrategy, times(1)).doTechnicalAnalysis();
  }
}
