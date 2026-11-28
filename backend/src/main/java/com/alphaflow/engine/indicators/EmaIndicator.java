package com.alphaflow.engine.indicators;

import com.alphaflow.engine.indicators.dtos.IndicatorComputationPoint;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.EmaAccumulator;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import com.alphaflow.persistence.enums.IndicatorOutputKey;
import com.alphaflow.persistence.enums.IndicatorParamKey;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmaIndicator implements Indicator {

  public static final String STATE_EMA = "ema";

  @Override
  public IndicatorType type() {
    return IndicatorType.EMA;
  }

  @Override
  public boolean isStateful() {
    return true;
  }

  @Override
  public int requiredLookback(IndicatorParams params) {
    return params.getInt(IndicatorParamKey.PERIOD);
  }

  @Override
  public boolean hasCompleteState(Map<String, BigDecimal> state) {
    return state != null && state.get(STATE_EMA) != null;
  }

  @Override
  public Map<LocalDate, Map<String, BigDecimal>> compute(
      List<PriceBar> bars, IndicatorParams params, PriceSource source) {
    Map<LocalDate, IndicatorComputationPoint> stateful =
        computeStateful(bars, params, source, null);
    Map<LocalDate, Map<String, BigDecimal>> values = new LinkedHashMap<>(stateful.size());
    for (Map.Entry<LocalDate, IndicatorComputationPoint> entry : stateful.entrySet()) {
      values.put(entry.getKey(), entry.getValue().values());
    }
    return values;
  }

  @Override
  public Map<LocalDate, IndicatorComputationPoint> computeStateful(
      List<PriceBar> bars,
      IndicatorParams params,
      PriceSource source,
      Map<String, BigDecimal> initialState) {
    int period = params.getInt(IndicatorParamKey.PERIOD);
    log.debug(
        "Computing EMA indicator for {} bars, period={}, source={}", bars.size(), period, source);
    EmaAccumulator accumulator =
        hasCompleteState(initialState)
            ? EmaAccumulator.seeded(period, initialState.get(STATE_EMA))
            : EmaAccumulator.fresh(period);
    Map<LocalDate, IndicatorComputationPoint> points = new LinkedHashMap<>();

    for (PriceBar bar : bars) {
      Optional<BigDecimal> ema = accumulator.next(bar.valueFor(source));
      ema.ifPresent(emaVal ->
          points.put(
              bar.date(),
              IndicatorComputationPoint.stateful(
                  Map.of(IndicatorOutputKey.VALUE.getValue(), IndicatorMath.publish(emaVal)),
                  Map.of(STATE_EMA, accumulator.current()))));
    }

    return points;
  }
}
