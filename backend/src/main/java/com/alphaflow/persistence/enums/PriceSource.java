package com.alphaflow.persistence.enums;

/**
 * The price/volume field an indicator's input series is drawn from (e.g. EMA on CLOSE,
 *
 * <p>SMA on VOLUME). Persisted as a string in {@code indicator_values.source} /
 *
 * <p>{@code indicator_state.source}. Multi-field indicators such as Stochastic read the
 *
 * <p>fields they intrinsically require regardless of this selector.
 */
public enum PriceSource {
  OPEN,

  HIGH,

  LOW,

  CLOSE,

  VOLUME
}
