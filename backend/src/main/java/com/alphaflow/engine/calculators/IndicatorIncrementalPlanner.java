package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.indicators.Indicator;
import com.alphaflow.engine.indicators.dtos.IndicatorAnchor;
import com.alphaflow.engine.indicators.dtos.IndicatorComputationPoint;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorRegistry;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Pure planning and incremental computation logic for technical indicators.
 *
 * <p>Decides whether a ticker can be skipped, computed from a short tail window, or needs full
 * price history, and computes the exact entity inserts/updates for each indicator definition.
 */
@Component
@Slf4j
public class IndicatorIncrementalPlanner {

  public static final int MAX_TAIL_NEW_BARS = 30;

  private final IndicatorRegistry indicatorRegistry;

  public IndicatorIncrementalPlanner(IndicatorRegistry indicatorRegistry) {
    this.indicatorRegistry = indicatorRegistry;
  }

  /** Returns the largest bar window needed by any finite-window (non-stateful) indicator. */
  public int computeMaxFiniteLookback(List<IndicatorDefinition> definitions) {
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

  /** Returns true when a DAILY ticker already has up-to-date values and valid state for all defs. */
  public boolean isDailyTickerUpToDate(
      Timeframe timeframe,
      LocalDate maxPriceDate,
      List<IndicatorDefinition> definitions,
      Map<Long, IndicatorAnchor> anchorsByDefId) {
    if (timeframe != Timeframe.DAILY) {
      return false;
    }
    for (IndicatorDefinition definition : definitions) {
      IndicatorAnchor anchor =
          anchorsByDefId.getOrDefault(definition.getIndicatorDefinitionId(), IndicatorAnchor.EMPTY);
      if (!anchor.hasLatest()) {
        return false;
      }
      Indicator indicator = indicatorRegistry.get(definition.getType());
      if (!anchor.latest().getPriceDate().equals(maxPriceDate)
          || !indicator.hasCompleteState(anchor.latest().getState())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true when every definition on this ticker has a valid anchor row, complete persisted
   * state (for recursive indicators), and a date gap within {@link #MAX_TAIL_NEW_BARS}.
   */
  public boolean canUseTailWindow(
      Timeframe timeframe,
      LocalDate maxPriceDate,
      List<IndicatorDefinition> definitions,
      Map<Long, IndicatorAnchor> anchorsByDefId) {
    for (IndicatorDefinition definition : definitions) {
      IndicatorAnchor anchor =
          anchorsByDefId.getOrDefault(definition.getIndicatorDefinitionId(), IndicatorAnchor.EMPTY);
      if (!anchor.hasLatest()) {
        return false;
      }
      Indicator indicator = indicatorRegistry.get(definition.getType());
      boolean isWeeklyMidWeek =
          timeframe == Timeframe.WEEKLY && anchor.latest().getPriceDate().equals(maxPriceDate);

      if (isWeeklyMidWeek) {
        if (indicator.isStateful()
            && (!anchor.hasPrevious()
                || !indicator.hasCompleteState(anchor.previous().getState()))) {
          return false;
        }
      } else {
        if (indicator.isStateful() && !indicator.hasCompleteState(anchor.latest().getState())) {
          return false;
        }
        if (dateGap(timeframe, anchor.latest().getPriceDate(), maxPriceDate) > MAX_TAIL_NEW_BARS) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Computes inserts and/or in-place updates for a single indicator definition and appends them to
   * {@code toSave}.
   */
  @SuppressWarnings("unchecked")
  public <T extends com.alphaflow.persistence.entities.Indicator> void collectDefinitionUpdates(
      Timeframe timeframe,
      IndicatorDefinition definition,
      List<PriceBar> bars,
      IndicatorAnchor anchor,
      LocalDate maxPriceDate,
      List<T> toSave,
      BiFunction<LocalDate, IndicatorComputationPoint, T> entityFactory) {
    Indicator indicator = indicatorRegistry.get(definition.getType());
    IndicatorParams params = IndicatorParams.of(definition.getParams());

    if (!anchor.hasLatest()) {
      // Case 1: Cold start — compute full series from bar 0
      computeColdStart(indicator, params, definition, bars, toSave, entityFactory);
    } else if (timeframe == Timeframe.WEEKLY
        && anchor.latest().getPriceDate().equals(maxPriceDate)) {
      // Case 2: Weekly mid-week update (Tue-Fri) — recompute week t from t - 1 and update on diff
      computeWeeklyMidWeekUpsert(indicator, params, definition, bars, anchor, maxPriceDate, toSave);
    } else if (maxPriceDate.isAfter(anchor.latest().getPriceDate())) {
      // Case 3: New bar(s) arrived — resume incrementally or self-heal legacy null state
      computeIncrementalNewBars(
          indicator, params, definition, bars, anchor.latest(), toSave, entityFactory);
    } else if (indicator.isStateful() && !indicator.hasCompleteState(anchor.latest().getState())) {
      // Case 4: Same-day legacy row with state == null — backfill state in-place
      computeLegacyStateBackfill(indicator, params, definition, bars, anchor.latest(), toSave);
    }
  }

  private <T extends com.alphaflow.persistence.entities.Indicator> void computeColdStart(
      Indicator indicator,
      IndicatorParams params,
      IndicatorDefinition definition,
      List<PriceBar> bars,
      List<T> toSave,
      BiFunction<LocalDate, IndicatorComputationPoint, T> entityFactory) {
    Map<LocalDate, IndicatorComputationPoint> computed =
        indicator.computeStateful(bars, params, definition.getSource(), null);
    for (Map.Entry<LocalDate, IndicatorComputationPoint> entry : computed.entrySet()) {
      toSave.add(entityFactory.apply(entry.getKey(), entry.getValue()));
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends com.alphaflow.persistence.entities.Indicator> void computeWeeklyMidWeekUpsert(
      Indicator indicator,
      IndicatorParams params,
      IndicatorDefinition definition,
      List<PriceBar> bars,
      IndicatorAnchor anchor,
      LocalDate maxPriceDate,
      List<T> toSave) {
    com.alphaflow.persistence.entities.Indicator latestRow = anchor.latest();
    com.alphaflow.persistence.entities.Indicator previousRow = anchor.previous();
    Map<LocalDate, IndicatorComputationPoint> computed;

    if (indicator.isStateful()
        && anchor.hasPrevious()
        && indicator.hasCompleteState(previousRow.getState())) {
      // Resume week t from week (t - 1)'s persisted state (never compound onto week t's state)
      computed =
          indicator.computeStateful(
              List.of(bars.getLast()), params, definition.getSource(), previousRow.getState());
    } else if (!indicator.isStateful()) {
      List<PriceBar> tailSlice = sliceTail(bars, indicator.requiredLookback(params));
      computed = indicator.computeStateful(tailSlice, params, definition.getSource(), null);
    } else {
      // Fallback: full history + backfill week (t - 1)'s state so future mid-week runs are O(1)
      computed = indicator.computeStateful(bars, params, definition.getSource(), null);
      if (anchor.hasPrevious() && !indicator.hasCompleteState(previousRow.getState())) {
        IndicatorComputationPoint previousPoint = computed.get(previousRow.getPriceDate());
        if (previousPoint != null) {
          previousRow.setState(previousPoint.state());
          toSave.add((T) previousRow);
        }
      }
    }

    IndicatorComputationPoint recomputed = computed.get(maxPriceDate);
    if (recomputed != null
        && (!mapsNumericallyEqual(latestRow.getValues(), recomputed.values())
            || !mapsNumericallyEqual(latestRow.getState(), recomputed.state()))) {
      latestRow.setValues(recomputed.values());
      latestRow.setState(recomputed.state());
      toSave.add((T) latestRow);
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends com.alphaflow.persistence.entities.Indicator> void computeIncrementalNewBars(
      Indicator indicator,
      IndicatorParams params,
      IndicatorDefinition definition,
      List<PriceBar> bars,
      com.alphaflow.persistence.entities.Indicator latestRow,
      List<T> toSave,
      BiFunction<LocalDate, IndicatorComputationPoint, T> entityFactory) {
    LocalDate lastDate = latestRow.getPriceDate();
    Map<LocalDate, IndicatorComputationPoint> computed;

    if (indicator.isStateful() && indicator.hasCompleteState(latestRow.getState())) {
      computed =
          indicator.computeStateful(
              barsAfter(bars, lastDate), params, definition.getSource(), latestRow.getState());
    } else if (!indicator.isStateful()) {
      List<PriceBar> windowPlusNew =
          sliceWindowPlusNew(bars, lastDate, indicator.requiredLookback(params));
      computed = indicator.computeStateful(windowPlusNew, params, definition.getSource(), null);
    } else {
      // Legacy anchor without state: full history + backfill anchor's state in-place
      computed = indicator.computeStateful(bars, params, definition.getSource(), null);
      IndicatorComputationPoint anchorPoint = computed.get(lastDate);
      if (anchorPoint != null) {
        latestRow.setState(anchorPoint.state());
        toSave.add((T) latestRow);
      }
    }

    for (Map.Entry<LocalDate, IndicatorComputationPoint> entry : computed.entrySet()) {
      if (entry.getKey().isAfter(lastDate)) {
        toSave.add(entityFactory.apply(entry.getKey(), entry.getValue()));
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends com.alphaflow.persistence.entities.Indicator> void computeLegacyStateBackfill(
      Indicator indicator,
      IndicatorParams params,
      IndicatorDefinition definition,
      List<PriceBar> bars,
      com.alphaflow.persistence.entities.Indicator latestRow,
      List<T> toSave) {
    Map<LocalDate, IndicatorComputationPoint> computed =
        indicator.computeStateful(bars, params, definition.getSource(), null);
    IndicatorComputationPoint anchorPoint = computed.get(latestRow.getPriceDate());
    if (anchorPoint != null) {
      latestRow.setState(anchorPoint.state());
      toSave.add((T) latestRow);
    }
  }

  private long dateGap(Timeframe timeframe, LocalDate fromDate, LocalDate toDate) {
    if (timeframe == Timeframe.DAILY) {
      return ChronoUnit.DAYS.between(fromDate, toDate);
    } else if (timeframe == Timeframe.WEEKLY) {
      return ChronoUnit.WEEKS.between(fromDate, toDate);
    } else {
      log.error("Unsupported timeframe for tail window gap check: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  private List<PriceBar> sliceTail(List<PriceBar> bars, int count) {
    int startIndex = Math.max(0, bars.size() - count);
    return bars.subList(startIndex, bars.size());
  }

  private List<PriceBar> barsAfter(List<PriceBar> bars, LocalDate lastDate) {
    return bars.subList(findFirstIndexAfter(bars, lastDate), bars.size());
  }

  private List<PriceBar> sliceWindowPlusNew(
      List<PriceBar> bars, LocalDate lastDate, int requiredLookback) {
    int firstNewIndex = findFirstIndexAfter(bars, lastDate);
    int startIndex = Math.max(0, firstNewIndex - (requiredLookback - 1));
    return bars.subList(startIndex, bars.size());
  }

  int findFirstIndexAfter(List<PriceBar> bars, LocalDate lastDate) {
    for (int i = 0; i < bars.size(); i++) {
      if (bars.get(i).date().isAfter(lastDate)) {
        return i;
      }
    }
    return bars.size();
  }

  boolean mapsNumericallyEqual(Map<String, BigDecimal> left, Map<String, BigDecimal> right) {
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
