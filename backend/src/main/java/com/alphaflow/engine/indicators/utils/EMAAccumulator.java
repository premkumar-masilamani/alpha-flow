package com.alphaflow.engine.indicators.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EmaAccumulator {

  private final int period;
  private final BigDecimal multiplier;
  private final List<BigDecimal> seedWindow = new ArrayList<>();
  private BigDecimal ema; // null until seeded

  private EmaAccumulator(int period, BigDecimal ema) {
    if (period < 1) {
      throw new IllegalArgumentException("EMA period must be >= 1, got " + period);
    }
    this.period = period;
    // Calculates multiplier: k = 2 / (period + 1)
    // Run at INTERNAL_SCALE (12 decimals) using IndicatorMath.divide(...)
    this.multiplier = IndicatorMath.divide(BigDecimal.valueOf(2), BigDecimal.valueOf(period + 1L));
    this.ema = ema;
  }

  public static EmaAccumulator fresh(int period) {
    return new EmaAccumulator(period, null);
  }

  public boolean isSeeded() {
    return ema != null;
  }

  public BigDecimal current() {
    return ema;
  }

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
