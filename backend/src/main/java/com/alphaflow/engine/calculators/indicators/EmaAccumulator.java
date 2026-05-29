package com.alphaflow.engine.calculators.indicators;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A resumable exponential moving average over a stream of input values.
 * <p>
 * Standard seeding: the first {@code period} values are accumulated and the first EMA emitted equals
 * their simple average (SMA); thereafter {@code ema = value·k + emaPrev·(1−k)} with {@code k = 2/(period+1)}.
 * All arithmetic runs at {@link IndicatorMath#INTERNAL_SCALE}.
 * <p>
 * Construct {@link #fresh(int)} for a backfill (self-seeds), or {@link #seeded(int, BigDecimal)} to
 * resume from a persisted EMA value (already past seeding) — the two paths produce identical values
 * for any bar once both are seeded, which is what makes resume bit-exact. Reused by both
 * {@link EmaIndicator} and {@link MacdIndicator} (whose three chained EMAs are each an accumulator).
 */
final class EmaAccumulator {

    private final int period;
    private final BigDecimal multiplier;

    private BigDecimal ema;                       // null until seeded
    private final List<BigDecimal> seedWindow = new ArrayList<>();

    private EmaAccumulator(int period, BigDecimal ema) {
        if (period < 1) {
            throw new IllegalArgumentException("EMA period must be >= 1, got " + period);
        }
        this.period = period;
        this.multiplier = IndicatorMath.divide(BigDecimal.valueOf(2), BigDecimal.valueOf(period + 1L));
        this.ema = ema;
    }

    /** A fresh accumulator that self-seeds from the first {@code period} values it sees. */
    static EmaAccumulator fresh(int period) {
        return new EmaAccumulator(period, null);
    }

    /** An accumulator resumed from a persisted EMA value (already past the seeding phase). */
    static EmaAccumulator seeded(int period, BigDecimal ema) {
        return new EmaAccumulator(period, IndicatorMath.internal(ema));
    }

    boolean isSeeded() {
        return ema != null;
    }

    /** The current EMA value, or {@code null} if still seeding. */
    BigDecimal current() {
        return ema;
    }

    /**
     * Feeds one input value.
     *
     * @return the EMA for this bar if defined, or empty while still accumulating the seed window.
     */
    Optional<BigDecimal> next(BigDecimal value) {
        if (ema == null) {
            seedWindow.add(value);
            if (seedWindow.size() < period) {
                return Optional.empty();
            }
            ema = IndicatorMath.average(seedWindow); // first EMA = SMA of first `period` values
            return Optional.of(ema);
        }
        // ema = value * k + emaPrev * (1 - k)
        BigDecimal next = value.multiply(multiplier)
                .add(ema.multiply(BigDecimal.ONE.subtract(multiplier)));
        ema = IndicatorMath.internal(next);
        return Optional.of(ema);
    }
}
