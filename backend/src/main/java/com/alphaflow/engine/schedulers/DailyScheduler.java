package com.alphaflow.engine.schedulers;

import com.alphaflow.engine.calculators.CandlestickPatternCalculator;
import com.alphaflow.engine.calculators.ChartPatternCalculator;
import com.alphaflow.engine.calculators.IndicatorCalculator;
import com.alphaflow.engine.calculators.SupportResistanceCalculator;
import com.alphaflow.engine.calculators.WeeklyPriceCalculator;
import com.alphaflow.engine.configs.ChartPatternConfig;
import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.engine.downloaders.YahooFinanceDownloader;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled coordinator for daily price ingestion and downstream technical analysis pipelines.
 */
@Component
@Slf4j
public class DailyScheduler {

  private final YahooFinanceDownloader yahooFinanceDownloader;
  private final WeeklyPriceCalculator weeklyPriceCalculator;
  private final IndicatorCalculator indicatorCalculator;
  private final SupportResistanceCalculator supportResistanceCalculator;
  private final CandlestickPatternCalculator candlestickPatternCalculator;
  private final ChartPatternCalculator chartPatternCalculator;
  private final ChartPatternConfig chartPatternConfig;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public DailyScheduler(
      YahooFinanceDownloader yahooFinanceDownloader,
      WeeklyPriceCalculator weeklyPriceCalculator,
      IndicatorCalculator indicatorCalculator,
      SupportResistanceCalculator supportResistanceCalculator,
      CandlestickPatternCalculator candlestickPatternCalculator,
      ChartPatternCalculator chartPatternCalculator,
      ChartPatternConfig chartPatternConfig) {
    this.yahooFinanceDownloader = yahooFinanceDownloader;
    this.weeklyPriceCalculator = weeklyPriceCalculator;
    this.indicatorCalculator = indicatorCalculator;
    this.supportResistanceCalculator = supportResistanceCalculator;
    this.candlestickPatternCalculator = candlestickPatternCalculator;
    this.chartPatternCalculator = chartPatternCalculator;
    this.chartPatternConfig = chartPatternConfig;
  }

  @Scheduled(cron = "0 0 * * * *", zone = MarketConfig.TIMEZONE_KOLKATA)
  public void runScheduledUpdate() {
    log.info("Starting scheduled daily data update cycle (on the hour)...");
    run();
  }

  @Async
  @EventListener(ApplicationReadyEvent.class)
  public void runOnStartup() {
    log.info("Starting initial daily data update cycle upon startup...");
    run();
  }

  /**
   * Executes the daily ingestion and multi-step technical analysis pipeline.
   */
  public void run() {
    if (running.compareAndSet(false, true)) {
      long cycleStart = System.currentTimeMillis();
      try {
        log.info("Step 1/6: Downloading Yahoo Finance daily data...");
        long start = System.currentTimeMillis();
        int newlyIngestedRows = yahooFinanceDownloader.downloadAndPersistDailyPrices();
        log.info(
            "Step 1/6 completed in {}. Newly ingested rows: {}",
            SchedulerUtils.formatDuration(System.currentTimeMillis() - start),
            newlyIngestedRows);

        if (newlyIngestedRows > 0) {
          log.info("Step 2/6: Computing weekly candles...");
          start = System.currentTimeMillis();
          weeklyPriceCalculator.computeWeeklyPrices();
          log.info(
              "Step 2/6 completed in {}.",
              SchedulerUtils.formatDuration(System.currentTimeMillis() - start));

          log.info("Step 3/6: Computing support and resistances...");
          start = System.currentTimeMillis();
          supportResistanceCalculator.computeSupportResistances();
          log.info(
              "Step 3/6 completed in {}.",
              SchedulerUtils.formatDuration(System.currentTimeMillis() - start));

          log.info("Step 4/6: Computing candlestick patterns...");
          start = System.currentTimeMillis();
          candlestickPatternCalculator.computeCandleStickPatterns();
          log.info(
              "Step 4/6 completed in {}.",
              SchedulerUtils.formatDuration(System.currentTimeMillis() - start));

          if (chartPatternConfig.isEnabled()) {
            log.info("Step 5/6: Computing chart patterns...");
            start = System.currentTimeMillis();
            chartPatternCalculator.computeChartPatterns();
            log.info(
                "Step 5/6 completed in {}.",
                SchedulerUtils.formatDuration(System.currentTimeMillis() - start));
          } else {
            log.info("Step 5/6: Computing chart patterns is disabled.");
          }

          log.info("Step 6/6: Computing indicators...");
          start = System.currentTimeMillis();
          indicatorCalculator.computeIndicators();
          log.info(
              "Step 6/6 completed in {}.",
              SchedulerUtils.formatDuration(System.currentTimeMillis() - start));

          log.info(
              "Scheduled daily data update cycle completed successfully in {}.",
              SchedulerUtils.formatDuration(System.currentTimeMillis() - cycleStart));
        } else {
          log.info(
              "Zero new daily price rows ingested across all tickers. "
                  + "Skipping downstream calculations (Steps 2 to 6). Cycle completed in {}.",
              SchedulerUtils.formatDuration(System.currentTimeMillis() - cycleStart));
        }
      } catch (Exception exception) {
        log.error(
            "Error occurred during scheduled daily data update cycle after {}",
            SchedulerUtils.formatDuration(System.currentTimeMillis() - cycleStart),
            exception);
      } finally {
        running.set(false);
      }
    } else {
      log.warn("Daily data update cycle skipped: a previous run is still in progress.");
    }
  }
}
