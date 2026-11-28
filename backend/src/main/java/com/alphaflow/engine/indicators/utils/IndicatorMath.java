package com.alphaflow.engine.indicators.utils;

import com.alphaflow.engine.indicators.dtos.PriceBar;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class IndicatorMath {

  public static final int INTERNAL_SCALE = 12;

  public static final int PUBLISHED_SCALE = 4;

  public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

  public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

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
   * Calculates True Range for a single price bar relative to its preceding bar. True Range captures
   * both intraday spread and overnight gaps as the maximum of {@code high - low}, {@code |high -
   * prevClose|}, and {@code |low - prevClose|}.
   *
   * @param currentBar bar being evaluated
   * @param previousBar immediately preceding bar, or {@code null} for the first bar in a series
   * @return True Range value at internal precision
   */
  public static BigDecimal calculateTrueRange(PriceBar currentBar, PriceBar previousBar) {
    BigDecimal highLowRange = currentBar.high().subtract(currentBar.low()).abs();
    if (previousBar != null) {
      // Expand beyond the bar's own high-low spread whenever price gaps away from prior close.
      BigDecimal highPrevClose = currentBar.high().subtract(previousBar.close()).abs();
      BigDecimal lowPrevClose = currentBar.low().subtract(previousBar.close()).abs();
      return highLowRange.max(highPrevClose).max(lowPrevClose);
    } else {
      return highLowRange;
    }
  }

  /**
   * Computes Wilder's rolling Average True Range (ATR) series in O(N) time at internal precision.
   *
   * <p>The first {@code period - 1} entries are {@code null} while accumulating the seed window.
   * Index {@code period - 1} is initialized to the simple mean of the first {@code period} True
   * Ranges, and every subsequent entry applies Wilder's recursive smoothing {@code (prevAtr *
   * (period - 1) + trueRange) / period} to decay prior volatility smoothly without window drop-off
   * discontinuities.
   *
   * @param bars chronologically ordered price bars
   * @param period Wilder smoothing period (must be positive)
   * @return rolling ATR series aligned 1-to-1 with {@code bars}, or empty list if insufficient data
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
          // Warm-up phase: accumulate True Ranges until a full seed window is available.
          initialTrueRangeSum = initialTrueRangeSum.add(trueRange);
          atrSeries.add(null);
        } else if (i == period - 1) {
          // Seed Wilder's recurrence with the unweighted mean of the initial window.
          initialTrueRangeSum = initialTrueRangeSum.add(trueRange);
          currentAtr = divide(initialTrueRangeSum, periodDecimal);
          atrSeries.add(currentAtr);
        } else {
          // Recursive Wilder smoothing weights the prior ATR by (period - 1) / period.
          currentAtr = divide(currentAtr.multiply(smoothingFactor).add(trueRange), periodDecimal);
          atrSeries.add(currentAtr);
        }
      }
      return atrSeries;
    } else {
      return List.of();
    }
  }

  /**
   * Computes a rolling Simple Moving Average (SMA) over the tail slice {@code [startIndex ..
   * items.size() - 1]} in O(window + tail) time at internal precision.
   *
   * <p>Accumulation starts at {@code Math.max(0, startIndex - period + 1)} so incremental runs
   * evaluating only the latest bar(s) avoid redundant arithmetic over historical entries while
   * producing values identical to a full-history SMA at every index {@code i >= startIndex}.
   *
   * @param items chronologically ordered input series
   * @param valueExtractor extracts the {@link BigDecimal} value to average from each element
   * @param period rolling window size (must be positive)
   * @param startIndex first index in {@code items} for which an SMA output is required
   * @param <T> element type of the input series
   * @return SMA values aligned with {@code items[startIndex .. items.size() - 1]}, or empty list
   *     when inputs are invalid or {@code startIndex >= items.size()}
   */
  public static <T> List<BigDecimal> computeSimpleMovingAverage(
      List<T> items, Function<T, BigDecimal> valueExtractor, int period, int startIndex) {
    if (items != null
        && valueExtractor != null
        && period > 0
        && startIndex >= 0
        && startIndex < items.size()) {
      int windowStart = Math.max(0, startIndex - period + 1);
      List<BigDecimal> movingAverages = new ArrayList<>(items.size() - startIndex);
      BigDecimal sum = BigDecimal.ZERO;

      for (int i = windowStart; i < items.size(); i++) {
        sum = sum.add(valueExtractor.apply(items.get(i)));
        if (i - windowStart >= period) {
          sum = sum.subtract(valueExtractor.apply(items.get(i - period)));
        }
        if (i >= startIndex) {
          int count = Math.min(i + 1, period);
          movingAverages.add(divide(sum, BigDecimal.valueOf(count)));
        }
      }
      return movingAverages;
    } else {
      return List.of();
    }
  }
}
