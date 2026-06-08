package com.alphaflow.engine.calculators.indicators;

import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Simple moving average over a configurable source field (e.g. SMA-20 on volume). */
@Component
@Slf4j
public class SmaIndicator implements Indicator {

  @Override
  public IndicatorType type() {
    return IndicatorType.SMA;
  }

  @Override
  public Map<LocalDate, Map<String, BigDecimal>> compute(
      List<PriceBar> bars, IndicatorParams params, PriceSource source) {
    int period = params.getInt("period");
    if (period < 1) {
      throw new IllegalArgumentException("SMA period must be >= 1. Provided: " + period);
    }
    log.debug(
        "Computing SMA indicator for {} bars, period={}, source={}", bars.size(), period, source);
    Map<LocalDate, Map<String, BigDecimal>> values = new java.util.LinkedHashMap<>();
    Deque<BigDecimal> window = new ArrayDeque<>(period);
    BigDecimal sum = BigDecimal.ZERO;

    for (PriceBar bar : bars) {
      BigDecimal v = bar.valueFor(source);
      window.addLast(v);
      sum = sum.add(v);
      if (window.size() > period) {
        sum = sum.subtract(window.removeFirst());
      }
      if (window.size() == period) {
        BigDecimal sma = IndicatorMath.divide(sum, BigDecimal.valueOf(period));
        values.put(bar.date(), Map.of("value", IndicatorMath.publish(sma)));
      }
    }
    return values;
  }
}
