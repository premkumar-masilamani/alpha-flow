package com.alphaflow.persistence.enums;

import lombok.Getter;

/**
 * Represents the calculated output keys stored inside the JSONB values database column of the
 * daily_indicators and weekly_indicators tables.
 */
@Getter
public enum IndicatorOutputKey {
  VALUE("value"),
  MACD("macd"),
  SIGNAL("signal"),
  HISTOGRAM("histogram"),
  K("k"),
  D("d"),
  UPPER("upper"),
  MIDDLE("middle"),
  LOWER("lower"),
  BANDWIDTH("bandwidth");

  private final String value;

  IndicatorOutputKey(String value) {
    this.value = value;
  }
}
