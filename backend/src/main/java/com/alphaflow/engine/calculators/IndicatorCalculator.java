package com.alphaflow.engine.calculators;

import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Orchestrates indicator computation across all active tickers — the third pipeline step, after the
 * Yahoo download and the weekly rollup. Mirrors {@link WeeklyPriceCalculator}: each ticker is
 * processed in its own transaction ({@link IndicatorTickerProcessor}), and a failure on one ticker is
 * logged and isolated so the rest still complete.
 */
@Component
public class IndicatorCalculator {

    private static final Logger log = LoggerFactory.getLogger(IndicatorCalculator.class);

    private final TickerRepository tickerRepository;
    private final IndicatorTickerProcessor indicatorTickerProcessor;

    public IndicatorCalculator(
            TickerRepository tickerRepository,
            IndicatorTickerProcessor indicatorTickerProcessor
    ) {
        this.tickerRepository = tickerRepository;
        this.indicatorTickerProcessor = indicatorTickerProcessor;
    }

    public void computeIndicators() {
        log.info("Starting indicator computation...");

        int page = 0;
        int pageSize = 50;
        boolean hasMore = true;

        while (hasMore) {
            Pageable pageable = PageRequest.of(page, pageSize);
            List<Ticker> tickers = tickerRepository.findByIsActiveTrue(pageable);
            if (tickers.isEmpty()) {
                break;
            }
            log.info("Processing indicators page {} (size: {})...", page, tickers.size());
            for (Ticker ticker : tickers) {
                try {
                    indicatorTickerProcessor.processTicker(ticker);
                } catch (Exception e) {
                    log.error("Failed to compute indicators for ticker {}: {}", ticker.getTickerSymbol(), e.getMessage(), e);
                }
            }
            page++;
            if (tickers.size() < pageSize) {
                hasMore = false;
            }
        }

        log.info("Indicator computation completed.");
    }
}
