package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.indicators.Indicator;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorRegistry;
import com.alphaflow.engine.pipeline.PipelineJob;
import com.alphaflow.persistence.dtos.TickerIndicatorSyncState;
import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyIndicator;
import com.alphaflow.persistence.repositories.DailyIndicatorRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.WeeklyIndicatorRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
public class IndicatorCalculator implements PipelineJob {

  public static final String JOB_NAME = "CALCULATE_INDICATOR";

  private final IndicatorRegistry indicatorRegistry;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final DailyIndicatorRepository dailyIndicatorRepository;
  private final WeeklyIndicatorRepository weeklyIndicatorRepository;

  @Autowired @Lazy private IndicatorCalculator indicatorCalculator;

  public IndicatorCalculator(
      IndicatorRegistry registry,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailyIndicatorRepository dailyIndicatorRepository,
      WeeklyIndicatorRepository weeklyIndicatorRepository) {
    this.indicatorRegistry = registry;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailyIndicatorRepository = dailyIndicatorRepository;
    this.weeklyIndicatorRepository = weeklyIndicatorRepository;
  }

  @Override
  public String getJobName() {
    return JOB_NAME;
  }

  @Override
  public boolean shouldRun(Timeframe timeframe) {
    List<TickerIndicatorSyncState> syncStates = loadSyncStates(timeframe);
    for (TickerIndicatorSyncState syncState : syncStates) {
      LocalDate today = LocalDate.now(syncState.ticker().getCountry().getZoneId());
      if (PipelineGating.requiresUpdate(
          timeframe, syncState.latestSourceDate(), syncState.latestCalculatedDate(), today)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void execute(Timeframe timeframe) {
    computeIndicators(timeframe);
  }

  public void computeIndicators(Timeframe timeframe) {
    log.info("Computing {} indicators...", timeframe);

    List<TickerIndicatorSyncState> syncStates = loadSyncStates(timeframe);
    Map<Ticker, List<TickerIndicatorSyncState>> statesByTicker = new LinkedHashMap<>();
    for (TickerIndicatorSyncState syncState : syncStates) {
      statesByTicker
          .computeIfAbsent(syncState.ticker(), ignoredTicker -> new ArrayList<>())
          .add(syncState);
    }

    Map<Ticker, List<TickerIndicatorSyncState>> staleByTicker = new LinkedHashMap<>();
    for (Map.Entry<Ticker, List<TickerIndicatorSyncState>> entry : statesByTicker.entrySet()) {
      Ticker ticker = entry.getKey();
      LocalDate today = LocalDate.now(ticker.getCountry().getZoneId());
      List<TickerIndicatorSyncState> staleDefinitions = new ArrayList<>();
      for (TickerIndicatorSyncState syncState : entry.getValue()) {
        if (PipelineGating.requiresUpdate(
            timeframe, syncState.latestSourceDate(), syncState.latestCalculatedDate(), today)) {
          staleDefinitions.add(syncState);
        }
      }
      if (!staleDefinitions.isEmpty()) {
        staleByTicker.put(ticker, staleDefinitions);
      } else {
        log.debug(
            "{}: up to date across all {} indicator definitions. Skipping.",
            ticker.getTickerSymbol(),
            entry.getValue().size());
      }
    }

    int totalActiveTickers = statesByTicker.size();
    int staleTickerCount = staleByTicker.size();
    if (staleTickerCount > 0) {
      log.info(
          "{} indicator computation: {} of {} active tickers require processing (skipping {} up-to-date tickers).",
          timeframe,
          staleTickerCount,
          totalActiveTickers,
          totalActiveTickers - staleTickerCount);

      IndicatorCalculator proxy = (indicatorCalculator != null) ? indicatorCalculator : this;

      for (Map.Entry<Ticker, List<TickerIndicatorSyncState>> entry : staleByTicker.entrySet()) {
        Ticker ticker = entry.getKey();
        try {
          proxy.computeIndicatorForTicker(ticker, timeframe, entry.getValue());
        } catch (Exception exception) {
          log.error(
              "Failed to compute {} indicators for ticker {}: {}",
              timeframe,
              ticker.getTickerSymbol(),
              exception.getMessage(),
              exception);
        }
      }

      log.info("{} indicators computed.", timeframe);
    } else {
      log.info(
          "{} indicator computation: 0 of {} active tickers require processing; skipping step.",
          timeframe,
          totalActiveTickers);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void computeIndicatorForTicker(
      Ticker ticker, Timeframe timeframe, List<TickerIndicatorSyncState> staleStates) {
    log.info("{}: Computing {} indicators...", ticker.getTickerSymbol(), timeframe);
    if (!staleStates.isEmpty()) {
      List<PriceBar> bars = loadBars(ticker, timeframe);
      if (!bars.isEmpty()) {
        int savedCount = computeAndSaveIndicators(ticker, timeframe, bars, staleStates);
        log.info(
            "Ticker {}: Saved {} {} indicators.",
            ticker.getTickerSymbol(),
            savedCount,
            timeframe.name().toLowerCase());
      } else {
        log.debug(
            "{}: No {} price bars found. Skipping indicator computation.",
            ticker.getTickerSymbol(),
            timeframe.name().toLowerCase());
      }
    } else {
      log.info(
          "{}: No stale indicator definitions found. Skipping computation.",
          ticker.getTickerSymbol());
    }
  }

  private List<TickerIndicatorSyncState> loadSyncStates(Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyIndicatorRepository.findSyncStatesForActiveTickers();
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyIndicatorRepository.findSyncStatesForActiveTickers();
    } else {
      log.error("Unsupported timeframe for loading indicator sync states: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  private List<PriceBar> loadBars(Ticker ticker, Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
          .map(PriceBar::from)
          .toList();
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker).stream()
          .map(PriceBar::from)
          .toList();
    } else {
      log.error("Unsupported timeframe for loading bars: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  private int computeAndSaveIndicators(
      Ticker ticker,
      Timeframe timeframe,
      List<PriceBar> bars,
      List<TickerIndicatorSyncState> staleStates) {
    if (timeframe == Timeframe.DAILY) {
      List<DailyIndicator> toInsert = new ArrayList<>();
      for (TickerIndicatorSyncState syncState : staleStates) {
        IndicatorDefinition definition = syncState.indicatorDefinition();
        LocalDate latestCalculatedDate = syncState.latestCalculatedDate();
        Map<LocalDate, Map<String, BigDecimal>> computed = computeSingleSeries(bars, definition);

        for (Map.Entry<LocalDate, Map<String, BigDecimal>> point : computed.entrySet()) {
          LocalDate date = point.getKey();
          if (latestCalculatedDate == null || date.isAfter(latestCalculatedDate)) {
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
      } else {
        return 0;
      }
    } else if (timeframe == Timeframe.WEEKLY) {
      List<WeeklyIndicator> toInsert = new ArrayList<>();
      for (TickerIndicatorSyncState syncState : staleStates) {
        IndicatorDefinition definition = syncState.indicatorDefinition();
        LocalDate latestCalculatedDate = syncState.latestCalculatedDate();
        Map<LocalDate, Map<String, BigDecimal>> computed = computeSingleSeries(bars, definition);

        if (latestCalculatedDate != null) {
          weeklyIndicatorRepository
              .deleteByTickerAndIndicatorDefinitionAndPriceDateGreaterThanEqual(
                  ticker, definition, latestCalculatedDate);
        }

        for (Map.Entry<LocalDate, Map<String, BigDecimal>> point : computed.entrySet()) {
          LocalDate date = point.getKey();
          if (latestCalculatedDate == null || !date.isBefore(latestCalculatedDate)) {
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
      } else {
        return 0;
      }
    } else {
      log.error("Unsupported timeframe for saving indicators: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  private Map<LocalDate, Map<String, BigDecimal>> computeSingleSeries(
      List<PriceBar> bars, IndicatorDefinition definition) {
    Indicator indicator = indicatorRegistry.get(definition.getType());
    IndicatorParams params = IndicatorParams.of(definition.getParams());
    return indicator.compute(bars, params, definition.getSource());
  }
}
