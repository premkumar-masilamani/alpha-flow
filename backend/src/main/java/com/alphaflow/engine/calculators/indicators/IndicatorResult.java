package com.alphaflow.engine.calculators.indicators;

import java.util.List;

/**
 * Output of {@link Indicator#compute}: the plot values to upsert, and the running state to persist.
 *
 * @param values       plot values for every bar in the input where the indicator is fully defined
 *                     (no rows during warm-up), in chronological order.
 * @param newStateJson the recursive running state as of the last input bar, as a JSON object of
 *                     string-encoded decimals, or {@code null} for windowed indicators (and for
 *                     recursive indicators still inside their warm-up period). The engine persists
 *                     this to {@code indicator_state.internals}, always lagging the latest bar.
 */
public record IndicatorResult(List<PlotPoint> values, String newStateJson) {
}
