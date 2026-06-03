package com.alphaflow.engine.calculators.indicators;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Shared numeric conventions for the hand-rolled indicators.
 *
 * <p>All recursive/intermediate arithmetic runs at {@link #INTERNAL_SCALE} so that
 * resume-from-state is deterministic and bit-exact: the running state persisted in {@code
 * indicator_state.internals} carries full internal precision (string-encoded), while values
 * published to {@code indicator_values} are rounded to {@link #PUBLISHED_SCALE} to match the {@code
 * numeric(18,4)} column. Using the same scale and rounding everywhere is what guarantees a resumed
 * run reproduces a full recompute exactly.
 */
public final class IndicatorMath {

  /** Precision for internal/running values (EMA state, Wilder averages, intermediate smoothing). */
  public static final int INTERNAL_SCALE = 12;

  /** Precision for published plot values; matches {@code numeric(18,4)}. */
  public static final int PUBLISHED_SCALE = 4;

  public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

  public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private IndicatorMath() {}

  /**
   * Rounds an internal value to {@link #INTERNAL_SCALE} to keep scale bounded and deterministic.
   */
  public static BigDecimal internal(BigDecimal value) {
    return value.setScale(INTERNAL_SCALE, ROUNDING);
  }

  /** Rounds a value for publication to {@code indicator_values}. */
  public static BigDecimal publish(BigDecimal value) {
    return value.setScale(PUBLISHED_SCALE, ROUNDING);
  }

  /** Internal-scale division. */
  public static BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
    return numerator.divide(denominator, INTERNAL_SCALE, ROUNDING);
  }

  /** Simple average of {@code values} at internal scale. */
  public static BigDecimal average(List<BigDecimal> values) {
    BigDecimal sum = BigDecimal.ZERO;
    for (BigDecimal v : values) {
      sum = sum.add(v);
    }
    return divide(sum, BigDecimal.valueOf(values.size()));
  }
}
