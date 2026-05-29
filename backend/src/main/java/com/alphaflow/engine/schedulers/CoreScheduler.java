package com.alphaflow.engine.schedulers;

import com.alphaflow.engine.calculators.WeeklyPriceCalculator;
import com.alphaflow.engine.downloaders.YahooFinanceDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    public CoreScheduler(
            YahooFinanceDownloader yahooFinanceDownloader,
            WeeklyPriceCalculator weeklyPriceCalculator
    ) {
        this.yahooFinanceDownloader = yahooFinanceDownloader;
        this.weeklyPriceCalculator = weeklyPriceCalculator;
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
     * Runs the data update pipeline immediately upon application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        log.info("Starting initial data update cycle upon startup...");
        run();
    }

    private void run() {
        try {
            log.info("Step 1/2: Downloading Yahoo Finance daily data...");
            yahooFinanceDownloader.download();

            log.info("Step 2/2: Computing weekly candles...");
            weeklyPriceCalculator.computeWeeklyPrices();

            log.info("Scheduled data update cycle completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during scheduled data update cycle", e);
        }
    }
}
