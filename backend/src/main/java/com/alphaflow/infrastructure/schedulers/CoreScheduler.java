package com.alphaflow.infrastructure.schedulers;

import com.alphaflow.backtest.engine.CandlestickBacktester;
import com.alphaflow.backtest.engine.RenkoBacktester;
import com.alphaflow.backtest.services.PerformanceScoringService;
import com.alphaflow.engine.calculation.*;
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
    private final MarketDataCalculator marketDataCalculator;
    private final MarketStateCalculator marketStateCalculator;
    private final MarketStateDerivativeCalculator marketStateDerivativeCalculator;
    private final RenkoDataCalculator renkoDataCalculator;
    private final CandlestickBacktester candlestickBacktester;
    private final RenkoBacktester renkoBacktester;
    private final PerformanceScoringService performanceScoringService;

    public CoreScheduler(
            BinanceDataDownloader binanceDataDownloader,
            MarketDataCalculator marketDataCalculator,
            MarketStateCalculator marketStateCalculator,
            MarketStateDerivativeCalculator marketStateDerivativeCalculator,
            RenkoDataCalculator renkoDataCalculator,
            CandlestickBacktester candlestickBacktester,
            RenkoBacktester renkoBacktester,
            PerformanceScoringService performanceScoringService
    ) {
        this.binanceDataDownloader = binanceDataDownloader;
        this.marketDataCalculator = marketDataCalculator;
        this.marketStateCalculator = marketStateCalculator;
        this.marketStateDerivativeCalculator = marketStateDerivativeCalculator;
        this.renkoDataCalculator = renkoDataCalculator;
        this.candlestickBacktester = candlestickBacktester;
        this.renkoBacktester = renkoBacktester;
        this.performanceScoringService = performanceScoringService;
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
            marketDataCalculator.calculate();

            log.info("Step 3/7: Computing market state indicators...");
            marketStateCalculator.calculate();

            log.info("Step 4/7: Computing capital momentum...");
            marketStateDerivativeCalculator.calculate();

            log.info("Step 5/7: Computing Renko bricks...");
            renkoDataCalculator.calculate();

            log.info("Step 6/7: Running Candlestick backtests...");
            candlestickBacktester.compute();

            log.info("Step 7/8: Running Renko backtests...");
            renkoBacktester.compute();

            log.info("Step 8/8: Evaluating strategy performance...");
            performanceScoringService.updateAllScores();

            log.info("Scheduled data update cycle completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during scheduled data update cycle", e);
        }
    }
}
