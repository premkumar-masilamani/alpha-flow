package com.alphaflow.persistence.enums;

public enum ChartPatternType {
  // Reversals
  DOUBLE_TOP("DT", "Double Top", PatternSentiment.BEARISH_REVERSAL),
  DOUBLE_BOTTOM("DB", "Double Bottom", PatternSentiment.BULLISH_REVERSAL),
  TRIPLE_TOP("TT", "Triple Top", PatternSentiment.BEARISH_REVERSAL),
  TRIPLE_BOTTOM("TB", "Triple Bottom", PatternSentiment.BULLISH_REVERSAL),
  HEAD_AND_SHOULDERS("HNS", "Head and Shoulders", PatternSentiment.BEARISH_REVERSAL),
  INVERSE_HEAD_AND_SHOULDERS(
      "IHNS", "Inverse Head and Shoulders", PatternSentiment.BULLISH_REVERSAL),
  RISING_WEDGE("RW", "Rising Wedge", PatternSentiment.BEARISH_REVERSAL),
  FALLING_WEDGE("FW", "Falling Wedge", PatternSentiment.BULLISH_REVERSAL),

  // Continuations
  ASCENDING_TRIANGLE("AT", "Ascending Triangle", PatternSentiment.BULLISH_CONTINUATION),
  DESCENDING_TRIANGLE("DST", "Descending Triangle", PatternSentiment.BEARISH_CONTINUATION),
  SYMMETRICAL_TRIANGLE("ST", "Symmetrical Triangle", PatternSentiment.BULLISH_CONTINUATION),
  CUP_AND_HANDLE("CH", "Cup and Handle", PatternSentiment.BULLISH_CONTINUATION);

  private final String shortName;
  private final String displayName;
  private final PatternSentiment sentiment;

  ChartPatternType(String shortName, String displayName, PatternSentiment sentiment) {
    this.shortName = shortName;
    this.displayName = displayName;
    this.sentiment = sentiment;
  }

  public String getShortName() {
    return shortName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public PatternSentiment getSentiment() {
    return sentiment;
  }
}
