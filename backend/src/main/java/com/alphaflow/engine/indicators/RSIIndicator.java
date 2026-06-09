package com.alphaflow.engine.indicators;

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
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Relative Strength Index using Wilder's smoothing. */
@Component
@Slf4j
public class RSIIndicator implements Indicator {

  @Override
  public IndicatorType type() {
    return IndicatorType.RSI;
  }

  @Override
  public Map<LocalDate, Map<String, BigDecimal>> compute(
      List<PriceBar> bars, IndicatorParams params, PriceSource source) {
    // 1. Setup and parameter retrieval
    int period = params.getInt(IndicatorParamKey.PERIOD);
    if (period < 1) {
      throw new IllegalArgumentException("RSI period must be >= 1. Provided: " + period);
    }
    log.debug(
        "Computing RSI indicator for {} bars, period={}, source={}", bars.size(), period, source);
    BigDecimal periodBd = BigDecimal.valueOf(period);

    BigDecimal avgGain = null;
    BigDecimal avgLoss = null;
    BigDecimal prevValue = null;
    boolean seeded = false;

    List<BigDecimal> seedGains = new ArrayList<>();
    List<BigDecimal> seedLosses = new ArrayList<>();
    Map<LocalDate, Map<String, BigDecimal>> values = new java.util.LinkedHashMap<>();

    // 2. Loop chronologically through all bars
    for (PriceBar bar : bars) {
      BigDecimal value = bar.valueFor(source);
      // Skip the first bar since delta cannot be calculated without a prior value
      if (prevValue == null) {
        prevValue = value;
        continue;
      }

      // Calculate delta price changes (Gains & Losses)
      BigDecimal delta = value.subtract(prevValue);
      BigDecimal gain = delta.signum() > 0 ? delta : BigDecimal.ZERO;
      BigDecimal loss = delta.signum() < 0 ? delta.negate() : BigDecimal.ZERO;

      // 3. Seeding stage: wait to accumulate `period` deltas, then average them
      if (!seeded) {
        seedGains.add(gain);
        seedLosses.add(loss);
        if (seedGains.size() == period) {
          avgGain = IndicatorMath.average(seedGains);
          avgLoss = IndicatorMath.average(seedLosses);
          seeded = true;
          values.put(
              bar.date(), Map.of(IndicatorOutputKey.VALUE.getValue(), rsi(avgGain, avgLoss)));
        }
      } else {
        // 4. Wilder's smoothing stage: apply exponential smoothing to subsequent deltas
        avgGain = wilder(avgGain, gain, periodBd);
        avgLoss = wilder(avgLoss, loss, periodBd);
        values.put(bar.date(), Map.of(IndicatorOutputKey.VALUE.getValue(), rsi(avgGain, avgLoss)));
      }
      prevValue = value;
    }

    return values;
  }

  private static BigDecimal wilder(BigDecimal avgPrev, BigDecimal current, BigDecimal period) {
    BigDecimal smoothed = avgPrev.multiply(period.subtract(BigDecimal.ONE)).add(current);
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
