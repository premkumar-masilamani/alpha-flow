package com.alphaflow.engine.indicators.dtos;

import com.alphaflow.persistence.entities.Indicator;

/** Holds the two most recent persisted indicator rows (t and t - 1) for a ticker and definition. */
public record IndicatorAnchor(Indicator latest, Indicator previous) {

  public static final IndicatorAnchor EMPTY = new IndicatorAnchor(null, null);

  public boolean hasLatest() {
    return latest != null;
  }

  public boolean hasPrevious() {
    return previous != null;
  }
}
