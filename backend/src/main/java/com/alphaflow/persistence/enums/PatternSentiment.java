package com.alphaflow.persistence.enums;

public enum PatternSentiment {
  BULLISH_REVERSAL,
  BEARISH_REVERSAL,
  BULLISH_CONTINUATION,
  BEARISH_CONTINUATION;

  public static PatternSentiment from(String text) {
    if (text == null || text.trim().isEmpty()) {
      return null;
    }
    String normalized = text.trim().toUpperCase();
    for (PatternSentiment sentiment : values()) {
      if (sentiment.name().equals(normalized)) {
        return sentiment;
      }
    }
    throw new IllegalArgumentException("Unknown pattern sentiment: " + text);
  }
}
