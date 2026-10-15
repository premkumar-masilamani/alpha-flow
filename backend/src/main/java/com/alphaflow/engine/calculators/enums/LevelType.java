package com.alphaflow.engine.calculators.enums;

public enum LevelType {
  SUPPORT,
  RESISTANCE;

  public static LevelType from(String text) {
    if (text == null || text.trim().isEmpty()) {
      return null;
    }
    String normalized = text.trim().toUpperCase();
    for (LevelType levelType : values()) {
      if (levelType.name().equals(normalized)) {
        return levelType;
      }
    }
    throw new IllegalArgumentException("Unknown level type: " + text);
  }
}
