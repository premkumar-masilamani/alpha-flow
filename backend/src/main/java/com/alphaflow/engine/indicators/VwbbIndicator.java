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
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Volume-Weighted Bollinger Bands (VWBB). Uses Typical Price and O(1) rolling sum calculations. */
@Component
@Slf4j
public class VwbbIndicator implements Indicator {

  private static final MathContext SQRT_CONTEXT =
      new MathContext(IndicatorMath.INTERNAL_SCALE + 4, RoundingMode.HALF_UP);

  private record TypicalPriceTerms(
      BigDecimal volume,
      BigDecimal typicalPrice,
      BigDecimal weightedPrice,
      BigDecimal weightedPriceSq,
      BigDecimal typicalPriceSq) {}

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
    int stdDevMultiplier = params.getInt(IndicatorParamKey.STD_DEV);
    if (stdDevMultiplier < 1) {
      throw new IllegalArgumentException(
          "VWBB stdDev multiplier must be >= 1. Provided: " + stdDevMultiplier);
    }

    log.debug(
        "Computing VWBB rolling indicator for {} bars, period={}, stdDev={}, source={}",
        bars.size(),
        period,
        stdDevMultiplier,
        source);

    Map<LocalDate, Map<String, BigDecimal>> values = new LinkedHashMap<>();
    Deque<TypicalPriceTerms> window = new ArrayDeque<>(period);

    BigDecimal sumVol = BigDecimal.ZERO;
    BigDecimal sumPriceVol = BigDecimal.ZERO;
    BigDecimal sumPriceSqVol = BigDecimal.ZERO;

    // Fallback rolling sums
    BigDecimal sumP = BigDecimal.ZERO;
    BigDecimal sumP2 = BigDecimal.ZERO;

    BigDecimal periodBd = BigDecimal.valueOf(period);
    BigDecimal multiplierBd = BigDecimal.valueOf(stdDevMultiplier);

    for (PriceBar bar : bars) {
      // Step 1: Calculate Typical Price (TP_t) = (high + low + close) / 3.0
      BigDecimal tp =
          bar.high()
              .add(bar.low())
              .add(bar.close())
              .divide(BigDecimal.valueOf(3), IndicatorMath.INTERNAL_SCALE, RoundingMode.HALF_UP);

      BigDecimal vol = bar.volume();
      BigDecimal weightedPrice = tp.multiply(vol);
      BigDecimal weightedPriceSq = tp.multiply(tp).multiply(vol);
      BigDecimal typicalPriceSq = tp.multiply(tp);

      TypicalPriceTerms terms =
          new TypicalPriceTerms(vol, tp, weightedPrice, weightedPriceSq, typicalPriceSq);

      // Slide window and update rolling sums
      window.addLast(terms);
      sumVol = sumVol.add(vol);
      sumPriceVol = sumPriceVol.add(weightedPrice);
      sumPriceSqVol = sumPriceSqVol.add(weightedPriceSq);
      sumP = sumP.add(tp);
      sumP2 = sumP2.add(typicalPriceSq);

      if (window.size() > period) {
        TypicalPriceTerms removed = window.removeFirst();
        sumVol = sumVol.subtract(removed.volume());
        sumPriceVol = sumPriceVol.subtract(removed.weightedPrice());
        sumPriceSqVol = sumPriceSqVol.subtract(removed.weightedPriceSq());
        sumP = sumP.subtract(removed.typicalPrice());
        sumP2 = sumP2.subtract(removed.typicalPriceSq());
      }

      if (window.size() == period) {
        BigDecimal vwap;
        BigDecimal variance;

        if (sumVol.signum() == 0) {
          // Fallback to unweighted SMA and variance
          vwap = IndicatorMath.divide(sumP, periodBd);
          BigDecimal meanSq = vwap.multiply(vwap);
          variance = IndicatorMath.divide(sumP2, periodBd).subtract(meanSq);
        } else {
          // Rolling VWAP and Variance
          vwap = IndicatorMath.divide(sumPriceVol, sumVol);
          BigDecimal meanSq = vwap.multiply(vwap);
          variance = IndicatorMath.divide(sumPriceSqVol, sumVol).subtract(meanSq);
        }

        // Guard variance against small negative precision errors
        if (variance.signum() < 0) {
          variance = BigDecimal.ZERO;
        }

        BigDecimal stdDev = variance.sqrt(SQRT_CONTEXT);
        BigDecimal devOffset = stdDev.multiply(multiplierBd);

        BigDecimal upper = vwap.add(devOffset);
        BigDecimal lower = vwap.subtract(devOffset);

        BigDecimal bandwidth;
        if (vwap.compareTo(BigDecimal.ZERO) == 0) {
          bandwidth = BigDecimal.ZERO;
        } else {
          bandwidth = IndicatorMath.divide(upper.subtract(lower), vwap);
        }

        values.put(
            bar.date(),
            Map.of(
                IndicatorOutputKey.UPPER.getValue(), IndicatorMath.publish(upper),
                IndicatorOutputKey.MIDDLE.getValue(), IndicatorMath.publish(vwap),
                IndicatorOutputKey.LOWER.getValue(), IndicatorMath.publish(lower),
                IndicatorOutputKey.BANDWIDTH.getValue(), IndicatorMath.publish(bandwidth)));
      }
    }

    return values;
  }
}
