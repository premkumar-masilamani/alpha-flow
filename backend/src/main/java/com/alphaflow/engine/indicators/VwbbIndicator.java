package com.alphaflow.engine.indicators;

import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import com.alphaflow.persistence.enums.IndicatorOutputKey;
import com.alphaflow.persistence.enums.IndicatorParamKey;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Volume-Weighted Bollinger Bands (VWBB). Computes the middle band as Volume-Weighted Moving
 * Average (VWMA) and outer bands using Volume-Weighted Standard Deviation.
 */
@Component
@Slf4j
public class VwbbIndicator implements Indicator {

  private static final MathContext SQRT_CONTEXT =
      new MathContext(IndicatorMath.INTERNAL_SCALE + 4, RoundingMode.HALF_UP);

  @Override
  public IndicatorType type() {
    return IndicatorType.VWBB;
  }

  @Override
  public Map<LocalDate, Map<String, BigDecimal>> compute(
      List<PriceBar> bars, IndicatorParams params, PriceSource source) {
    int period = params.getInt(IndicatorParamKey.PERIOD);
    if (period < 1) {
      throw new IllegalArgumentException("VWBB period must be >= 1. Provided: " + period);
    }
    log.debug(
        "Computing VWBB indicator for {} bars, period={}, source={}", bars.size(), period, source);

    int multiplier = params.getInt("stdDev", 2);

    Map<LocalDate, Map<String, BigDecimal>> values = new LinkedHashMap<>();
    Deque<PriceBar> window = new ArrayDeque<>(period);

    for (PriceBar bar : bars) {
      window.addLast(bar);
      if (window.size() > period) {
        window.removeFirst();
      }

      if (window.size() == period) {
        values.put(bar.date(), computeBandsForWindow(window, period, multiplier, source));
      }
    }

    return values;
  }

  private Map<String, BigDecimal> computeBandsForWindow(
      Collection<PriceBar> window, int period, int multiplier, PriceSource source) {
    BigDecimal sumPriceVol = BigDecimal.ZERO;
    BigDecimal sumVol = BigDecimal.ZERO;

    for (PriceBar bar : window) {
      BigDecimal p = bar.valueFor(source);
      BigDecimal v = bar.volume();
      sumPriceVol = sumPriceVol.add(p.multiply(v));
      sumVol = sumVol.add(v);
    }

    BigDecimal vwma;
    BigDecimal variance;

    if (sumVol.signum() == 0) {
      // Fallback to unweighted calculation (SMA and unweighted variance)
      BigDecimal sumPrice = BigDecimal.ZERO;
      for (PriceBar bar : window) {
        sumPrice = sumPrice.add(bar.valueFor(source));
      }
      vwma = IndicatorMath.divide(sumPrice, BigDecimal.valueOf(period));

      BigDecimal sumSqDev = BigDecimal.ZERO;
      for (PriceBar bar : window) {
        BigDecimal dev = bar.valueFor(source).subtract(vwma);
        sumSqDev = sumSqDev.add(dev.multiply(dev));
      }
      variance = IndicatorMath.divide(sumSqDev, BigDecimal.valueOf(period));
    } else {
      vwma = IndicatorMath.divide(sumPriceVol, sumVol);

      BigDecimal sumWeightedSqDev = BigDecimal.ZERO;
      for (PriceBar bar : window) {
        BigDecimal dev = bar.valueFor(source).subtract(vwma);
        BigDecimal v = bar.volume();
        sumWeightedSqDev = sumWeightedSqDev.add(v.multiply(dev.multiply(dev)));
      }
      variance = IndicatorMath.divide(sumWeightedSqDev, sumVol);
    }

    if (variance.signum() < 0) {
      variance = BigDecimal.ZERO;
    }

    BigDecimal stdDev = variance.sqrt(SQRT_CONTEXT);
    BigDecimal devOffset = stdDev.multiply(BigDecimal.valueOf(multiplier));

    BigDecimal upper = vwma.add(devOffset);
    BigDecimal lower = vwma.subtract(devOffset);

    return Map.of(
        IndicatorOutputKey.UPPER.getValue(), IndicatorMath.publish(upper),
        IndicatorOutputKey.MIDDLE.getValue(), IndicatorMath.publish(vwma),
        IndicatorOutputKey.LOWER.getValue(), IndicatorMath.publish(lower));
  }
}
