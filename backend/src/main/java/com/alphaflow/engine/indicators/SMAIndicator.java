package com.alphaflow.engine.indicators;

import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
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
public class SMAIndicator implements Indicator {

  @Override
  public IndicatorType type() {
    return IndicatorType.SMA;
  }

  @Override
  public Map<LocalDate, Map<String, BigDecimal>> compute(
      List<PriceBar> bars, IndicatorParams params, PriceSource source) {
    // 1. Retrieve & validate the period parameter (e.g., 20 or 50)
    int period = params.getInt("period");
    if (period < 1) {
      throw new IllegalArgumentException("SMA period must be >= 1. Provided: " + period);
    }
    log.debug(
        "Computing SMA indicator for {} bars, period={}, source={}", bars.size(), period, source);
    Map<LocalDate, Map<String, BigDecimal>> values = new java.util.LinkedHashMap<>();
    Deque<BigDecimal> window = new ArrayDeque<>(period);
    BigDecimal sum = BigDecimal.ZERO;

    // 2. Loop chronologically through all bars
    for (PriceBar bar : bars) {
      // Extract the target value based on PriceSource (CLOSE or VOLUME)
      BigDecimal v = bar.valueFor(source);

      // Add value to sliding window and running sum
      window.addLast(v);
      sum = sum.add(v);

      // Slide window: if window size is larger than period, drop the oldest value
      if (window.size() > period) {
        sum = sum.subtract(window.removeFirst());
      }

      // 3. Publish values when we have accumulated enough history (size == period)
      if (window.size() == period) {
        // Divide sum by period with internal scale (12 decimal places)
        BigDecimal sma = IndicatorMath.divide(sum, BigDecimal.valueOf(period));
        // Round for publication (4 decimal places) and save
        values.put(bar.date(), Map.of("value", IndicatorMath.publish(sma)));
      }
    }
    return values;
  }
}
