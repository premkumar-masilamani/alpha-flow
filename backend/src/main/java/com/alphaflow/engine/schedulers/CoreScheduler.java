package com.alphaflow.engine.schedulers;

import com.alphaflow.engine.calculators.IndicatorCalculator;
import com.alphaflow.engine.calculators.WeeklyPriceCalculator;
import com.alphaflow.engine.downloaders.YahooFinanceDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CoreScheduler for periodic data updates.
 * This component automates the download of daily price data and the
 * aggregation of weekly price data on a regular basis.
 */
@Component
public class CoreScheduler {

    private static final Logger log = LoggerFactory.getLogger(CoreScheduler.class);

    private final YahooFinanceDownloader yahooFinanceDownloader;
    private final WeeklyPriceCalculator weeklyPriceCalculator;
    private final IndicatorCalculator indicatorCalculator;
    private final com.alphaflow.api.services.AnalysisService analysisService;

    // Guards against overlapping pipeline runs (e.g. startup run still in progress when the hourly cron fires).
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CoreScheduler(
            YahooFinanceDownloader yahooFinanceDownloader,
            WeeklyPriceCalculator weeklyPriceCalculator,
            IndicatorCalculator indicatorCalculator,
            com.alphaflow.api.services.AnalysisService analysisService
    ) {
        this.yahooFinanceDownloader = yahooFinanceDownloader;
        this.weeklyPriceCalculator = weeklyPriceCalculator;
        this.indicatorCalculator = indicatorCalculator;
        this.analysisService = analysisService;
    }

    /**
     * Runs the data update pipeline every hour on the hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void runScheduledUpdate() {
        log.info("Starting scheduled data update cycle (on the hour)...");
        run();
    }

    /**
     * Runs the data update pipeline asynchronously upon application startup,
     * so it does not block the application's main thread.
     */
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
        try {
            log.info("Step 1/4: Downloading Yahoo Finance daily data...");
            yahooFinanceDownloader.download();

            log.info("Step 2/4: Computing weekly candles...");
            weeklyPriceCalculator.computeWeeklyPrices();

            log.info("Step 3/4: Computing indicators...");
            indicatorCalculator.computeIndicators();

            log.info("Step 4/4: Computing technical analysis signals...");
            analysisService.computeAnalysis();

            log.info("Scheduled data update cycle completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during scheduled data update cycle", e);
        } finally {
            running.set(false);
        }
    }
}
