package com.alphaflow.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * CronScheduler for periodic data updates.
 * This component automates the download of tick data, computation of market data metrics,
 * and calculation of market state indicators on a regular basis.
 */
@Component
public class CronScheduler {

    private static final Logger log = LoggerFactory.getLogger(CronScheduler.class);

    private final TickDataDownloader tickDataDownloader;
    private final MarketDataComputer marketDataComputer;
    private final MarketStateComputer marketStateComputer;
    private final RenkoDataComputer renkoDataComputer;

    public CronScheduler(
            TickDataDownloader tickDataDownloader,
            MarketDataComputer marketDataComputer,
            MarketStateComputer marketStateComputer,
            RenkoDataComputer renkoDataComputer
    ) {
        this.tickDataDownloader = tickDataDownloader;
        this.marketDataComputer = marketDataComputer;
        this.marketStateComputer = marketStateComputer;
        this.renkoDataComputer = renkoDataComputer;
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
            log.info("Step 1/4: Downloading tick data...");
            tickDataDownloader.download();

            log.info("Step 2/4: Computing market data metrics...");
            marketDataComputer.compute();

            log.info("Step 3/4: Computing market state indicators...");
            marketStateComputer.compute();

            log.info("Step 4/4: Computing Renko bricks...");
            renkoDataComputer.compute();

            log.info("Scheduled data update cycle completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during scheduled data update cycle", e);
        }
    }
}
