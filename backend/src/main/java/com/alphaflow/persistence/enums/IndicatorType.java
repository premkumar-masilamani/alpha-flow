package com.alphaflow.persistence.enums;

/**
 * The technical indicator family. Persisted as a string in
 *
 * <p>{@code indicator_values.indicator_type} / {@code indicator_state.indicator_type}.
 */
public enum IndicatorType {
  SMA,

  EMA,

  RSI,

  MACD,

  STOCHASTIC
}
