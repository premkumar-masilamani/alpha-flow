package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.indicators.dtos.IndicatorAnchor;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.pipeline.PipelineJob;
import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Indicator;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyIndicator;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.repositories.DailyIndicatorRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.IndicatorDefinitionRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyIndicatorRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
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

/**
 * Orchestrates bulk pre-loading and per-ticker transactional persistence for technical indicators.
 *
 * <p>Delegates incremental planning and calculation decisions to {@link
 * IndicatorIncrementalPlanner}.
 */
@Component
@Slf4j
public class IndicatorCalculator implements PipelineJob {

  public static final String JOB_NAME = "CALCULATE_INDICATOR";

  private final IndicatorIncrementalPlanner planner;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final DailyIndicatorRepository dailyIndicatorRepository;
  private final WeeklyIndicatorRepository weeklyIndicatorRepository;
  private final TickerRepository tickerRepository;
  private final IndicatorDefinitionRepository indicatorDefinitionRepository;

  @Autowired @Lazy private IndicatorCalculator indicatorCalculator;

  public IndicatorCalculator(
      IndicatorIncrementalPlanner planner,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailyIndicatorRepository dailyIndicatorRepository,
      WeeklyIndicatorRepository weeklyIndicatorRepository,
      TickerRepository tickerRepository,
      IndicatorDefinitionRepository indicatorDefinitionRepository) {
    this.planner = planner;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailyIndicatorRepository = dailyIndicatorRepository;
    this.weeklyIndicatorRepository = weeklyIndicatorRepository;
    this.tickerRepository = tickerRepository;
    this.indicatorDefinitionRepository = indicatorDefinitionRepository;
  }

  @Override
  public String getJobName() {
    return JOB_NAME;
  }

