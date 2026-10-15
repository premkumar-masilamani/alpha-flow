package com.alphaflow.persistence.enums;

public enum ChartPatternStatus {
  IN_PROGRESS,
  COMPLETED,
  TARGET_REACHED,
  INVALIDATED;

  public static ChartPatternStatus from(String text) {
    if (text == null || text.trim().isEmpty()) {
      return null;
    }
    String normalized = text.trim().toUpperCase();
    for (ChartPatternStatus status : values()) {
      if (status.name().equals(normalized)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown chart pattern status: " + text);
  }
}
