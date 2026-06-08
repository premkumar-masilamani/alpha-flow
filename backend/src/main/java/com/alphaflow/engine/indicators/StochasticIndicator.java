package com.alphaflow.engine.indicators;

import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Slow Stochastic oscillator (standard "(k, kSmooth, dSmooth)", e.g. 14,3,3). */
@Component
@Slf4j
public class StochasticIndicator implements Indicator {

  private static BigDecimal max(Deque<BigDecimal> values) {
    BigDecimal m = null;
    for (BigDecimal v : values) {
      if (m == null || v.compareTo(m) > 0) {
        m = v;
      }
    }
    return m;
  }

  private static BigDecimal min(Deque<BigDecimal> values) {
    BigDecimal m = null;
    for (BigDecimal v : values) {
      if (m == null || v.compareTo(m) < 0) {
        m = v;
      }
    }
    return m;
  }

  @Override
  public IndicatorType type() {
    return IndicatorType.STOCHASTIC;
  }

  @Override
  public Map<LocalDate, Map<String, BigDecimal>> compute(
      List<PriceBar> bars, IndicatorParams params, PriceSource source) {
    // 1. Setup and parameter retrieval
    int k = params.getInt("k");
    int kSmooth = params.getInt("kSmooth");
    int dSmooth = params.getInt("dSmooth");
    if (k < 1) {
      throw new IllegalArgumentException("Stochastic k must be >= 1. Provided: " + k);
    }
    if (kSmooth < 1) {
      throw new IllegalArgumentException("Stochastic kSmooth must be >= 1. Provided: " + kSmooth);
    }
    if (dSmooth < 1) {
      throw new IllegalArgumentException("Stochastic dSmooth must be >= 1. Provided: " + dSmooth);
    }

    log.debug(
        "Computing Stochastic indicator for {} bars, k={}, kSmooth={}, dSmooth={}, source={}",
        bars.size(),
        k,
        kSmooth,
        dSmooth,
        source);

    // Deques representing sliding windows for tracking values
    Deque<BigDecimal> highs = new ArrayDeque<>(k);
    Deque<BigDecimal> lows = new ArrayDeque<>(k);
    Deque<BigDecimal> rawKWindow = new ArrayDeque<>(kSmooth);
    Deque<BigDecimal> kWindow = new ArrayDeque<>(dSmooth);

    Map<LocalDate, Map<String, BigDecimal>> values = new java.util.LinkedHashMap<>();

    // 2. Loop chronologically through all bars
    for (PriceBar bar : bars) {
      // Step 1: Accumulate Highs & Lows (First k bars)
      highs.addLast(bar.high());
      lows.addLast(bar.low());
      if (highs.size() > k) {
        highs.removeFirst();
        lows.removeFirst();
      }
      if (highs.size() < k) {
        continue; // Skip until we have k bars of history
      }

      // Step 2: Compute Raw %K = ((Close - LL_k) / (HH_k - LL_k)) * 100
      BigDecimal highestHigh = max(highs);
      BigDecimal lowestLow = min(lows);
      BigDecimal range = highestHigh.subtract(lowestLow);
      BigDecimal rawK =
          range.signum() == 0
              ? BigDecimal.ZERO
              : IndicatorMath.internal(
                  IndicatorMath.divide(bar.close().subtract(lowestLow), range)
                      .multiply(IndicatorMath.HUNDRED));

      // Step 3: Compute Slow %K (Output `k`) using simple averaging of rawKWindow
      rawKWindow.addLast(rawK);
      if (rawKWindow.size() > kSmooth) {
        rawKWindow.removeFirst();
      }
      if (rawKWindow.size() < kSmooth) {
        continue; // Skip until we have enough rawK values for smoothing
      }

      BigDecimal kValue = IndicatorMath.average(new ArrayList<>(rawKWindow));
      Map<String, BigDecimal> barValues = new LinkedHashMap<>();
      barValues.put("k", IndicatorMath.publish(kValue));

      // Step 4: Compute Slow %D (Output `d`) using simple averaging of kWindow
      kWindow.addLast(kValue);
      if (kWindow.size() > dSmooth) {
        kWindow.removeFirst();
      }
      if (kWindow.size() == dSmooth) {
        BigDecimal dValue = IndicatorMath.average(new ArrayList<>(kWindow));
        barValues.put("d", IndicatorMath.publish(dValue));
      }
      // Step 5: Save published output mapped to bar date
      values.put(bar.date(), barValues);
    }
    return values;
  }
}
