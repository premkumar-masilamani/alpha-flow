package com.alphaflow.engine.calculators;

import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    int page = 0;
    int pageSize = 50;
    boolean hasMore = true;

    while (hasMore) {
      Pageable pageable = PageRequest.of(page, pageSize, Sort.by("tickerId"));
      List<Ticker> tickers = tickerRepository.findByIsActiveTrue(pageable);
      if (tickers.isEmpty()) {
        break;
      }
      log.info("Processing weekly prices page {} (size: {})...", page, tickers.size());
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
      page++;
      if (tickers.size() < pageSize) {
        hasMore = false;
      }
    }

    log.info("Weekly price computation completed.");
  }
}
