package com.alphaflow.persistence.enums;

public enum CandlestickPattern {
  BULLISH_MARUBOZU("MBZ", "Bullish Marubozu", PatternSentiment.BULL),
  BULLISH_PIERCING("PRC", "Bullish Piercing", PatternSentiment.BULL),
  BULLISH_ENGULFING("ENG", "Bullish Engulfing", PatternSentiment.BULL),
  HAMMER("HAM", "Hammer", PatternSentiment.BULL),
  MORNING_STAR("MNS", "Morning Star", PatternSentiment.BULL),

  BEARISH_MARUBOZU("MBZ", "Bearish Marubozu", PatternSentiment.BEAR),
  BEARISH_PIERCING("PRC", "Bearish Piercing", PatternSentiment.BEAR),
  BEARISH_ENGULFING("ENG", "Bearish Engulfing", PatternSentiment.BEAR),
  INVERTED_HAMMER("IVH", "Inverted Hammer", PatternSentiment.BEAR),
  EVENING_STAR("EVS", "Evening Star", PatternSentiment.BEAR),
  HANGING_MAN("HNG", "Hanging Man", PatternSentiment.BEAR);

  private final String shortName;
  private final String longName;
  private final PatternSentiment sentiment;

  CandlestickPattern(String shortName, String longName, PatternSentiment sentiment) {
    this.shortName = shortName;
    this.longName = longName;
    this.sentiment = sentiment;
  }

  public String getShortName() {
    return shortName;
  }

  public String getLongName() {
    return longName;
  }

  public PatternSentiment getSentiment() {
    return sentiment;
  }
}
