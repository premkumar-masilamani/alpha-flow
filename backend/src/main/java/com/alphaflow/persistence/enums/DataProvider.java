package com.alphaflow.persistence.enums;

public enum DataProvider {
  YAHOO_FINANCE,
  ANGEL_ONE;

  public static DataProvider from(String text) {
    if (text == null || text.trim().isEmpty()) {
      return null;
    }
    String normalized = text.trim().toUpperCase();
    for (DataProvider provider : values()) {
      if (provider.name().equals(normalized)) {
        return provider;
      }
    }
    throw new IllegalArgumentException("Unknown data provider: " + text);
  }
}
