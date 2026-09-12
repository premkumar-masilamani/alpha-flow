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

@Component
@Slf4j
public class BollingerBandsIndicator implements Indicator {

  private static final MathContext SQRT_CONTEXT =
      new MathContext(IndicatorMath.INTERNAL_SCALE + 4, RoundingMode.HALF_UP);

  @Override
  public IndicatorType type() {
    return IndicatorType.BB;
  }

  @Override
  public Map<LocalDate, Map<String, BigDecimal>> compute(
      List<PriceBar> bars, IndicatorParams params, PriceSource source) {
    int period = params.getInt(IndicatorParamKey.PERIOD);
    if (period < 1) {
      throw new IllegalArgumentException(
          "Bollinger Bands period must be >= 1. Provided: " + period);
    }
    int stdDevMultiplier = params.getInt(IndicatorParamKey.STD_DEV);
    if (stdDevMultiplier < 1) {
      throw new IllegalArgumentException(
          "Bollinger Bands stdDev multiplier must be >= 1. Provided: " + stdDevMultiplier);
    }

    log.debug(
        "Computing Bollinger Bands rolling indicator for {} bars, period={}, stdDev={}, source={}",
        bars.size(),
        period,
        stdDevMultiplier,
        source);

    Map<LocalDate, Map<String, BigDecimal>> values = new LinkedHashMap<>();
    Deque<BigDecimal> window = new ArrayDeque<>(period);

    BigDecimal sumPrice = BigDecimal.ZERO;
    BigDecimal sumPriceSq = BigDecimal.ZERO;

    BigDecimal periodBd = BigDecimal.valueOf(period);
    BigDecimal multiplierBd = BigDecimal.valueOf(stdDevMultiplier);

    for (PriceBar bar : bars) {
      BigDecimal price = bar.valueFor(source);
      BigDecimal priceSq = price.multiply(price);

      window.addLast(price);
      sumPrice = sumPrice.add(price);
      sumPriceSq = sumPriceSq.add(priceSq);

      if (window.size() > period) {
        BigDecimal removed = window.removeFirst();
        sumPrice = sumPrice.subtract(removed);
        sumPriceSq = sumPriceSq.subtract(removed.multiply(removed));
      }

      if (window.size() == period) {
        BigDecimal sma = IndicatorMath.divide(sumPrice, periodBd);
        BigDecimal meanSq = sma.multiply(sma);
        BigDecimal meanPriceSq = IndicatorMath.divide(sumPriceSq, periodBd);
        BigDecimal variance = meanPriceSq.subtract(meanSq);

        // Guard variance against tiny negative precision artifacts
        if (variance.signum() < 0) {
          variance = BigDecimal.ZERO;
        }

        BigDecimal stdDev = variance.sqrt(SQRT_CONTEXT);
        BigDecimal devOffset = stdDev.multiply(multiplierBd);

        BigDecimal upper = sma.add(devOffset);
        BigDecimal lower = sma.subtract(devOffset);

        BigDecimal bandwidth;
        if (sma.compareTo(BigDecimal.ZERO) != 0) {
          bandwidth = IndicatorMath.divide(upper.subtract(lower), sma);
        } else {
          bandwidth = BigDecimal.ZERO;
        }

        values.put(
            bar.date(),
            Map.of(
                IndicatorOutputKey.UPPER.getValue(), IndicatorMath.publish(upper),
                IndicatorOutputKey.MIDDLE.getValue(), IndicatorMath.publish(sma),
                IndicatorOutputKey.LOWER.getValue(), IndicatorMath.publish(lower),
                IndicatorOutputKey.BANDWIDTH.getValue(), IndicatorMath.publish(bandwidth)));
      }
    }

    return values;
  }
}
