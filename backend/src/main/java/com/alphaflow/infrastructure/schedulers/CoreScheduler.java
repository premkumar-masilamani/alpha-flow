package com.alphaflow.infrastructure.schedulers;

import com.alphaflow.api.services.WeeklyCandleService;
import com.alphaflow.engine.calculation.IndicatorCalculator;
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

    private final YahooFinanceDownloader yahooFinanceDownloader;
    private final WeeklyCandleService weeklyCandleService;
    private final IndicatorCalculator indicatorCalculator;

    public CoreScheduler(
            YahooFinanceDownloader yahooFinanceDownloader,
            WeeklyCandleService weeklyCandleService,
            IndicatorCalculator indicatorCalculator
    ) {
        this.yahooFinanceDownloader = yahooFinanceDownloader;
        this.weeklyCandleService = weeklyCandleService;
        this.indicatorCalculator = indicatorCalculator;
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
            log.info("Step 1/3: Downloading Yahoo Finance daily data...");
            yahooFinanceDownloader.download();

            log.info("Step 2/3: Computing weekly candles...");
            weeklyCandleService.computeWeeklyCandles();

            log.info("Step 3/3: Computing indicators...");
            indicatorCalculator.calculate();

            log.info("Scheduled data update cycle completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during scheduled data update cycle", e);
        }
    }
}
