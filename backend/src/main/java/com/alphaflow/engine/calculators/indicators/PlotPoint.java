package com.alphaflow.engine.calculators.indicators;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One published indicator output for one bar — the engine maps these to {@code indicator_values}
 * rows. An indicator that emits multiple plots (MACD, Stochastic) produces several PlotPoints per
 * date, distinguished by {@link #outputName()}.
 */
public record PlotPoint(LocalDate date, String outputName, BigDecimal value) {
}
