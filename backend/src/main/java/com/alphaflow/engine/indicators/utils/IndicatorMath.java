package com.alphaflow.engine.indicators.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;

public final class IndicatorMath {

  public static final int INTERNAL_SCALE = 12;

  public static final int PUBLISHED_SCALE = 4;

  public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

  public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private IndicatorMath() {}

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
}
