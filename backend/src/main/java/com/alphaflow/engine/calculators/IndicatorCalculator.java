package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.configs.IndicatorConfig;
import com.alphaflow.engine.indicators.Indicator;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorRegistry;
import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyIndicator;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.repositories.DailyIndicatorRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyIndicatorRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

  public void computeIndicators() {
    log.info("Computing indicators...");

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

    log.info("Indicators computed.");
  }

  private Map<IndicatorDefinition, Map<LocalDate, Map<String, BigDecimal>>> computeIndicators(
      List<PriceBar> bars, List<IndicatorDefinition> definitions) {
    if (bars.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<IndicatorDefinition, Map<LocalDate, Map<String, BigDecimal>>> results =
        new LinkedHashMap<>();
    for (IndicatorDefinition definition : definitions) {
      Indicator indicator = indicatorRegistry.get(definition.getType());
      IndicatorParams params = IndicatorParams.of(definition.getParams());
      Map<LocalDate, Map<String, BigDecimal>> computed =
          indicator.compute(bars, params, definition.getSource());
      results.put(definition, computed);
    }
    return results;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void computeIndicatorForTicker(Ticker ticker) {
    log.info("{}: Computing indicators...", ticker.getTickerSymbol());
    List<IndicatorDefinition> indicatorDefinitions = indicatorConfig.getDefinitions();
    if (indicatorDefinitions.isEmpty()) {
      log.info(
          "{}: No indicator definitions found. Skipping computation.", ticker.getTickerSymbol());
      return;
    }

    // Step 1: Load the bars (daily and weekly)
    List<PriceBar> dailyBars = loadBars(ticker, Timeframe.DAILY);
    List<PriceBar> weeklyBars = loadBars(ticker, Timeframe.WEEKLY);

    // Step 2: Compute the indicators (common)
    Map<IndicatorDefinition, Map<LocalDate, Map<String, BigDecimal>>> dailyIndicators =
        computeIndicators(dailyBars, indicatorDefinitions);
    Map<IndicatorDefinition, Map<LocalDate, Map<String, BigDecimal>>> weeklyIndicators =
        computeIndicators(weeklyBars, indicatorDefinitions);

    // Step 3: Save the computed indicators (daily and weekly)
    int dailySaved = saveIndicators(ticker, Timeframe.DAILY, dailyIndicators);
    int weeklySaved = saveIndicators(ticker, Timeframe.WEEKLY, weeklyIndicators);

    log.info(
        "Ticker {}: Saved {} daily and {} weekly indicators.",
        ticker.getTickerSymbol(),
        dailySaved,
        weeklySaved);
  }

  private int saveIndicators(
      Ticker ticker,
      Timeframe timeframe,
      Map<IndicatorDefinition, Map<LocalDate, Map<String, BigDecimal>>> computedIndicators) {
    if (computedIndicators.isEmpty()) {
      return 0;
    }

    if (timeframe == Timeframe.DAILY) {
      List<DailyIndicator> toInsert = new ArrayList<>();
      for (Map.Entry<IndicatorDefinition, Map<LocalDate, Map<String, BigDecimal>>> entry :
          computedIndicators.entrySet()) {
        IndicatorDefinition definition = entry.getKey();
        LocalDate lastDate = getLastComputedDate(ticker, definition, Timeframe.DAILY);

        for (Map.Entry<LocalDate, Map<String, BigDecimal>> point : entry.getValue().entrySet()) {
          LocalDate date = point.getKey();
          if (lastDate == null || date.isAfter(lastDate)) {
            toInsert.add(
                DailyIndicator.builder()
                    .ticker(ticker)
                    .indicatorDefinition(definition)
                    .priceDate(date)
                    .values(point.getValue())
                    .build());
          }
        }
      }

      if (!toInsert.isEmpty()) {
        dailyIndicatorRepository.saveAll(toInsert);
        return toInsert.size();
      }
      return 0;
    } else if (timeframe == Timeframe.WEEKLY) {
      List<WeeklyIndicator> toInsert = new ArrayList<>();
      for (Map.Entry<IndicatorDefinition, Map<LocalDate, Map<String, BigDecimal>>> entry :
          computedIndicators.entrySet()) {
        IndicatorDefinition definition = entry.getKey();
        LocalDate lastDate = getLastComputedDate(ticker, definition, Timeframe.WEEKLY);

        for (Map.Entry<LocalDate, Map<String, BigDecimal>> point : entry.getValue().entrySet()) {
          LocalDate date = point.getKey();
          if (lastDate == null || date.isAfter(lastDate)) {
            toInsert.add(
                WeeklyIndicator.builder()
                    .ticker(ticker)
                    .indicatorDefinition(definition)
                    .priceDate(date)
                    .values(point.getValue())
                    .build());
          }
        }
      }

      if (!toInsert.isEmpty()) {
        weeklyIndicatorRepository.saveAll(toInsert);
        return toInsert.size();
      }
      return 0;
    }

    log.error("Unsupported timeframe for saving indicators: {}", timeframe);
    throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
  }

  private LocalDate getLastComputedDate(
      Ticker ticker, IndicatorDefinition definition, Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyIndicatorRepository
          .findFirstByTickerAndIndicatorDefinitionOrderByPriceDateDesc(ticker, definition)
          .map(com.alphaflow.persistence.entities.Indicator::getPriceDate)
          .orElse(null);
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyIndicatorRepository
          .findFirstByTickerAndIndicatorDefinitionOrderByPriceDateDesc(ticker, definition)
          .map(com.alphaflow.persistence.entities.Indicator::getPriceDate)
          .orElse(null);
    }

    log.error("Unsupported timeframe for fetching last computed date: {}", timeframe);
    throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
  }

  private List<PriceBar> loadBars(Ticker ticker, Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
          .map(this::toPriceBar)
          .toList();
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
          .map(this::toPriceBar)
          .toList();
    }

    log.error("Unsupported timeframe for loading bars: {}", timeframe);
    throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
  }

  private PriceBar toPriceBar(DailyPrice d) {
    return new PriceBar(
        d.getPriceDate(),
        d.getPriceOpen(),
        d.getPriceHigh(),
        d.getPriceLow(),
        d.getPriceClose(),
        d.getVolume());
  }

  private PriceBar toPriceBar(WeeklyPrice w) {
    return new PriceBar(
        w.getPriceDate(),
        w.getPriceOpen(),
        w.getPriceHigh(),
        w.getPriceLow(),
        w.getPriceClose(),
        w.getVolume());
  }
}
