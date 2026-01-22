package com.alphaflow.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for periodic data updates.
 * This component automates the download of tick data, computation of market data metrics,
 * and calculation of market state indicators on a regular basis.
 */
@Component
public class DataUpdateScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataUpdateScheduler.class);

    private final TickDataDownloader tickDataDownloader;
    private final MarketDataComputer marketDataComputer;
    private final MarketStateComputer marketStateComputer;

    public DataUpdateScheduler(
            TickDataDownloader tickDataDownloader,
            MarketDataComputer marketDataComputer,
            MarketStateComputer marketStateComputer
    ) {
        this.tickDataDownloader = tickDataDownloader;
        this.marketDataComputer = marketDataComputer;
        this.marketStateComputer = marketStateComputer;
    }

    /**
     * Runs the data update pipeline every hour.
     * The process is executed with a fixed delay of one hour after the previous completion
     * to prevent overlapping executions if a cycle takes longer than expected.
     */
    @Scheduled(fixedDelay = 3600000) // 1 hour in milliseconds
    public void runDataUpdate() {
        log.info("Starting scheduled data update cycle...");
        try {
            log.info("Step 1/3: Downloading tick data...");
            tickDataDownloader.download();

            log.info("Step 2/3: Computing market data metrics...");
            marketDataComputer.compute();

            log.info("Step 3/3: Computing market state indicators...");
            marketStateComputer.compute();

            log.info("Scheduled data update cycle completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during scheduled data update cycle", e);
        }
    }
}
