package com.alphaflow.engine.indicators.utils;

import com.alphaflow.engine.indicators.dtos.PriceBar;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
   * Calculates the True Range (TR) for a single bar given the previous bar.
   * TR = max(high - low, |high - prevClose|, |low - prevClose|)
   */
  public static BigDecimal calculateTrueRange(PriceBar current, PriceBar previous) {
    BigDecimal highLow = current.high().subtract(current.low()).abs();
    BigDecimal highPrevClose = current.high().subtract(previous.close()).abs();
    BigDecimal lowPrevClose = current.low().subtract(previous.close()).abs();
    return highLow.max(highPrevClose).max(lowPrevClose);
  }

  /**
   * Calculates the Average True Range (ATR) over the last {@code period} bars.
   *
   * @param bars price history
   * @param period smoothing period
   * @return ATR value scaled to 4 decimal places, or ZERO if insufficient bars
   */
  public static BigDecimal calculateAtr(List<PriceBar> bars, int period) {
    if (bars == null || bars.size() < period || period <= 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal sumTr = BigDecimal.ZERO;
    int startIndex = Math.max(1, bars.size() - period);
    int count = 0;
    for (int i = startIndex; i < bars.size(); i++) {
      PriceBar current = bars.get(i);
      PriceBar previous = bars.get(i - 1);
      sumTr = sumTr.add(calculateTrueRange(current, previous));
      count++;
    }
    if (count == 0) {
      return BigDecimal.ZERO;
    }
    return sumTr.divide(BigDecimal.valueOf(count), PUBLISHED_SCALE, ROUNDING);
  }
}
