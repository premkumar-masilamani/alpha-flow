package com.alphaflow.engine.calculators;

import com.alphaflow.engine.calculators.indicators.Indicator;
import com.alphaflow.engine.calculators.indicators.IndicatorParams;
import com.alphaflow.engine.calculators.indicators.IndicatorRegistry;
import com.alphaflow.engine.calculators.indicators.PlotPoint;
import com.alphaflow.engine.calculators.indicators.PriceBar;
import com.alphaflow.engine.configs.IndicatorConfig;
import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyIndicator;
import com.alphaflow.persistence.enums.Timeframe;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.IndicatorRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  private static final Logger logger = LoggerFactory.getLogger(IndicatorCalculator.class);

  private final TickerRepository tickerRepository;
  private final IndicatorRegistry indicatorRegistry;
  private final IndicatorConfig indicatorConfig;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final IndicatorRepository indicatorRepository;

  @Autowired @Lazy private IndicatorCalculator indicatorCalculator;

  /**
   * Constructs an IndicatorCalculator.
   *
   * @param tickerRepository the ticker repository
   * @param registry the indicator implementation registry
   * @param properties the configured active indicators properties
   * @param dailyPriceRepository the daily prices repository
   * @param weeklyPriceRepository the weekly prices repository
   * @param indicatorRepository the indicators repository
   */
  public IndicatorCalculator(
      TickerRepository tickerRepository,
      IndicatorRegistry registry,
      IndicatorConfig properties,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      IndicatorRepository indicatorRepository) {
    this.tickerRepository = tickerRepository;
    this.indicatorRegistry = registry;
    this.indicatorConfig = properties;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.indicatorRepository = indicatorRepository;
  }

  /** Triggers the computation of indicators across all active tickers. */
  public void computeIndicators() {
    logger.info("Starting indicator computation...");

    List<Ticker> tickers = tickerRepository.findByIsActiveTrue();
    logger.info("Found {} active tickers to process for indicators.", tickers.size());

    IndicatorCalculator proxy = (indicatorCalculator != null) ? indicatorCalculator : this;

    for (Ticker ticker : tickers) {
      try {
        proxy.processTicker(ticker);
      } catch (Exception e) {
        logger.error(
            "Failed to compute indicators for ticker {}: {}",
            ticker.getTickerSymbol(),
            e.getMessage(),
            e);
      }
    }

    logger.info("Indicator computation completed.");
  }

  /**
   * Computes indicators for a specific ticker across all configured timeframes.
   *
   * @param ticker the ticker entity to process
   */
  @Transactional
  public void processTicker(Ticker ticker) {
    for (Timeframe timeframe : Timeframe.values()) {
      List<IndicatorDefinition> IndicatorDefinitions = indicatorConfig.forTimeframe(timeframe);
      if (IndicatorDefinitions.isEmpty()) {
        continue;
      }

      List<PriceBar> bars =
          (timeframe == Timeframe.DAILY) ? loadDailyBars(ticker) : loadWeeklyBars(ticker);

      if (bars.isEmpty()) {
        continue;
      }

      // Delete all existing indicators for this ticker and timeframe to perform full overwrite
      List<Long> indicatorIds =
          IndicatorDefinitions.stream().map(IndicatorDefinition::getIndicatorId).toList();
      indicatorRepository.deleteByTickerAndIndicatorIds(ticker, indicatorIds, timeframe);

      if (timeframe == Timeframe.DAILY) {
        List<DailyIndicator> toInsert = new ArrayList<>();
        for (IndicatorDefinition definition : IndicatorDefinitions) {
          Indicator indicator = indicatorRegistry.get(definition.getType());
          IndicatorParams params = IndicatorParams.of(definition.getParams());

          List<PlotPoint> computed = indicator.compute(bars, params, definition.getSource());

          Map<LocalDate, Map<String, BigDecimal>> pointsByDate = new LinkedHashMap<>();
          for (PlotPoint point : computed) {
            pointsByDate
                .computeIfAbsent(point.date(), d -> new LinkedHashMap<>())
                .put(point.outputName(), point.value());
          }

          for (Map.Entry<LocalDate, Map<String, BigDecimal>> entry : pointsByDate.entrySet()) {
            toInsert.add(
                DailyIndicator.builder()
                    .ticker(ticker)
                    .indicatorDefinition(definition)
                    .priceDate(entry.getKey())
                    .values(entry.getValue())
                    .build());
          }
        }

        if (!toInsert.isEmpty()) {
          indicatorRepository.saveAll(toInsert, timeframe);
        }
      } else {
        List<WeeklyIndicator> toInsert = new ArrayList<>();
        for (IndicatorDefinition definition : IndicatorDefinitions) {
          Indicator indicator = indicatorRegistry.get(definition.getType());
          IndicatorParams params = IndicatorParams.of(definition.getParams());

          List<PlotPoint> computed = indicator.compute(bars, params, definition.getSource());

          Map<LocalDate, Map<String, BigDecimal>> pointsByDate = new LinkedHashMap<>();
          for (PlotPoint point : computed) {
            pointsByDate
                .computeIfAbsent(point.date(), d -> new LinkedHashMap<>())
                .put(point.outputName(), point.value());
          }

          for (Map.Entry<LocalDate, Map<String, BigDecimal>> entry : pointsByDate.entrySet()) {
            toInsert.add(
                WeeklyIndicator.builder()
                    .ticker(ticker)
                    .indicatorDefinition(definition)
                    .priceDate(entry.getKey())
                    .values(entry.getValue())
                    .build());
          }
        }

        if (!toInsert.isEmpty()) {
          indicatorRepository.saveAll(toInsert, timeframe);
        }
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
