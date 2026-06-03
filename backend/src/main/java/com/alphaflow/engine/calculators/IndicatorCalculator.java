package com.alphaflow.engine.calculators;

import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Orchestrates indicator computation across all active tickers — the third pipeline step, after the
 *
 * <p>Yahoo download and the weekly rollup. Mirrors {@link WeeklyPriceCalculator}: each ticker is
 *
 * <p>processed in its own transaction ({@link IndicatorTickerProcessor}), and a failure on one
 * ticker is
 *
 * <p>logged and isolated so the rest still complete.
 */
@Component
public class IndicatorCalculator {

  private static final Logger log = LoggerFactory.getLogger(IndicatorCalculator.class);

  private final TickerRepository tickerRepository;

  private final IndicatorTickerProcessor indicatorTickerProcessor;

  public IndicatorCalculator(
      TickerRepository tickerRepository, IndicatorTickerProcessor indicatorTickerProcessor) {

    this.tickerRepository = tickerRepository;

    this.indicatorTickerProcessor = indicatorTickerProcessor;
  }

  public void computeIndicators() {

    log.info("Starting indicator computation...");

    List<Ticker> tickers = tickerRepository.findByIsActiveTrue();

    log.info("Found {} active tickers to process.", tickers.size());

    for (Ticker ticker : tickers) {

      try {

        indicatorTickerProcessor.processTicker(ticker);

      } catch (Exception e) {

        log.error(
            "Failed to compute indicators for ticker {}: {}",
            ticker.getTickerSymbol(),
            e.getMessage(),
            e);
      }
    }

    log.info("Indicator computation completed.");
  }
}
