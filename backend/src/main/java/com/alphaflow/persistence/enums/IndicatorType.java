package com.alphaflow.persistence.enums;

public enum IndicatorType {
  SMA,
  EMA,
  RSI,
  MACD,
  STOCHASTIC,
  BB;

  public static IndicatorType from(String text) {
    if (text == null || text.trim().isEmpty()) {
      return null;
    }
    String normalized = text.trim().toUpperCase();
    for (IndicatorType type : values()) {
      if (type.name().equals(normalized)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown indicator type: " + text);
  }
}