  @Override
  public boolean shouldRun(Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyIndicatorRepository.hasPendingDailyIndicators();
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyIndicatorRepository.hasPendingWeeklyIndicators();
    } else {
      log.error("Unsupported timeframe for loading indicator shouldRun: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  @Override
  public void execute(Timeframe timeframe) {
    List<Ticker> targetTickers = loadTargetTickers(timeframe);
    List<IndicatorDefinition> definitions = indicatorDefinitionRepository.findAll();

    if (!targetTickers.isEmpty() && !definitions.isEmpty()) {
      log.info(
          "Computing {} indicators for {} tickers and {} definitions...",
          timeframe,
          targetTickers.size(),
          definitions.size());

      IndicatorCalculator proxy = (indicatorCalculator != null) ? indicatorCalculator : this;

      // 1. Bulk pre-load latest price dates and top-2 anchor rows (t and t - 1)
      Map<Long, LocalDate> maxPriceDates = loadMaxPriceDates(timeframe);
      Map<Long, Map<Long, IndicatorAnchor>> anchorsByTicker =
          proxy.loadAnchorsByTickerId(timeframe);

      // 2. Partition tickers into skip, fast tail-window, or full-history fallback
      List<Ticker> tickersToProcess = new ArrayList<>();
      List<Long> tailWindowTickerIds = new ArrayList<>();
      for (Ticker ticker : targetTickers) {
        LocalDate maxPriceDate = maxPriceDates.get(ticker.getTickerId());
        Map<Long, IndicatorAnchor> tickerAnchors =
            anchorsByTicker.getOrDefault(ticker.getTickerId(), Collections.emptyMap());
        if (maxPriceDate != null
            && !planner.isDailyTickerUpToDate(
                timeframe, maxPriceDate, definitions, tickerAnchors)) {
          tickersToProcess.add(ticker);
          if (planner.canUseTailWindow(timeframe, maxPriceDate, definitions, tickerAnchors)) {
            tailWindowTickerIds.add(ticker.getTickerId());
          }
        }
      }

      // 3. Bulk-load short tail bars once for all fast-path tickers
      int tailLimit =
          planner.computeMaxFiniteLookback(definitions)
              + IndicatorIncrementalPlanner.MAX_TAIL_NEW_BARS;
      Map<Long, List<PriceBar>> tailBarsByTicker =
          tailWindowTickerIds.isEmpty()
              ? Collections.emptyMap()
              : proxy.loadTailBarsByTickerId(timeframe, tailWindowTickerIds, tailLimit);

      // 4. Compute and persist each ticker in its own REQUIRES_NEW transaction
      for (Ticker ticker : tickersToProcess) {
        try {
          proxy.computeIndicatorForTicker(
              ticker,
              timeframe,
              definitions,
              anchorsByTicker.getOrDefault(ticker.getTickerId(), Collections.emptyMap()),
              maxPriceDates.get(ticker.getTickerId()),
              tailBarsByTicker.get(ticker.getTickerId()));
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
          "No tickers ({}) or definitions ({}) to process for {} indicators.",
          targetTickers.size(),
          definitions.size(),
          timeframe);
    }
  }

  @Transactional(readOnly = true)
  public Map<Long, Map<Long, IndicatorAnchor>> loadAnchorsByTickerId(Timeframe timeframe) {
    List<? extends Indicator> rows;
    if (timeframe == Timeframe.DAILY) {
      rows = dailyIndicatorRepository.findTop2LatestForActiveTickers();
    } else if (timeframe == Timeframe.WEEKLY) {
      rows = weeklyIndicatorRepository.findTop2LatestForActiveTickers();
    } else {
      log.error("Unsupported timeframe for loading top-2 indicators: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }

    Map<Long, Map<Long, IndicatorAnchor>> anchorsByTicker = new LinkedHashMap<>();
    for (Indicator row : rows) {
      Long tickerId = row.getTicker().getTickerId();
      Long definitionId = row.getIndicatorDefinition().getIndicatorDefinitionId();
      Map<Long, IndicatorAnchor> tickerAnchors =
          anchorsByTicker.computeIfAbsent(tickerId, ignored -> new LinkedHashMap<>());
      IndicatorAnchor existing = tickerAnchors.get(definitionId);
      if (existing == null) {
        tickerAnchors.put(definitionId, new IndicatorAnchor(row, null));
      } else {
        tickerAnchors.put(definitionId, new IndicatorAnchor(existing.latest(), row));
      }
    }
    return anchorsByTicker;
  }

  @Transactional(readOnly = true)
  public Map<Long, List<PriceBar>> loadTailBarsByTickerId(
      Timeframe timeframe, Collection<Long> tickerIds, int limitPerTicker) {
    Map<Long, List<PriceBar>> barsByTickerId = new LinkedHashMap<>();
    if (timeframe == Timeframe.DAILY) {
      for (DailyPrice dailyPrice :
          dailyPriceRepository.findTailBarsForTickers(tickerIds, limitPerTicker)) {
        barsByTickerId
            .computeIfAbsent(dailyPrice.getTicker().getTickerId(), ignored -> new ArrayList<>())
            .add(PriceBar.from(dailyPrice));
      }
    } else if (timeframe == Timeframe.WEEKLY) {
      for (WeeklyPrice weeklyPrice :
          weeklyPriceRepository.findTailBarsForTickers(tickerIds, limitPerTicker)) {
        barsByTickerId
            .computeIfAbsent(weeklyPrice.getTicker().getTickerId(), ignored -> new ArrayList<>())
            .add(PriceBar.from(weeklyPrice));
      }
    } else {
      log.error("Unsupported timeframe for loading tail bars: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
    return barsByTickerId;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void computeIndicatorForTicker(
      Ticker ticker,
      Timeframe timeframe,
      List<IndicatorDefinition> definitions,
      Map<Long, IndicatorAnchor> anchorsByDefId,
      LocalDate maxPriceDate,
      List<PriceBar> preloadedTailBars) {
    log.info("{}: Computing {} indicators...", ticker.getTickerSymbol(), timeframe);
    List<PriceBar> bars =
        (preloadedTailBars != null) ? preloadedTailBars : loadFullBars(ticker, timeframe);
    if (!bars.isEmpty()) {
      int savedCount =
          computeAndSaveIndicators(
              ticker, timeframe, definitions, bars, anchorsByDefId, maxPriceDate);
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
  }

  private List<Ticker> loadTargetTickers(Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyIndicatorRepository.findTickersWithPendingDailyIndicators();
    } else if (timeframe == Timeframe.WEEKLY) {
      return tickerRepository.findByIsActiveTrue();
    } else {
      log.error("Unsupported timeframe for indicator computation: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  private Map<Long, LocalDate> loadMaxPriceDates(Timeframe timeframe) {
    List<Object[]> rows;
    if (timeframe == Timeframe.DAILY) {
      rows = dailyPriceRepository.findMaxPriceDatePerActiveTicker();
    } else if (timeframe == Timeframe.WEEKLY) {
      rows = weeklyPriceRepository.findMaxPriceDatePerActiveTicker();
    } else {
      log.error("Unsupported timeframe for loading max price dates: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }

    Map<Long, LocalDate> maxDateByTickerId = new LinkedHashMap<>();
    for (Object[] row : rows) {
      maxDateByTickerId.put((Long) row[0], (LocalDate) row[1]);
    }
    return maxDateByTickerId;
  }

  private List<PriceBar> loadFullBars(Ticker ticker, Timeframe timeframe) {
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
      List<IndicatorDefinition> definitions,
      List<PriceBar> bars,
      Map<Long, IndicatorAnchor> anchorsByDefId,
      LocalDate maxPriceDate) {
    if (timeframe == Timeframe.DAILY) {
      List<DailyIndicator> toSave = new ArrayList<>();
      for (IndicatorDefinition definition : definitions) {
        IndicatorAnchor anchor =
            anchorsByDefId.getOrDefault(
                definition.getIndicatorDefinitionId(), IndicatorAnchor.EMPTY);
        planner.collectDefinitionUpdates(
            timeframe,
            definition,
            bars,
            anchor,
            maxPriceDate,
            toSave,
            (priceDate, point) ->
                DailyIndicator.builder()
                    .ticker(ticker)
                    .indicatorDefinition(definition)
                    .priceDate(priceDate)
                    .values(point.values())
                    .state(point.state())
                    .build());
      }
      if (!toSave.isEmpty()) {
        dailyIndicatorRepository.saveAll(toSave);
        return toSave.size();
      } else {
        return 0;
      }
    } else if (timeframe == Timeframe.WEEKLY) {
      List<WeeklyIndicator> toSave = new ArrayList<>();
      for (IndicatorDefinition definition : definitions) {
        IndicatorAnchor anchor =
            anchorsByDefId.getOrDefault(
                definition.getIndicatorDefinitionId(), IndicatorAnchor.EMPTY);
        planner.collectDefinitionUpdates(
            timeframe,
            definition,
            bars,
            anchor,
            maxPriceDate,
            toSave,
            (priceDate, point) ->
                WeeklyIndicator.builder()
                    .ticker(ticker)
                    .indicatorDefinition(definition)
                    .priceDate(priceDate)
                    .values(point.values())
                    .state(point.state())
                    .build());
      }
      if (!toSave.isEmpty()) {
        weeklyIndicatorRepository.saveAll(toSave);
        return toSave.size();
      } else {
        return 0;
      }
    } else {
      log.error("Unsupported timeframe for saving indicators: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }
}
