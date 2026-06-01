package com.alphaflow.engine.calculators.indicators;

import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Slow Stochastic oscillator (standard "(k, kSmooth, dSmooth)", e.g. 14,3,3).
 * <ul>
 *   <li>rawK = (close − lowestLow) / (highestHigh − lowestLow) × 100 over the {@code k}-bar range</li>
 *   <li>{@code k} (%K) = SMA(rawK, kSmooth)</li>
 *   <li>{@code d} (%D) = SMA(%K, dSmooth)</li>
 * </ul>
 * Range is taken from the {@code high}/{@code low} fields (not the configured source); when the
 * range is zero, rawK is defined as 0 to avoid division by zero. Windowed and non-recursive
 * ({@code newStateJson == null}); resumed purely from the price bars the caller loads.
 */
@Component
public class StochasticIndicator implements Indicator {

    private static BigDecimal max(Deque<BigDecimal> values) {
        BigDecimal m = null;
        for (BigDecimal v : values) {
            if (m == null || v.compareTo(m) > 0) {
                m = v;
            }
        }
        return m;
    }

    private static BigDecimal min(Deque<BigDecimal> values) {
        BigDecimal m = null;
        for (BigDecimal v : values) {
            if (m == null || v.compareTo(m) < 0) {
                m = v;
            }
        }
        return m;
    }

    @Override
    public IndicatorType type() {
        return IndicatorType.STOCHASTIC;
    }

    @Override
    public boolean requiresState() {
        return false; // windowed: reconstructed from the last k + smoothing bars
    }

    @Override
    public IndicatorResult compute(List<PriceBar> bars, String priorStateJson, IndicatorParams params, PriceSource source) {
        int k = params.getInt("k");
        int kSmooth = params.getInt("kSmooth");
        int dSmooth = params.getInt("dSmooth");
        if (k < 1) {
            throw new IllegalArgumentException("Stochastic k must be >= 1. Provided: " + k);
        }
        if (kSmooth < 1) {
            throw new IllegalArgumentException("Stochastic kSmooth must be >= 1. Provided: " + kSmooth);
        }
        if (dSmooth < 1) {
            throw new IllegalArgumentException("Stochastic dSmooth must be >= 1. Provided: " + dSmooth);
        }

        Deque<BigDecimal> highs = new ArrayDeque<>(k);
        Deque<BigDecimal> lows = new ArrayDeque<>(k);
        Deque<BigDecimal> rawKWindow = new ArrayDeque<>(kSmooth);
        Deque<BigDecimal> kWindow = new ArrayDeque<>(dSmooth);

        List<PlotPoint> values = new ArrayList<>();
        for (PriceBar bar : bars) {
            highs.addLast(bar.high());
            lows.addLast(bar.low());
            if (highs.size() > k) {
                highs.removeFirst();
                lows.removeFirst();
            }
            if (highs.size() < k) {
                continue;
            }

            BigDecimal highestHigh = max(highs);
            BigDecimal lowestLow = min(lows);
            BigDecimal range = highestHigh.subtract(lowestLow);
            BigDecimal rawK = range.signum() == 0
                    ? BigDecimal.ZERO
                    : IndicatorMath.internal(
                    IndicatorMath.divide(bar.close().subtract(lowestLow), range).multiply(IndicatorMath.HUNDRED));

            rawKWindow.addLast(rawK);
            if (rawKWindow.size() > kSmooth) {
                rawKWindow.removeFirst();
            }
            if (rawKWindow.size() < kSmooth) {
                continue;
            }

            BigDecimal kValue = IndicatorMath.average(new ArrayList<>(rawKWindow));
            values.add(new PlotPoint(bar.date(), "k", IndicatorMath.publish(kValue)));

            kWindow.addLast(kValue);
            if (kWindow.size() > dSmooth) {
                kWindow.removeFirst();
            }
            if (kWindow.size() == dSmooth) {
                BigDecimal dValue = IndicatorMath.average(new ArrayList<>(kWindow));
                values.add(new PlotPoint(bar.date(), "d", IndicatorMath.publish(dValue)));
            }
        }
        return new IndicatorResult(values, null);
    }
}
