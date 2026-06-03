package com.alphaflow.persistence.enums;

/**
 * The aggregation timeframe an indicator (or price series) operates on.
 *
 * <p>Persisted as a string in {@code indicator_values.timeframe} / {@code
 * indicator_state.timeframe}.
 *
 * <p>Daily and Weekly are the only materialized timeframes today; MONTHLY would require a new
 * rollup.
 */
public enum Timeframe {
  DAILY,

  WEEKLY
}
