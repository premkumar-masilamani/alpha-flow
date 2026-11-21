package com.alphaflow.engine.indicators.utils;

import com.alphaflow.engine.indicators.dtos.PriceBar;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class IndicatorMath {

  public static final int INTERNAL_SCALE = 12;

  public static final int PUBLISHED_SCALE = 4;

  public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

  public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private IndicatorMath() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static BigDecimal internal(BigDecimal value) {
    return value.setScale(INTERNAL_SCALE, ROUNDING);
  }

  public static BigDecimal publish(BigDecimal value) {
    return value.setScale(PUBLISHED_SCALE, ROUNDING);
  }

  public static BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
    return numerator.divide(denominator, INTERNAL_SCALE, ROUNDING);
  }

  public static BigDecimal average(List<BigDecimal> values) {
    BigDecimal sum = BigDecimal.ZERO;
    for (BigDecimal value : values) {
      sum = sum.add(value);
    }
    return divide(sum, BigDecimal.valueOf(values.size()));
  }

  public static BigDecimal max(Collection<BigDecimal> values) {
    BigDecimal maxVal = null;
    for (BigDecimal value : values) {
      if (maxVal == null || value.compareTo(maxVal) > 0) {
        maxVal = value;
      }
    }
    return maxVal;
  }

  public static BigDecimal min(Collection<BigDecimal> values) {
    BigDecimal minVal = null;
    for (BigDecimal value : values) {
      if (minVal == null || value.compareTo(minVal) < 0) {
        minVal = value;
      }
    }
    return minVal;
  }

  /**
   * Calculates True Range for a single price bar relative to its previous bar.
   *
   * @param currentBar current bar
   * @param previousBar previous bar, or null for the first bar in a series
   * @return True Range value
   */
  public static BigDecimal calculateTrueRange(PriceBar currentBar, PriceBar previousBar) {
    BigDecimal highLowRange = currentBar.high().subtract(currentBar.low()).abs();
    if (previousBar != null) {
      BigDecimal highPrevClose = currentBar.high().subtract(previousBar.close()).abs();
      BigDecimal lowPrevClose = currentBar.low().subtract(previousBar.close()).abs();
      return highLowRange.max(highPrevClose).max(lowPrevClose);
    } else {
      return highLowRange;
    }
  }

  /**
   * Computes Wilder's rolling Average True Range (ATR) series in O(N) time at internal precision.
   * Indices prior to {@code period - 1} are {@code null}.
   *
   * @param bars chronologically ordered price bars
   * @param period Wilder smoothing period (must be positive)
   * @return rolling ATR series matching {@code bars.size()}, or empty list if insufficient data
   */
  public static List<BigDecimal> calculateWilderAtrSeries(List<PriceBar> bars, int period) {
    if (bars != null && period > 0 && bars.size() >= period) {
      List<BigDecimal> atrSeries = new ArrayList<>(bars.size());
      BigDecimal initialTrueRangeSum = BigDecimal.ZERO;
      BigDecimal periodDecimal = BigDecimal.valueOf(period);
      BigDecimal smoothingFactor = BigDecimal.valueOf(period - 1L);
      BigDecimal currentAtr = BigDecimal.ZERO;

      for (int i = 0; i < bars.size(); i++) {
        PriceBar currentBar = bars.get(i);
        PriceBar previousBar = (i > 0) ? bars.get(i - 1) : null;
        BigDecimal trueRange = calculateTrueRange(currentBar, previousBar);

        if (i < period - 1) {
          initialTrueRangeSum = initialTrueRangeSum.add(trueRange);
          atrSeries.add(null);
        } else if (i == period - 1) {
          initialTrueRangeSum = initialTrueRangeSum.add(trueRange);
          currentAtr = divide(initialTrueRangeSum, periodDecimal);
          atrSeries.add(currentAtr);
        } else {
          currentAtr = divide(currentAtr.multiply(smoothingFactor).add(trueRange), periodDecimal);
          atrSeries.add(currentAtr);
        }
      }
      return atrSeries;
    } else {
      return List.of();
    }
  }
}
