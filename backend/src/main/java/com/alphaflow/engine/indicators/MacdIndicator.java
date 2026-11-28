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
public class MacdIndicator implements Indicator {

  public static final String STATE_FAST_EMA = "fastEma";
  public static final String STATE_SLOW_EMA = "slowEma";
  public static final String STATE_SIGNAL_EMA = "signalEma";

  @Override
  public IndicatorType type() {
    return IndicatorType.MACD;
  }

  @Override
  public boolean isStateful() {
    return true;
  }

  @Override
  public int requiredLookback(IndicatorParams params) {
    return params.getInt(IndicatorParamKey.SLOW) + params.getInt(IndicatorParamKey.SIGNAL, 9) - 1;
  }

  @Override
  public boolean hasCompleteState(Map<String, BigDecimal> state) {
    return state != null
        && state.get(STATE_FAST_EMA) != null
        && state.get(STATE_SLOW_EMA) != null
        && state.get(STATE_SIGNAL_EMA) != null;
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
    int fastPeriod = params.getInt(IndicatorParamKey.FAST);
    int slowPeriod = params.getInt(IndicatorParamKey.SLOW);
    int signalPeriod = params.getInt(IndicatorParamKey.SIGNAL, 9);

    log.debug(
        "Computing MACD indicator for {} bars, fast={}, slow={}, signal={}, source={}",
        bars.size(),
        fastPeriod,
        slowPeriod,
        signalPeriod,
        source);

    EmaAccumulator fast;
    EmaAccumulator slow;
    EmaAccumulator signal;
    if (hasCompleteState(initialState)) {
      fast = EmaAccumulator.seeded(fastPeriod, initialState.get(STATE_FAST_EMA));
      slow = EmaAccumulator.seeded(slowPeriod, initialState.get(STATE_SLOW_EMA));
      signal = EmaAccumulator.seeded(signalPeriod, initialState.get(STATE_SIGNAL_EMA));
    } else {
      fast = EmaAccumulator.fresh(fastPeriod);
      slow = EmaAccumulator.fresh(slowPeriod);
      signal = EmaAccumulator.fresh(signalPeriod);
    }

    Map<LocalDate, IndicatorComputationPoint> points = new LinkedHashMap<>();

    for (PriceBar bar : bars) {
      BigDecimal value = bar.valueFor(source);
      fast.next(value);
      slow.next(value);

      if (!fast.isSeeded() || !slow.isSeeded()) {
        continue;
      }

      BigDecimal macd = IndicatorMath.internal(fast.current().subtract(slow.current()));
      Map<String, BigDecimal> barValues = new LinkedHashMap<>();
      barValues.put(IndicatorOutputKey.MACD.getValue(), IndicatorMath.publish(macd));

      Map<String, BigDecimal> barState = new LinkedHashMap<>();
      barState.put(STATE_FAST_EMA, fast.current());
      barState.put(STATE_SLOW_EMA, slow.current());

      Optional<BigDecimal> signalEma = signal.next(macd);

      if (signalEma.isPresent()) {
        BigDecimal signalVal = signalEma.get();
        barValues.put(IndicatorOutputKey.SIGNAL.getValue(), IndicatorMath.publish(signalVal));

        BigDecimal histogram = IndicatorMath.internal(macd.subtract(signalVal));
        barValues.put(IndicatorOutputKey.HISTOGRAM.getValue(), IndicatorMath.publish(histogram));
        barState.put(STATE_SIGNAL_EMA, signal.current());
      }
      points.put(bar.date(), IndicatorComputationPoint.stateful(barValues, barState));
    }

    return points;
  }
}
