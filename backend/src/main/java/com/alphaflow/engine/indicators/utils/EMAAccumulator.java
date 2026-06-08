package com.alphaflow.engine.indicators.utils;

import com.alphaflow.engine.indicators.EMAIndicator;
import com.alphaflow.engine.indicators.MACDIndicator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A resumable exponential moving average over a stream of input values.
 *
 * <p>Standard seeding: the first {@code period} values are accumulated and the first EMA emitted
 * equals
 *
 * <p>their simple average (SMA); thereafter {@code ema = value·k + emaPrev·(1−k)} with {@code k =
 * 2/(period+1)}.
 *
 * <p>All arithmetic runs at {@link IndicatorMath#INTERNAL_SCALE}.
 *
 * <p>Construct {@link #fresh(int)} for a backfill (self-seeds), or {@link #seeded(int, BigDecimal)}
 * to
 *
 * <p>resume from a persisted EMA value (already past seeding) — the two paths produce identical
 * values
 *
 * <p>for any bar once both are seeded, which is what makes resume bit-exact. Reused by both
 *
 * <p>{@link EMAIndicator} and {@link MACDIndicator} (whose three chained EMAs are each an
 * accumulator).
 */
public final class EMAAccumulator {

  private final int period;
  private final BigDecimal multiplier;
  private final List<BigDecimal> seedWindow = new ArrayList<>();
  private BigDecimal ema; // null until seeded

  private EMAAccumulator(int period, BigDecimal ema) {
    if (period < 1) {
      throw new IllegalArgumentException("EMA period must be >= 1, got " + period);
    }
    this.period = period;
    // Calculates multiplier: k = 2 / (period + 1)
    // Run at INTERNAL_SCALE (12 decimals) using IndicatorMath.divide(...)
    this.multiplier = IndicatorMath.divide(BigDecimal.valueOf(2), BigDecimal.valueOf(period + 1L));
    this.ema = ema;
  }

  /** A fresh accumulator that self-seeds from the first {@code period} values it sees. */
  public static EMAAccumulator fresh(int period) {
    return new EMAAccumulator(period, null);
  }

  public boolean isSeeded() {
    return ema != null;
  }

  /** The current EMA value, or {@code null} if still seeding. */
  public BigDecimal current() {
    return ema;
  }

  /**
   * Feeds one input value.
   *
   * @return the EMA for this bar if defined, or empty while still accumulating the seed window.
   */
  public Optional<BigDecimal> next(BigDecimal value) {
    if (ema == null) {
      // Collect current value into the seed window
      seedWindow.add(value);

      // If we don't have enough history, return empty
      if (seedWindow.size() < period) {
        return Optional.empty();
      }

      // On the P-th value, compute the simple average (SMA) to seed the EMA
      ema = IndicatorMath.average(seedWindow); // first EMA = SMA of first `period` values
      return Optional.of(ema);
    }

    // Formula: ema_next = value * multiplier + ema_prev * (1 - multiplier)
    BigDecimal next =
        value.multiply(multiplier).add(ema.multiply(BigDecimal.ONE.subtract(multiplier)));

    // Keep internal precision bounded to 12 decimal places
    ema = IndicatorMath.internal(next);
    return Optional.of(ema);
  }
}
