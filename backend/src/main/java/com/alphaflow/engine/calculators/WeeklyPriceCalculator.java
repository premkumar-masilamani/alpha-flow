package com.alphaflow.engine.calculators;

import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WeeklyPriceCalculator {

  private static final Logger log = LoggerFactory.getLogger(WeeklyPriceCalculator.class);

  private final TickerRepository tickerRepository;

  private final WeeklyTickerProcessor weeklyTickerProcessor;

  public WeeklyPriceCalculator(
      TickerRepository tickerRepository, WeeklyTickerProcessor weeklyTickerProcessor) {

    this.tickerRepository = tickerRepository;

    this.weeklyTickerProcessor = weeklyTickerProcessor;
  }

  public void computeWeeklyPrices() {

    log.info("Starting weekly price computation...");

    List<Ticker> tickers = tickerRepository.findByIsActiveTrue();

    log.info("Found {} active tickers to process.", tickers.size());

    for (Ticker ticker : tickers) {

      try {

        weeklyTickerProcessor.processTicker(ticker);

      } catch (Exception e) {

        log.error(
            "Failed to compute weekly prices for ticker {}: {}",
            ticker.getTickerSymbol(),
            e.getMessage(),
            e);
      }
    }

    log.info("Weekly price computation completed.");
  }
}
