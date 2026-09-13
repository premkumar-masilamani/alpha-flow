package com.alphaflow.persistence.enums;

public enum ChartPatternType {
  // Reversals
  DOUBLE_TOP("DTP", "Double Top", PatternSentiment.BEARISH_REVERSAL),
  DOUBLE_BOTTOM("DBM", "Double Bottom", PatternSentiment.BULLISH_REVERSAL),
  TRIPLE_TOP("TTP", "Triple Top", PatternSentiment.BEARISH_REVERSAL),
  TRIPLE_BOTTOM("TBM", "Triple Bottom", PatternSentiment.BULLISH_REVERSAL),
  HEAD_AND_SHOULDERS("HNS", "Head and Shoulders", PatternSentiment.BEARISH_REVERSAL),
  INVERSE_HEAD_AND_SHOULDERS(
      "IHS", "Inverse Head and Shoulders", PatternSentiment.BULLISH_REVERSAL),
  RISING_WEDGE("RWG", "Rising Wedge", PatternSentiment.BEARISH_REVERSAL),
  FALLING_WEDGE("FWG", "Falling Wedge", PatternSentiment.BULLISH_REVERSAL),

  // Continuations
  ASCENDING_TRIANGLE("AST", "Ascending Triangle", PatternSentiment.BULLISH_CONTINUATION),
  DESCENDING_TRIANGLE("DST", "Descending Triangle", PatternSentiment.BEARISH_CONTINUATION),
  SYMMETRICAL_TRIANGLE("SYT", "Symmetrical Triangle", PatternSentiment.BULLISH_CONTINUATION),
  CUP_AND_HANDLE("CPH", "Cup and Handle", PatternSentiment.BULLISH_CONTINUATION);

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
