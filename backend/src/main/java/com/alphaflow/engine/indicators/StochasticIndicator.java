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
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StochasticIndicator implements Indicator {

  @Override
  public IndicatorType type() {
    return IndicatorType.STOCHASTIC;
  }

  @Override
  public int requiredLookback(IndicatorParams params) {
    return params.getInt(IndicatorParamKey.K)
        + params.getInt(IndicatorParamKey.K_SMOOTH)
        + params.getInt(IndicatorParamKey.D_SMOOTH)
        - 2;
  }

  @Override
  public Map<LocalDate, Map<String, BigDecimal>> compute(
      List<PriceBar> bars, IndicatorParams params, PriceSource source) {
    // 1. Setup and parameter retrieval
    int k = params.getInt(IndicatorParamKey.K);
    int smoothK = params.getInt(IndicatorParamKey.K_SMOOTH);
    int smoothD = params.getInt(IndicatorParamKey.D_SMOOTH);
    if (k < 1) {
      throw new IllegalArgumentException("Stochastic k must be >= 1. Provided: " + k);
    }
    if (smoothK < 1) {
      throw new IllegalArgumentException("Stochastic kSmooth must be >= 1. Provided: " + smoothK);
    }
    if (smoothD < 1) {
      throw new IllegalArgumentException("Stochastic dSmooth must be >= 1. Provided: " + smoothD);
    }

    log.debug(
        "Computing Stochastic indicator for {} bars, k={}, kSmooth={}, dSmooth={}, source={}",
        bars.size(),
        k,
        smoothK,
        smoothD,
        source);

    // Deques representing sliding windows for tracking values
    Deque<BigDecimal> highs = new ArrayDeque<>(k);
    Deque<BigDecimal> lows = new ArrayDeque<>(k);
    Deque<BigDecimal> rawWindowK = new ArrayDeque<>(smoothK);
    Deque<BigDecimal> windowK = new ArrayDeque<>(smoothD);
    BigDecimal sumRawK = BigDecimal.ZERO;
    BigDecimal sumK = BigDecimal.ZERO;
    BigDecimal divisorK = BigDecimal.valueOf(smoothK);
    BigDecimal divisorD = BigDecimal.valueOf(smoothD);

    Map<LocalDate, Map<String, BigDecimal>> values = new LinkedHashMap<>();

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
      BigDecimal highestHigh = IndicatorMath.max(highs);
      BigDecimal lowestLow = IndicatorMath.min(lows);
      BigDecimal range = highestHigh.subtract(lowestLow);
      BigDecimal rawK =
          range.signum() == 0
              ? BigDecimal.ZERO
              : IndicatorMath.internal(
                  IndicatorMath.divide(bar.close().subtract(lowestLow), range)
                      .multiply(IndicatorMath.HUNDRED));

      // Step 3: Compute Slow %K (Output `k`) using O(1) rolling sum of rawWindowK
      rawWindowK.addLast(rawK);
      sumRawK = sumRawK.add(rawK);
      if (rawWindowK.size() > smoothK) {
        sumRawK = sumRawK.subtract(rawWindowK.removeFirst());
      }
      if (rawWindowK.size() < smoothK) {
        continue; // Skip until we have enough rawK values for smoothing
      }

      BigDecimal valueK = IndicatorMath.divide(sumRawK, divisorK);
      Map<String, BigDecimal> barValues = new LinkedHashMap<>();
      barValues.put(IndicatorOutputKey.K.getValue(), IndicatorMath.publish(valueK));

      // Step 4: Compute Slow %D (Output `d`) using O(1) rolling sum of windowK
      windowK.addLast(valueK);
      sumK = sumK.add(valueK);
      if (windowK.size() > smoothD) {
        sumK = sumK.subtract(windowK.removeFirst());
      }
      if (windowK.size() == smoothD) {
        BigDecimal valueD = IndicatorMath.divide(sumK, divisorD);
        barValues.put(IndicatorOutputKey.D.getValue(), IndicatorMath.publish(valueD));
      }
      // Step 5: Save published output mapped to bar date
      values.put(bar.date(), barValues);
    }
    return values;
  }
}
