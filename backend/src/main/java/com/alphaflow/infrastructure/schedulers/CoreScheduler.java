package com.alphaflow.infrastructure.schedulers;

import com.alphaflow.backtest.computers.BacktestComputer;
import com.alphaflow.backtest.computers.RenkoBacktestComputer;
import com.alphaflow.engine.computers.*;
import com.alphaflow.engine.downloaders.BinanceDataDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * CoreScheduler for periodic data updates.
 * This component automates the download of tick data, computation of market data metrics,
 * and calculation of market state indicators on a regular basis.
 */
@Component
public class CoreScheduler {

    private static final Logger log = LoggerFactory.getLogger(CoreScheduler.class);

    private final BinanceDataDownloader binanceDataDownloader;
    private final MarketDataComputer marketDataComputer;
    private final MarketStateComputer marketStateComputer;
    private final MarketStateDerivativeComputer marketStateDerivativeComputer;
    private final RenkoDataComputer renkoDataComputer;
    private final BacktestComputer backtestComputer;
    private final RenkoBacktestComputer renkoBacktestComputer;

    public CoreScheduler(
            BinanceDataDownloader binanceDataDownloader,
            MarketDataComputer marketDataComputer,
            MarketStateComputer marketStateComputer,
            MarketStateDerivativeComputer marketStateDerivativeComputer,
            RenkoDataComputer renkoDataComputer,
            BacktestComputer backtestComputer,
            RenkoBacktestComputer renkoBacktestComputer
    ) {
        this.binanceDataDownloader = binanceDataDownloader;
        this.marketDataComputer = marketDataComputer;
        this.marketStateComputer = marketStateComputer;
        this.marketStateDerivativeComputer = marketStateDerivativeComputer;
        this.renkoDataComputer = renkoDataComputer;
        this.backtestComputer = backtestComputer;
        this.renkoBacktestComputer = renkoBacktestComputer;
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
            log.info("Step 1/7: Downloading tick data...");
            binanceDataDownloader.download();

            log.info("Step 2/7: Computing market data metrics...");
            marketDataComputer.compute();

            log.info("Step 3/7: Computing market state indicators...");
            marketStateComputer.compute();

            log.info("Step 4/7: Computing capital momentum...");
            marketStateDerivativeComputer.compute();

            log.info("Step 5/7: Computing Renko bricks...");
            renkoDataComputer.compute();

            log.info("Step 6/7: Running backtests...");
            backtestComputer.compute();

            log.info("Step 7/7: Running Renko backtests...");
            renkoBacktestComputer.compute();

            log.info("Scheduled data update cycle completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during scheduled data update cycle", e);
        }
    }
}
