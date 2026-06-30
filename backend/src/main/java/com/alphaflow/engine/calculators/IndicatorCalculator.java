package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.configs.IndicatorConfig;
import com.alphaflow.engine.indicators.Indicator;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorRegistry;
import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyIndicator;
import com.alphaflow.persistence.repositories.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates indicator computation across all active tickers — the third pipeline step, after the
 * Yahoo download and the weekly rollup. Mirrors {@link WeeklyPriceCalculator}: each ticker is
 * processed in its own transaction, and a failure on one ticker is logged and isolated so the rest
 * still complete.
 */
@Component
@Slf4j
public class IndicatorCalculator {

  private final TickerRepository tickerRepository;
  private final IndicatorRegistry indicatorRegistry;
  private final IndicatorConfig indicatorConfig;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final DailyIndicatorRepository dailyIndicatorRepository;
  private final WeeklyIndicatorRepository weeklyIndicatorRepository;

  @Autowired @Lazy private IndicatorCalculator indicatorCalculator;

  /**
   * Constructs an IndicatorCalculator.
   *
   * @param tickerRepository the ticker repository
   * @param registry the indicator implementation registry
   * @param properties the configured active indicators properties
   * @param dailyPriceRepository the daily prices repository
   * @param weeklyPriceRepository the weekly prices repository
   * @param dailyIndicatorRepository the daily indicators repository
   * @param weeklyIndicatorRepository the weekly indicators repository
   */
  public IndicatorCalculator(
      TickerRepository tickerRepository,
      IndicatorRegistry registry,
      IndicatorConfig properties,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailyIndicatorRepository dailyIndicatorRepository,
      WeeklyIndicatorRepository weeklyIndicatorRepository) {
    this.tickerRepository = tickerRepository;
    this.indicatorRegistry = registry;
    this.indicatorConfig = properties;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailyIndicatorRepository = dailyIndicatorRepository;
    this.weeklyIndicatorRepository = weeklyIndicatorRepository;
  }

  /** Triggers the computation of indicators across all active tickers. */
  public void computeIndicators() {
    log.info("Starting indicator computation...");

    List<Ticker> tickers = tickerRepository.findByIsActiveTrue();
    log.info("Found {} active tickers to process for indicators.", tickers.size());

    IndicatorCalculator proxy = (indicatorCalculator != null) ? indicatorCalculator : this;

    for (Ticker ticker : tickers) {
      try {
        proxy.computeIndicatorForTicker(ticker);
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
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void computeIndicatorForTicker(Ticker ticker) {
    log.info("Ticker {}: Starting indicator computation...", ticker.getTickerSymbol());
    int dailySaved = 0;
    int weeklySaved = 0;

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

      if (timeframe == Timeframe.DAILY) {
        List<DailyIndicator> toInsert = new ArrayList<>();
        for (IndicatorDefinition definition : IndicatorDefinitions) {
          Indicator indicator = indicatorRegistry.get(definition.getType());
          IndicatorParams params = IndicatorParams.of(definition.getParams());

          Map<LocalDate, Map<String, BigDecimal>> computed =
              indicator.compute(bars, params, definition.getSource());

          LocalDate lastDate =
              dailyIndicatorRepository
                  .findFirstByTickerAndIndicatorDefinitionOrderByPriceDateDesc(ticker, definition)
                  .map(com.alphaflow.persistence.entities.Indicator::getPriceDate)
                  .orElse(null);

          for (Map.Entry<LocalDate, Map<String, BigDecimal>> entry : computed.entrySet()) {
            LocalDate date = entry.getKey();
            if (lastDate == null || date.isAfter(lastDate)) {
              toInsert.add(
                  DailyIndicator.builder()
                      .ticker(ticker)
                      .indicatorDefinition(definition)
                      .priceDate(date)
                      .values(entry.getValue())
                      .build());
            }
          }
        }

        if (!toInsert.isEmpty()) {
          dailyIndicatorRepository.saveAll(toInsert);
          dailySaved = toInsert.size();
        }
      } else {
        List<WeeklyIndicator> toInsert = new ArrayList<>();
        for (IndicatorDefinition definition : IndicatorDefinitions) {
          Indicator indicator = indicatorRegistry.get(definition.getType());
          IndicatorParams params = IndicatorParams.of(definition.getParams());

          Map<LocalDate, Map<String, BigDecimal>> computed =
              indicator.compute(bars, params, definition.getSource());

          LocalDate lastDate =
              weeklyIndicatorRepository
                  .findFirstByTickerAndIndicatorDefinitionOrderByPriceDateDesc(ticker, definition)
                  .map(com.alphaflow.persistence.entities.Indicator::getPriceDate)
                  .orElse(null);

          for (Map.Entry<LocalDate, Map<String, BigDecimal>> entry : computed.entrySet()) {
            LocalDate date = entry.getKey();
            if (lastDate == null || date.isAfter(lastDate)) {
              toInsert.add(
                  WeeklyIndicator.builder()
                      .ticker(ticker)
                      .indicatorDefinition(definition)
                      .priceDate(date)
                      .values(entry.getValue())
                      .build());
            }
          }
        }

        if (!toInsert.isEmpty()) {
          weeklyIndicatorRepository.saveAll(toInsert);
          weeklySaved = toInsert.size();
        }
      }
    }

    log.info(
        "Ticker {}: Indicator computation completed. Saved {} daily and {} weekly indicator records.",
        ticker.getTickerSymbol(),
        dailySaved,
        weeklySaved);
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
