package com.alphaflow.engine.schedulers;

import com.alphaflow.engine.calculators.CandlestickPatternCalculator;
import com.alphaflow.engine.calculators.ChartPatternCalculator;
import com.alphaflow.engine.calculators.IndicatorCalculator;
import com.alphaflow.engine.calculators.SupportResistanceCalculator;
import com.alphaflow.engine.calculators.WeeklyPriceCalculator;
import com.alphaflow.engine.downloaders.YahooFinanceDownloader;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CoreScheduler {

  private final YahooFinanceDownloader yahooFinanceDownloader;
  private final WeeklyPriceCalculator weeklyPriceCalculator;
  private final IndicatorCalculator indicatorCalculator;
  private final SupportResistanceCalculator supportResistanceCalculator;
  private final CandlestickPatternCalculator candlestickPatternCalculator;
  private final ChartPatternCalculator chartPatternCalculator;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public CoreScheduler(
      YahooFinanceDownloader yahooFinanceDownloader,
      WeeklyPriceCalculator weeklyPriceCalculator,
      IndicatorCalculator indicatorCalculator,
      SupportResistanceCalculator supportResistanceCalculator,
      CandlestickPatternCalculator candlestickPatternCalculator,
      ChartPatternCalculator chartPatternCalculator) {
    this.yahooFinanceDownloader = yahooFinanceDownloader;
    this.weeklyPriceCalculator = weeklyPriceCalculator;
    this.indicatorCalculator = indicatorCalculator;
    this.supportResistanceCalculator = supportResistanceCalculator;
    this.candlestickPatternCalculator = candlestickPatternCalculator;
    this.chartPatternCalculator = chartPatternCalculator;
  }

  @Scheduled(cron = "0 0 * * * *")
  public void runScheduledUpdate() {
    log.info("Starting scheduled data update cycle (on the hour)...");
    run();
  }

  @Async
  @EventListener(ApplicationReadyEvent.class)
  public void runOnStartup() {
    log.info("Starting initial data update cycle upon startup...");
    run();
  }

  private void run() {
    if (!running.compareAndSet(false, true)) {
      log.warn("Data update cycle skipped: a previous run is still in progress.");
      return;
    }

    long cycleStart = System.currentTimeMillis();
    try {
      log.info("Step 1/6: Downloading Yahoo Finance daily data...");
      long start = System.currentTimeMillis();
      yahooFinanceDownloader.downloadDailyPrices();
      log.info("Step 1/6 completed in {}.", formatDuration(System.currentTimeMillis() - start));

      log.info("Step 2/6: Computing weekly candles...");
      start = System.currentTimeMillis();
      weeklyPriceCalculator.computeWeeklyPrices();
      log.info("Step 2/6 completed in {}.", formatDuration(System.currentTimeMillis() - start));

      log.info("Step 3/6: Computing support and resistances...");
      start = System.currentTimeMillis();
      supportResistanceCalculator.computeSupportResistances();
      log.info("Step 3/6 completed in {}.", formatDuration(System.currentTimeMillis() - start));

      log.info("Step 4/6: Computing candlestick patterns...");
      start = System.currentTimeMillis();
      candlestickPatternCalculator.computeCandleStickPatterns();
      log.info("Step 4/6 completed in {}.", formatDuration(System.currentTimeMillis() - start));

      log.info("Step 5/6: Computing chart patterns...");
      start = System.currentTimeMillis();
      chartPatternCalculator.computeChartPatterns();
      log.info("Step 5/6 completed in {}.", formatDuration(System.currentTimeMillis() - start));

      log.info("Step 6/6: Computing indicators...");
      start = System.currentTimeMillis();
      indicatorCalculator.computeIndicators();
      log.info("Step 6/6 completed in {}.", formatDuration(System.currentTimeMillis() - start));

      log.info(
          "Scheduled data update cycle completed successfully in {}.",
          formatDuration(System.currentTimeMillis() - cycleStart));
    } catch (Exception exception) {
      log.error(
          "Error occurred during scheduled data update cycle after {}",
          formatDuration(System.currentTimeMillis() - cycleStart),
          exception);
    } finally {
      running.set(false);
    }
  }

  private String formatDuration(long ms) {
    long minutes = ms / 60000;
    long seconds = (ms % 60000) / 1000;
    return String.format("%d m %d s (%d ms)", minutes, seconds, ms);
  }
}
