package com.alphaflow.common.enums;

/**
 * The aggregation timeframe an indicator (or price series) operates on.
 *
 * <p>Persisted as a string in {@code indicator_values.timeframe} / {@code
 * indicator_state.timeframe}.
 */
public enum Timeframe {
  DAILY,
  WEEKLY
}
