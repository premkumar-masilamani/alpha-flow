package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.indicators.Indicator;
import com.alphaflow.engine.indicators.dtos.IndicatorComputationPoint;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorRegistry;
import com.alphaflow.engine.pipeline.PipelineJob;
import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.DailyPrice;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
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
  public static final int MAX_TAIL_NEW_BARS = 30;

  private final IndicatorRegistry indicatorRegistry;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final DailyIndicatorRepository dailyIndicatorRepository;
  private final WeeklyIndicatorRepository weeklyIndicatorRepository;
  private final TickerRepository tickerRepository;
  private final IndicatorDefinitionRepository indicatorDefinitionRepository;

  @Autowired @Lazy private IndicatorCalculator indicatorCalculator;

  public IndicatorCalculator(
      IndicatorRegistry registry,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailyIndicatorRepository dailyIndicatorRepository,
      WeeklyIndicatorRepository weeklyIndicatorRepository,
      TickerRepository tickerRepository,
      IndicatorDefinitionRepository indicatorDefinitionRepository) {
    this.indicatorRegistry = registry;
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
      Map<Long, LocalDate> maxPriceDateByTickerId = loadMaxPriceDates(timeframe);
      Map<Long, Map<Long, List<com.alphaflow.persistence.entities.Indicator>>> top2ByTickerId =
          proxy.loadTop2IndicatorsByTickerId(timeframe);

      int maxFiniteLookback = computeMaxFiniteLookback(definitions);
      List<Ticker> tickersToProcess = new ArrayList<>();
      List<Long> tailWindowTickerIds = new ArrayList<>();

      for (Ticker ticker : targetTickers) {
        LocalDate maxPriceDate = maxPriceDateByTickerId.get(ticker.getTickerId());
        if (maxPriceDate != null) {
          Map<Long, List<com.alphaflow.persistence.entities.Indicator>> tickerTop2 =
              top2ByTickerId.getOrDefault(ticker.getTickerId(), Collections.emptyMap());
          if (!isDailyTickerUpToDate(timeframe, maxPriceDate, definitions, tickerTop2)) {
            tickersToProcess.add(ticker);
            if (canUseTailWindow(timeframe, maxPriceDate, definitions, tickerTop2)) {
              tailWindowTickerIds.add(ticker.getTickerId());
            }
          }
        }
      }

      Map<Long, List<PriceBar>> tailBarsByTickerId =
          tailWindowTickerIds.isEmpty()
              ? Collections.emptyMap()
              : proxy.loadTailBarsByTickerId(
                  timeframe, tailWindowTickerIds, maxFiniteLookback + MAX_TAIL_NEW_BARS);

      for (Ticker ticker : tickersToProcess) {
        try {
          LocalDate maxPriceDate = maxPriceDateByTickerId.get(ticker.getTickerId());
          Map<Long, List<com.alphaflow.persistence.entities.Indicator>> tickerTop2 =
              top2ByTickerId.getOrDefault(ticker.getTickerId(), Collections.emptyMap());
          List<PriceBar> preloadedTailBars = tailBarsByTickerId.get(ticker.getTickerId());
          proxy.computeIndicatorForTicker(
              ticker, timeframe, definitions, tickerTop2, maxPriceDate, preloadedTailBars);
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
  public Map<Long, Map<Long, List<com.alphaflow.persistence.entities.Indicator>>>
      loadTop2IndicatorsByTickerId(Timeframe timeframe) {
    List<? extends com.alphaflow.persistence.entities.Indicator> rows;
    if (timeframe == Timeframe.DAILY) {
      rows = dailyIndicatorRepository.findTop2LatestForActiveTickers();
    } else if (timeframe == Timeframe.WEEKLY) {
      rows = weeklyIndicatorRepository.findTop2LatestForActiveTickers();
    } else {
      log.error("Unsupported timeframe for loading top-2 indicators: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }

    Map<Long, Map<Long, List<com.alphaflow.persistence.entities.Indicator>>> grouped =
        new LinkedHashMap<>();
    for (com.alphaflow.persistence.entities.Indicator row : rows) {
      Long tickerId = row.getTicker().getTickerId();
      Long definitionId = row.getIndicatorDefinition().getIndicatorDefinitionId();
      grouped
          .computeIfAbsent(tickerId, ignoredTickerId -> new LinkedHashMap<>())
          .computeIfAbsent(definitionId, ignoredDefId -> new ArrayList<>(2))
          .add(row);
    }
    return grouped;
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
      Map<Long, List<com.alphaflow.persistence.entities.Indicator>> top2ByDefId,
      LocalDate maxPriceDate,
      List<PriceBar> preloadedTailBars) {
    log.info("{}: Computing {} indicators...", ticker.getTickerSymbol(), timeframe);
    List<PriceBar> bars =
        (preloadedTailBars != null) ? preloadedTailBars : loadFullBars(ticker, timeframe);
    if (!bars.isEmpty()) {
      int savedCount =
          computeAndSaveIndicators(ticker, timeframe, definitions, bars, top2ByDefId, maxPriceDate);
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

  private int computeMaxFiniteLookback(List<IndicatorDefinition> definitions) {
    int maxLookback = 1;
    for (IndicatorDefinition definition : definitions) {
      Indicator indicator = indicatorRegistry.get(definition.getType());
      if (!indicator.isStateful()) {
        int lookback = indicator.requiredLookback(IndicatorParams.of(definition.getParams()));
        if (lookback > maxLookback) {
          maxLookback = lookback;
        }
      }
    }
    return maxLookback;
  }

  private boolean isDailyTickerUpToDate(
      Timeframe timeframe,
      LocalDate maxPriceDate,
      List<IndicatorDefinition> definitions,
      Map<Long, List<com.alphaflow.persistence.entities.Indicator>> tickerTop2) {
    if (timeframe != Timeframe.DAILY) {
      return false;
    }
    for (IndicatorDefinition definition : definitions) {
      List<com.alphaflow.persistence.entities.Indicator> defRows =
          tickerTop2.getOrDefault(definition.getIndicatorDefinitionId(), List.of());
      if (defRows.isEmpty()) {
        return false;
      }
      com.alphaflow.persistence.entities.Indicator lastIndicator = defRows.getFirst();
      Indicator indicator = indicatorRegistry.get(definition.getType());
      if (!lastIndicator.getPriceDate().equals(maxPriceDate)
          || !indicator.hasCompleteState(lastIndicator.getState())) {
        return false;
      }
    }
    return true;
  }

  private boolean canUseTailWindow(
      Timeframe timeframe,
      LocalDate maxPriceDate,
      List<IndicatorDefinition> definitions,
      Map<Long, List<com.alphaflow.persistence.entities.Indicator>> tickerTop2) {
    for (IndicatorDefinition definition : definitions) {
      List<com.alphaflow.persistence.entities.Indicator> defRows =
          tickerTop2.getOrDefault(definition.getIndicatorDefinitionId(), List.of());
      if (defRows.isEmpty()) {
        return false;
      }
      com.alphaflow.persistence.entities.Indicator lastIndicator = defRows.getFirst();
      Indicator indicator = indicatorRegistry.get(definition.getType());

      if (timeframe == Timeframe.WEEKLY && lastIndicator.getPriceDate().equals(maxPriceDate)) {
        if (indicator.isStateful()
            && (defRows.size() < 2 || !indicator.hasCompleteState(defRows.get(1).getState()))) {
          return false;
        }
      } else {
        if (indicator.isStateful() && !indicator.hasCompleteState(lastIndicator.getState())) {
          return false;
        }
        long gap;
        if (timeframe == Timeframe.DAILY) {
          gap = ChronoUnit.DAYS.between(lastIndicator.getPriceDate(), maxPriceDate);
        } else if (timeframe == Timeframe.WEEKLY) {
          gap = ChronoUnit.WEEKS.between(lastIndicator.getPriceDate(), maxPriceDate);
        } else {
          log.error("Unsupported timeframe for tail window gap check: {}", timeframe);
          throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        }
        if (gap > MAX_TAIL_NEW_BARS) {
          return false;
        }
      }
    }
    return true;
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
      Map<Long, List<com.alphaflow.persistence.entities.Indicator>> top2ByDefId,
      LocalDate maxPriceDate) {
    if (timeframe == Timeframe.DAILY) {
      List<DailyIndicator> toSave = new ArrayList<>();
      for (IndicatorDefinition definition : definitions) {
        collectDefinitionSaves(
            timeframe,
            definition,
            bars,
            top2ByDefId,
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
        collectDefinitionSaves(
            timeframe,
            definition,
            bars,
            top2ByDefId,
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

  @SuppressWarnings("unchecked")
  private <T extends com.alphaflow.persistence.entities.Indicator> void collectDefinitionSaves(
      Timeframe timeframe,
      IndicatorDefinition definition,
      List<PriceBar> bars,
      Map<Long, List<com.alphaflow.persistence.entities.Indicator>> top2ByDefId,
      LocalDate maxPriceDate,
      List<T> toSave,
      BiFunction<LocalDate, IndicatorComputationPoint, T> entityFactory) {
    Indicator indicator = indicatorRegistry.get(definition.getType());
    IndicatorParams params = IndicatorParams.of(definition.getParams());
    List<com.alphaflow.persistence.entities.Indicator> defRows =
        top2ByDefId.getOrDefault(definition.getIndicatorDefinitionId(), List.of());
    com.alphaflow.persistence.entities.Indicator lastIndicator =
        defRows.isEmpty() ? null : defRows.getFirst();
    com.alphaflow.persistence.entities.Indicator prevIndicator =
        defRows.size() > 1 ? defRows.get(1) : null;

    if (lastIndicator == null) {
      Map<LocalDate, IndicatorComputationPoint> computed =
          indicator.computeStateful(bars, params, definition.getSource(), null);
      for (Map.Entry<LocalDate, IndicatorComputationPoint> entry : computed.entrySet()) {
        toSave.add(entityFactory.apply(entry.getKey(), entry.getValue()));
      }
    } else if (timeframe == Timeframe.WEEKLY && lastIndicator.getPriceDate().equals(maxPriceDate)) {
      handleWeeklyMidWeekUpsert(
          indicator, params, definition, bars, lastIndicator, prevIndicator, maxPriceDate, toSave);
    } else if (maxPriceDate.isAfter(lastIndicator.getPriceDate())) {
      handleIncrementalNewBars(
          indicator, params, definition, bars, lastIndicator, toSave, entityFactory);
    } else if (indicator.isStateful() && !indicator.hasCompleteState(lastIndicator.getState())) {
      Map<LocalDate, IndicatorComputationPoint> computed =
          indicator.computeStateful(bars, params, definition.getSource(), null);
      IndicatorComputationPoint anchorPoint = computed.get(lastIndicator.getPriceDate());
      if (anchorPoint != null) {
        lastIndicator.setState(anchorPoint.state());
        toSave.add((T) lastIndicator);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends com.alphaflow.persistence.entities.Indicator> void handleWeeklyMidWeekUpsert(
      Indicator indicator,
      IndicatorParams params,
      IndicatorDefinition definition,
      List<PriceBar> bars,
      com.alphaflow.persistence.entities.Indicator lastIndicator,
      com.alphaflow.persistence.entities.Indicator prevIndicator,
      LocalDate maxPriceDate,
      List<T> toSave) {
    Map<LocalDate, IndicatorComputationPoint> computed;
    if (indicator.isStateful()
        && prevIndicator != null
        && indicator.hasCompleteState(prevIndicator.getState())) {
      computed =
          indicator.computeStateful(
              List.of(bars.getLast()), params, definition.getSource(), prevIndicator.getState());
    } else if (!indicator.isStateful()) {
      List<PriceBar> tailSlice = sliceTail(bars, indicator.requiredLookback(params));
      computed = indicator.computeStateful(tailSlice, params, definition.getSource(), null);
    } else {
      computed = indicator.computeStateful(bars, params, definition.getSource(), null);
      if (prevIndicator != null && !indicator.hasCompleteState(prevIndicator.getState())) {
        IndicatorComputationPoint prevPoint = computed.get(prevIndicator.getPriceDate());
        if (prevPoint != null) {
          prevIndicator.setState(prevPoint.state());
          toSave.add((T) prevIndicator);
        }
      }
    }

    IndicatorComputationPoint point = computed.get(maxPriceDate);
    if (point != null
        && (!mapsNumericallyEqual(lastIndicator.getValues(), point.values())
            || !mapsNumericallyEqual(lastIndicator.getState(), point.state()))) {
      lastIndicator.setValues(point.values());
      lastIndicator.setState(point.state());
      toSave.add((T) lastIndicator);
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends com.alphaflow.persistence.entities.Indicator> void handleIncrementalNewBars(
      Indicator indicator,
      IndicatorParams params,
      IndicatorDefinition definition,
      List<PriceBar> bars,
      com.alphaflow.persistence.entities.Indicator lastIndicator,
      List<T> toSave,
      BiFunction<LocalDate, IndicatorComputationPoint, T> entityFactory) {
    LocalDate lastDate = lastIndicator.getPriceDate();
    Map<LocalDate, IndicatorComputationPoint> computed;
    if (indicator.isStateful() && indicator.hasCompleteState(lastIndicator.getState())) {
      List<PriceBar> newBars = barsAfter(bars, lastDate);
      computed =
          indicator.computeStateful(
              newBars, params, definition.getSource(), lastIndicator.getState());
    } else if (!indicator.isStateful()) {
      List<PriceBar> windowPlusNew =
          sliceWindowPlusNew(bars, lastDate, indicator.requiredLookback(params));
      computed = indicator.computeStateful(windowPlusNew, params, definition.getSource(), null);
    } else {
      computed = indicator.computeStateful(bars, params, definition.getSource(), null);
      IndicatorComputationPoint anchorPoint = computed.get(lastDate);
      if (anchorPoint != null) {
        lastIndicator.setState(anchorPoint.state());
        toSave.add((T) lastIndicator);
      }
    }

    for (Map.Entry<LocalDate, IndicatorComputationPoint> entry : computed.entrySet()) {
      if (entry.getKey().isAfter(lastDate)) {
        toSave.add(entityFactory.apply(entry.getKey(), entry.getValue()));
      }
    }
  }

  private List<PriceBar> sliceTail(List<PriceBar> bars, int count) {
    int startIndex = Math.max(0, bars.size() - count);
    return bars.subList(startIndex, bars.size());
  }

  private List<PriceBar> barsAfter(List<PriceBar> bars, LocalDate lastDate) {
    int firstNewIndex = findFirstIndexAfter(bars, lastDate);
    return bars.subList(firstNewIndex, bars.size());
  }

  private List<PriceBar> sliceWindowPlusNew(
      List<PriceBar> bars, LocalDate lastDate, int requiredLookback) {
    int firstNewIndex = findFirstIndexAfter(bars, lastDate);
    int startIndex = Math.max(0, firstNewIndex - (requiredLookback - 1));
    return bars.subList(startIndex, bars.size());
  }

  private int findFirstIndexAfter(List<PriceBar> bars, LocalDate lastDate) {
    for (int i = 0; i < bars.size(); i++) {
      if (bars.get(i).date().isAfter(lastDate)) {
        return i;
      }
    }
    return bars.size();
  }

  private boolean mapsNumericallyEqual(
      Map<String, BigDecimal> left, Map<String, BigDecimal> right) {
    if (left == null && right == null) {
      return true;
    }
    if (left == null || right == null || left.size() != right.size()) {
      return false;
    }
    for (Map.Entry<String, BigDecimal> entry : left.entrySet()) {
      BigDecimal rightValue = right.get(entry.getKey());
      if (rightValue == null || entry.getValue().compareTo(rightValue) != 0) {
        return false;
      }
    }
    return true;
  }
}
