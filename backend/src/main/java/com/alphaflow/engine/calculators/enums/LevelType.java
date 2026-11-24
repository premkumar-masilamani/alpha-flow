package com.alphaflow.engine.calculators.enums;

/** Structural role of a support/resistance price zone relative to price action. */
public enum LevelType {
  /** Demand zone acting as a price floor beneath current or active price action. */
  SUPPORT,
  /** Supply zone acting as a price ceiling above current or active price action. */
  RESISTANCE;

  /**
   * Parses a case-insensitive textual level type into its enum constant.
   *
   * @param text raw string representation, or null/blank
   * @return matching {@link LevelType}, or {@code null} when input is null or blank
   * @throws IllegalArgumentException if non-blank input does not match a known level type
   */
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
