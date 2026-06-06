package com.alphaflow.engine.calculators;

import com.alphaflow.engine.calculators.indicators.Indicator;
import com.alphaflow.engine.calculators.indicators.IndicatorParams;
import com.alphaflow.engine.calculators.indicators.IndicatorRegistry;
import com.alphaflow.engine.calculators.indicators.PlotPoint;
import com.alphaflow.engine.calculators.indicators.PriceBar;
import com.alphaflow.engine.configs.IndicatorConfig;
import com.alphaflow.engine.configs.IndicatorConfig.IndicatorDefinition;
import com.alphaflow.persistence.entities.IndicatorValue;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Timeframe;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.IndicatorValueRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates indicator computation across all active tickers — the third pipeline step, after the
 * Yahoo download and the weekly rollup. Mirrors {@link WeeklyPriceCalculator}: each ticker is
 * processed in its own transaction, and a failure on one ticker is logged and isolated so the rest
 * still complete.
 */
@Component
public class IndicatorCalculator {

  private static final Logger log = LoggerFactory.getLogger(IndicatorCalculator.class);

  private final TickerRepository tickerRepository;
  private final IndicatorRegistry registry;
  private final IndicatorConfig properties;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final IndicatorValueRepository indicatorValueRepository;

  @Autowired @Lazy private IndicatorCalculator self;

  /**
   * Constructs an IndicatorCalculator.
   *
   * @param tickerRepository the ticker repository
   * @param registry the indicator implementation registry
   * @param properties the configured active indicators properties
   * @param dailyPriceRepository the daily prices repository
   * @param weeklyPriceRepository the weekly prices repository
   * @param indicatorValueRepository the indicator values repository
   */
  public IndicatorCalculator(
      TickerRepository tickerRepository,
      IndicatorRegistry registry,
      IndicatorConfig properties,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      IndicatorValueRepository indicatorValueRepository) {
    this.tickerRepository = tickerRepository;
    this.registry = registry;
    this.properties = properties;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.indicatorValueRepository = indicatorValueRepository;
  }

  /** Triggers the computation of indicators across all active tickers. */
  public void computeIndicators() {
    log.info("Starting indicator computation...");

    List<Ticker> tickers = tickerRepository.findByIsActiveTrue();
    log.info("Found {} active tickers to process for indicators.", tickers.size());

    IndicatorCalculator proxy = (self != null) ? self : this;

    for (Ticker ticker : tickers) {
      try {
        proxy.processTicker(ticker);
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

  /**
   * Computes indicators for a specific ticker across all configured timeframes.
   *
   * @param ticker the ticker entity to process
   */
  @Transactional
  public void processTicker(Ticker ticker) {
    for (Timeframe timeframe : Timeframe.values()) {
      List<IndicatorDefinition> definitions = properties.forTimeframe(timeframe);
      if (definitions.isEmpty()) {
        continue;
      }

      List<PriceBar> bars =
          (timeframe == Timeframe.DAILY) ? loadDailyBars(ticker) : loadWeeklyBars(ticker);

      if (bars.isEmpty()) {
        continue;
      }

      // Delete all existing indicators for this ticker and timeframe to perform full overwrite
      indicatorValueRepository.deleteByTickerAndTimeframe(ticker, timeframe);

      List<IndicatorValue> toInsert = new ArrayList<>();
      for (IndicatorDefinition definition : definitions) {
        Indicator indicator = registry.get(definition.getType());
        IndicatorParams params = IndicatorParams.of(definition.getParams());
        String paramsCanonical = params.canonical();

        List<PlotPoint> computed = indicator.compute(bars, params, definition.getSource());

        for (PlotPoint point : computed) {
          toInsert.add(
              IndicatorValue.builder()
                  .ticker(ticker)
                  .timeframe(timeframe)
                  .indicatorType(definition.getType())
                  .source(definition.getSource())
                  .params(paramsCanonical)
                  .outputName(point.outputName())
                  .priceDate(point.date())
                  .value(point.value())
                  .build());
        }
      }

      if (!toInsert.isEmpty()) {
        indicatorValueRepository.saveAll(toInsert);
      }
    }
  }

  private List<PriceBar> loadDailyBars(Ticker ticker) {
    return dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
        .map(
            d ->
                new PriceBar(
                    d.getPriceDate(),
                    d.getPriceOpen(),
                    d.getPriceHigh(),
                    d.getPriceLow(),
                    d.getPriceClose(),
                    d.getVolume()))
        .toList();
  }

  private List<PriceBar> loadWeeklyBars(Ticker ticker) {
    return weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
        .map(
            w ->
                new PriceBar(
                    w.getPriceDate(),
                    w.getPriceOpen(),
                    w.getPriceHigh(),
                    w.getPriceLow(),
                    w.getPriceClose(),
                    w.getVolume()))
        .toList();
  }
}
