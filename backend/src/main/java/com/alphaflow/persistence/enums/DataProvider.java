package com.alphaflow.persistence.enums;

import org.apache.commons.lang3.StringUtils;

public enum DataProvider {
  YAHOO_FINANCE;

  public static DataProvider from(String text) {
    if (StringUtils.isBlank(text)) {
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
