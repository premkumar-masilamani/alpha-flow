package com.alphaflow.persistence.enums;

public enum PriceSource {
  CLOSE,
  VOLUME;

  public static PriceSource from(String text) {
    if (text == null || text.trim().isEmpty()) {
      return null;
    }
    String normalized = text.trim().toUpperCase();
    for (PriceSource source : values()) {
      if (source.name().equals(normalized)) {
        return source;
      }
    }
    throw new IllegalArgumentException("Unknown price source: " + text);
  }
}
