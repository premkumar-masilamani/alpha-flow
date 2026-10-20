package com.alphaflow.engine.schedulers;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.engine.calculators.CandlestickPatternCalculator;
import com.alphaflow.engine.calculators.ChartPatternCalculator;
import com.alphaflow.engine.calculators.IndicatorCalculator;
import com.alphaflow.engine.calculators.SupportResistanceCalculator;
import com.alphaflow.engine.calculators.WeeklyPriceCalculator;
import com.alphaflow.engine.configs.ChartPatternConfig;
import com.alphaflow.engine.downloaders.YahooFinanceDownloader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class DailySchedulerTest {

  @Test
  void testScheduledUpdateSuccessWithChartPatternsDisabledByDefault() {
    YahooFinanceDownloader downloader = mock(YahooFinanceDownloader.class);
    WeeklyPriceCalculator weeklyPriceCalculator = mock(WeeklyPriceCalculator.class);
    IndicatorCalculator indicatorCalculator = mock(IndicatorCalculator.class);
    SupportResistanceCalculator supportResistanceCalculator =
        mock(SupportResistanceCalculator.class);
    CandlestickPatternCalculator patternCalculator = mock(CandlestickPatternCalculator.class);
    ChartPatternCalculator chartPatternCalculator = mock(ChartPatternCalculator.class);
    ChartPatternConfig chartPatternConfig = new ChartPatternConfig();

    when(downloader.downloadAndPersistDailyPrices()).thenReturn(5);

    DailyScheduler scheduler =
        new DailyScheduler(
            downloader,
            weeklyPriceCalculator,
            indicatorCalculator,
            supportResistanceCalculator,
            patternCalculator,
            chartPatternCalculator,
            chartPatternConfig);

    scheduler.runScheduledUpdate();

    verify(downloader, times(1)).downloadAndPersistDailyPrices();
    verify(weeklyPriceCalculator, times(1)).computeWeeklyPrices();
    verify(indicatorCalculator, times(1)).computeIndicators();
    verify(supportResistanceCalculator, times(1)).computeSupportResistances();
    verify(patternCalculator, times(1)).computeCandleStickPatterns();
    verify(chartPatternCalculator, never()).computeChartPatterns();
  }

  @Test
  void testScheduledUpdateSuccessWithChartPatternsEnabled() {
    YahooFinanceDownloader downloader = mock(YahooFinanceDownloader.class);
    WeeklyPriceCalculator weeklyPriceCalculator = mock(WeeklyPriceCalculator.class);
    IndicatorCalculator indicatorCalculator = mock(IndicatorCalculator.class);
    SupportResistanceCalculator supportResistanceCalculator =
        mock(SupportResistanceCalculator.class);
    CandlestickPatternCalculator patternCalculator = mock(CandlestickPatternCalculator.class);
    ChartPatternCalculator chartPatternCalculator = mock(ChartPatternCalculator.class);
    ChartPatternConfig chartPatternConfig = new ChartPatternConfig();
    chartPatternConfig.setEnabled(true);

    when(downloader.downloadAndPersistDailyPrices()).thenReturn(5);

    DailyScheduler scheduler =
        new DailyScheduler(
            downloader,
            weeklyPriceCalculator,
            indicatorCalculator,
            supportResistanceCalculator,
            patternCalculator,
            chartPatternCalculator,
            chartPatternConfig);

    scheduler.runScheduledUpdate();

    verify(downloader, times(1)).downloadAndPersistDailyPrices();
    verify(weeklyPriceCalculator, times(1)).computeWeeklyPrices();
    verify(indicatorCalculator, times(1)).computeIndicators();
    verify(supportResistanceCalculator, times(1)).computeSupportResistances();
    verify(patternCalculator, times(1)).computeCandleStickPatterns();
    verify(chartPatternCalculator, times(1)).computeChartPatterns();
  }

  @Test
  void testScheduledUpdateZeroNewRowsShortCircuitsDownstreamSteps() {
    YahooFinanceDownloader downloader = mock(YahooFinanceDownloader.class);
    WeeklyPriceCalculator weeklyPriceCalculator = mock(WeeklyPriceCalculator.class);
    IndicatorCalculator indicatorCalculator = mock(IndicatorCalculator.class);
    SupportResistanceCalculator supportResistanceCalculator =
        mock(SupportResistanceCalculator.class);
    CandlestickPatternCalculator patternCalculator = mock(CandlestickPatternCalculator.class);
    ChartPatternCalculator chartPatternCalculator = mock(ChartPatternCalculator.class);
    ChartPatternConfig chartPatternConfig = new ChartPatternConfig();

    when(downloader.downloadAndPersistDailyPrices()).thenReturn(0);

    DailyScheduler scheduler =
        new DailyScheduler(
            downloader,
            weeklyPriceCalculator,
            indicatorCalculator,
            supportResistanceCalculator,
            patternCalculator,
            chartPatternCalculator,
            chartPatternConfig);

    scheduler.runScheduledUpdate();

    verify(downloader, times(1)).downloadAndPersistDailyPrices();
    verify(weeklyPriceCalculator, never()).computeWeeklyPrices();
    verify(indicatorCalculator, never()).computeIndicators();
    verify(supportResistanceCalculator, never()).computeSupportResistances();
    verify(patternCalculator, never()).computeCandleStickPatterns();
    verify(chartPatternCalculator, never()).computeChartPatterns();
  }

  @Test
  void testRunOnStartupWithException() {
    YahooFinanceDownloader downloader = mock(YahooFinanceDownloader.class);
    WeeklyPriceCalculator weeklyPriceCalculator = mock(WeeklyPriceCalculator.class);
    IndicatorCalculator indicatorCalculator = mock(IndicatorCalculator.class);
    SupportResistanceCalculator supportResistanceCalculator =
        mock(SupportResistanceCalculator.class);
    CandlestickPatternCalculator patternCalculator = mock(CandlestickPatternCalculator.class);
    ChartPatternCalculator chartPatternCalculator = mock(ChartPatternCalculator.class);
    ChartPatternConfig chartPatternConfig = new ChartPatternConfig();

    doThrow(new RuntimeException("Simulated Failure")).when(downloader).downloadAndPersistDailyPrices();

    DailyScheduler scheduler =
        new DailyScheduler(
            downloader,
            weeklyPriceCalculator,
            indicatorCalculator,
            supportResistanceCalculator,
            patternCalculator,
            chartPatternCalculator,
            chartPatternConfig);

    scheduler.runOnStartup();

    verify(downloader, times(1)).downloadAndPersistDailyPrices();
    verify(weeklyPriceCalculator, never()).computeWeeklyPrices();
    verify(indicatorCalculator, never()).computeIndicators();
    verify(supportResistanceCalculator, never()).computeSupportResistances();
    verify(patternCalculator, never()).computeCandleStickPatterns();
    verify(chartPatternCalculator, never()).computeChartPatterns();
  }

  @Test
  void testConcurrentExecutionSkipped() throws InterruptedException {
    YahooFinanceDownloader downloader = mock(YahooFinanceDownloader.class);
    WeeklyPriceCalculator weeklyPriceCalculator = mock(WeeklyPriceCalculator.class);
    IndicatorCalculator indicatorCalculator = mock(IndicatorCalculator.class);
    SupportResistanceCalculator supportResistanceCalculator =
        mock(SupportResistanceCalculator.class);
    CandlestickPatternCalculator patternCalculator = mock(CandlestickPatternCalculator.class);
    ChartPatternCalculator chartPatternCalculator = mock(ChartPatternCalculator.class);
    ChartPatternConfig chartPatternConfig = new ChartPatternConfig();

    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(1);

    doAnswer(
            invocation -> {
              startLatch.countDown();
              finishLatch.await(5, TimeUnit.SECONDS);
              return 5;
            })
        .when(downloader)
        .downloadAndPersistDailyPrices();

    DailyScheduler scheduler =
        new DailyScheduler(
            downloader,
            weeklyPriceCalculator,
            indicatorCalculator,
            supportResistanceCalculator,
            patternCalculator,
            chartPatternCalculator,
            chartPatternConfig);

    Thread updateThread = new Thread(scheduler::runScheduledUpdate);
    updateThread.start();

    startLatch.await(2, TimeUnit.SECONDS);

    // Call scheduler again in main thread — should skip since updateThread is still running
    scheduler.runOnStartup();

    finishLatch.countDown();
    updateThread.join(2000);

    verify(downloader, times(1)).downloadAndPersistDailyPrices();
    verify(weeklyPriceCalculator, times(1)).computeWeeklyPrices();
    verify(indicatorCalculator, times(1)).computeIndicators();
    verify(supportResistanceCalculator, times(1)).computeSupportResistances();
    verify(patternCalculator, times(1)).computeCandleStickPatterns();
    verify(chartPatternCalculator, never()).computeChartPatterns();
  }
}
