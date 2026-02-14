package com.alphaflow.infrastructure.schedulers;

import com.alphaflow.backtest.engine.CandlestickBacktester;
import com.alphaflow.backtest.engine.RenkoBacktester;
import com.alphaflow.backtest.services.PerformanceScorer;
import com.alphaflow.engine.calculation.CandleDataCalculator;
import com.alphaflow.engine.calculation.IndicatorCalculator;
import com.alphaflow.engine.calculation.IndicatorDerivativeCalculator;
import com.alphaflow.engine.calculation.RenkoDataCalculator;
import com.alphaflow.engine.downloaders.BinanceDownloader;
import com.alphaflow.engine.downloaders.YahooFinanceDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * CoreScheduler for periodic data updates.
 * This component automates the download of tick data, computation of candle metrics,
 * and calculation of indicators on a regular basis.
 */
@Component
public class CoreScheduler {

    private static final Logger log = LoggerFactory.getLogger(CoreScheduler.class);

    private final BinanceDownloader binanceDownloader;
    private final YahooFinanceDownloader yahooFinanceDownloader;
    private final CandleDataCalculator candleDataCalculator;
    private final IndicatorCalculator indicatorCalculator;
    private final IndicatorDerivativeCalculator indicatorDerivativeCalculator;
    private final RenkoDataCalculator renkoDataCalculator;
    private final CandlestickBacktester candlestickBacktester;
    private final RenkoBacktester renkoBacktester;
    private final PerformanceScorer performanceScorer;

    public CoreScheduler(
            BinanceDownloader binanceDownloader,
            YahooFinanceDownloader yahooFinanceDownloader,
            CandleDataCalculator candleDataCalculator,
            IndicatorCalculator indicatorCalculator,
            IndicatorDerivativeCalculator indicatorDerivativeCalculator,
            RenkoDataCalculator renkoDataCalculator,
            CandlestickBacktester candlestickBacktester,
            RenkoBacktester renkoBacktester,
            PerformanceScorer performanceScorer
    ) {
        this.binanceDownloader = binanceDownloader;
        this.yahooFinanceDownloader = yahooFinanceDownloader;
        this.candleDataCalculator = candleDataCalculator;
        this.indicatorCalculator = indicatorCalculator;
        this.indicatorDerivativeCalculator = indicatorDerivativeCalculator;
        this.renkoDataCalculator = renkoDataCalculator;
        this.candlestickBacktester = candlestickBacktester;
        this.renkoBacktester = renkoBacktester;
        this.performanceScorer = performanceScorer;
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
            log.info("Step 1a/7: Downloading Binance tick data...");
            binanceDownloader.download();

            log.info("Step 1b/7: Downloading Yahoo Finance daily data...");
            yahooFinanceDownloader.download();

            log.info("Step 2/7: Computing candle metrics...");
            candleDataCalculator.calculate();

            log.info("Step 3/7: Computing indicators...");
            indicatorCalculator.calculate();

            log.info("Step 4/7: Computing capital momentum...");
            indicatorDerivativeCalculator.calculate();

            log.info("Step 5/7: Computing Renko data...");
            renkoDataCalculator.calculate();

            log.info("Step 6/7: Running Candlestick backtests...");
            candlestickBacktester.compute();

            log.info("Step 7/8: Running Renko backtests...");
            renkoBacktester.compute();

            log.info("Step 8/8: Evaluating strategy performance...");
            performanceScorer.score();

            log.info("Scheduled data update cycle completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during scheduled data update cycle", e);
        }
    }
}
