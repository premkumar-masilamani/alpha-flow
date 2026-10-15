package com.alphaflow.persistence.enums;

public enum TickerType {
  STOCK,
  INDEX;

  public static TickerType from(String text) {
    if (text == null || text.trim().isEmpty()) {
      return null;
    }
    String normalized = text.trim().toUpperCase();
    for (TickerType type : values()) {
      if (type.name().equals(normalized)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown ticker type: " + text);
  }
}
