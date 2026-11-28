package com.alphaflow.engine.indicators;

import com.alphaflow.engine.indicators.dtos.IndicatorComputationPoint;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import com.alphaflow.persistence.enums.IndicatorOutputKey;
import com.alphaflow.persistence.enums.IndicatorParamKey;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RsiIndicator implements Indicator {

  public static final String STATE_AVG_GAIN = "avgGain";
  public static final String STATE_AVG_LOSS = "avgLoss";
  public static final String STATE_PREV_VALUE = "prevValue";

  @Override
  public IndicatorType type() {
    return IndicatorType.RSI;
  }

  @Override
  public boolean isStateful() {
    return true;
  }

  @Override
  public int requiredLookback(IndicatorParams params) {
    return params.getInt(IndicatorParamKey.PERIOD) + 1;
  }

  @Override
  public boolean hasCompleteState(Map<String, BigDecimal> state) {
    return state != null
        && state.get(STATE_AVG_GAIN) != null
        && state.get(STATE_AVG_LOSS) != null
        && state.get(STATE_PREV_VALUE) != null;
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
    if (period < 1) {
      throw new IllegalArgumentException("RSI period must be >= 1. Provided: " + period);
    }
    log.debug(
        "Computing RSI indicator for {} bars, period={}, source={}", bars.size(), period, source);
    BigDecimal periodBd = BigDecimal.valueOf(period);
    BigDecimal periodMinusOne = periodBd.subtract(BigDecimal.ONE);

    BigDecimal avgGain = null;
    BigDecimal avgLoss = null;
    BigDecimal prevValue = null;
    boolean seeded = false;

    if (hasCompleteState(initialState)) {
      avgGain = initialState.get(STATE_AVG_GAIN);
      avgLoss = initialState.get(STATE_AVG_LOSS);
      prevValue = initialState.get(STATE_PREV_VALUE);
      seeded = true;
    }

    List<BigDecimal> seedGains = new ArrayList<>();
    List<BigDecimal> seedLosses = new ArrayList<>();
    Map<LocalDate, IndicatorComputationPoint> points = new LinkedHashMap<>();

    for (PriceBar bar : bars) {
      BigDecimal value = bar.valueFor(source);
      if (prevValue == null) {
        prevValue = value;
        continue;
      }

      BigDecimal delta = value.subtract(prevValue);
      BigDecimal gain = delta.signum() > 0 ? delta : BigDecimal.ZERO;
      BigDecimal loss = delta.signum() < 0 ? delta.negate() : BigDecimal.ZERO;

      if (!seeded) {
        seedGains.add(gain);
        seedLosses.add(loss);
        if (seedGains.size() == period) {
          avgGain = IndicatorMath.average(seedGains);
          avgLoss = IndicatorMath.average(seedLosses);
          seeded = true;
          points.put(
              bar.date(),
              IndicatorComputationPoint.stateful(
                  Map.of(IndicatorOutputKey.VALUE.getValue(), rsi(avgGain, avgLoss)),
                  Map.of(
                      STATE_AVG_GAIN, avgGain,
                      STATE_AVG_LOSS, avgLoss,
                      STATE_PREV_VALUE, value)));
        }
      } else {
        avgGain = wilder(avgGain, gain, periodBd, periodMinusOne);
        avgLoss = wilder(avgLoss, loss, periodBd, periodMinusOne);
        points.put(
            bar.date(),
            IndicatorComputationPoint.stateful(
                Map.of(IndicatorOutputKey.VALUE.getValue(), rsi(avgGain, avgLoss)),
                Map.of(
                    STATE_AVG_GAIN, avgGain,
                    STATE_AVG_LOSS, avgLoss,
                    STATE_PREV_VALUE, value)));
      }
      prevValue = value;
    }

    return points;
  }

  private static BigDecimal wilder(
      BigDecimal avgPrev, BigDecimal current, BigDecimal period, BigDecimal periodMinusOne) {
    BigDecimal smoothed = avgPrev.multiply(periodMinusOne).add(current);
    return IndicatorMath.divide(smoothed, period);
  }

  private static BigDecimal rsi(BigDecimal avgGain, BigDecimal avgLoss) {
    if (avgLoss.signum() == 0) {
      return IndicatorMath.publish(IndicatorMath.HUNDRED);
    }
    BigDecimal rs = IndicatorMath.divide(avgGain, avgLoss);
    BigDecimal rsi =
        IndicatorMath.HUNDRED.subtract(
            IndicatorMath.divide(IndicatorMath.HUNDRED, BigDecimal.ONE.add(rs)));
    return IndicatorMath.publish(rsi);
  }
}
