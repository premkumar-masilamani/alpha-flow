package com.alphaflow.persistence.enums;

import lombok.Getter;

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

  public static IndicatorOutputKey from(String text) {
    if (text == null || text.trim().isEmpty()) {
      return null;
    }
    for (IndicatorOutputKey key : values()) {
      if (key.name().equalsIgnoreCase(text.trim()) || key.value.equalsIgnoreCase(text.trim())) {
        return key;
      }
    }
    throw new IllegalArgumentException("Unknown indicator output key: " + text);
  }
}
