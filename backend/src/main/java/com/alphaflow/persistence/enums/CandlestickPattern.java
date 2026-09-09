package com.alphaflow.persistence.enums;

public enum CandlestickPattern {
  // A. Bullish Reversals
  LONG_WHITE_BODY("LBD", "Long White Body", PatternSentiment.BULLISH_REVERSAL),
  HAMMER("HAM", "Hammer", PatternSentiment.BULLISH_REVERSAL),
  INVERTED_HAMMER("IVH", "Inverted Hammer", PatternSentiment.BULLISH_REVERSAL),
  BULLISH_BELT_HOLD("BTH", "Bullish Belt Hold", PatternSentiment.BULLISH_REVERSAL),
  BULLISH_ENGULFING("ENG", "Bullish Engulfing", PatternSentiment.BULLISH_REVERSAL),
  BULLISH_HARAMI("HRM", "Bullish Harami", PatternSentiment.BULLISH_REVERSAL),
  BULLISH_HARAMI_CROSS("HMC", "Bullish Harami Cross", PatternSentiment.BULLISH_REVERSAL),
  PIERCING_LINE("PRC", "Piercing Line", PatternSentiment.BULLISH_REVERSAL),
  BULLISH_DOJI_STAR("DJS", "Bullish Doji Star", PatternSentiment.BULLISH_REVERSAL),
  BULLISH_MEETING_LINES("MTG", "Bullish Meeting Lines", PatternSentiment.BULLISH_REVERSAL),
  THREE_WHITE_SOLDIERS("TWS", "Three White Soldiers", PatternSentiment.BULLISH_REVERSAL),
  MORNING_STAR("MNS", "Morning Star", PatternSentiment.BULLISH_REVERSAL),
  MORNING_DOJI_STAR("MDS", "Morning Doji Star", PatternSentiment.BULLISH_REVERSAL),
  BULLISH_ABANDONED_BABY("ABB", "Bullish Abandoned Baby", PatternSentiment.BULLISH_REVERSAL),
  BULLISH_TRI_STAR("TRS", "Bullish Tri-Star", PatternSentiment.BULLISH_REVERSAL),
  BULLISH_BREAKAWAY("BKA", "Bullish Breakaway", PatternSentiment.BULLISH_REVERSAL),
  THREE_INSIDE_UP("TIU", "Three Inside Up", PatternSentiment.BULLISH_REVERSAL),
  THREE_OUTSIDE_UP("TOU", "Three Outside Up", PatternSentiment.BULLISH_REVERSAL),
  BULLISH_KICKING("KCK", "Bullish Kicking", PatternSentiment.BULLISH_REVERSAL),
  UNIQUE_THREE_RIVERS_BOTTOM(
      "UTR", "Unique Three Rivers Bottom", PatternSentiment.BULLISH_REVERSAL),
  THREE_STARS_IN_SOUTH("TSS", "Three Stars in the South", PatternSentiment.BULLISH_REVERSAL),
  CONCEALING_SWALLOW("CSW", "Concealing Swallow", PatternSentiment.BULLISH_REVERSAL),
  BULLISH_STICK_SANDWICH("STK", "Bullish Stick Sandwich", PatternSentiment.BULLISH_REVERSAL),
  HOMING_PIGEON("HMP", "Homing Pigeon", PatternSentiment.BULLISH_REVERSAL),
  LADDER_BOTTOM("LDB", "Ladder Bottom", PatternSentiment.BULLISH_REVERSAL),
  MATCHING_LOW("MTL", "Matching Low", PatternSentiment.BULLISH_REVERSAL),

  // B. Bearish Reversals
  LONG_BLACK_BODY("LBD", "Long Black Body", PatternSentiment.BEARISH_REVERSAL),
  HANGING_MAN("HNG", "Hanging Man", PatternSentiment.BEARISH_REVERSAL),
  SHOOTING_STAR("SST", "Shooting Star", PatternSentiment.BEARISH_REVERSAL),
  BEARISH_BELT_HOLD("BTH", "Bearish Belt Hold", PatternSentiment.BEARISH_REVERSAL),
  BEARISH_ENGULFING("ENG", "Bearish Engulfing", PatternSentiment.BEARISH_REVERSAL),
  BEARISH_HARAMI("HRM", "Bearish Harami", PatternSentiment.BEARISH_REVERSAL),
  BEARISH_HARAMI_CROSS("HMC", "Bearish Harami Cross", PatternSentiment.BEARISH_REVERSAL),
  DARK_CLOUD_COVER("DCC", "Dark Cloud Cover", PatternSentiment.BEARISH_REVERSAL),
  BEARISH_DOJI_STAR("DJS", "Bearish Doji Star", PatternSentiment.BEARISH_REVERSAL),
  BEARISH_MEETING_LINES("MTG", "Bearish Meeting Lines", PatternSentiment.BEARISH_REVERSAL),
  THREE_BLACK_CROWS("TBC", "Three Black Crows", PatternSentiment.BEARISH_REVERSAL),
  EVENING_STAR("EVS", "Evening Star", PatternSentiment.BEARISH_REVERSAL),
  EVENING_DOJI_STAR("EDS", "Evening Doji Star", PatternSentiment.BEARISH_REVERSAL),
  BEARISH_ABANDONED_BABY("ABB", "Bearish Abandoned Baby", PatternSentiment.BEARISH_REVERSAL),
  BEARISH_TRI_STAR("TRS", "Bearish Tri-Star", PatternSentiment.BEARISH_REVERSAL),
  BEARISH_BREAKAWAY("BKA", "Bearish Breakaway", PatternSentiment.BEARISH_REVERSAL),
  THREE_INSIDE_DOWN("TID", "Three Inside Down", PatternSentiment.BEARISH_REVERSAL),
  THREE_OUTSIDE_DOWN("TOD", "Three Outside Down", PatternSentiment.BEARISH_REVERSAL),
  BEARISH_KICKING("KCK", "Bearish Kicking", PatternSentiment.BEARISH_REVERSAL),
  LADDER_TOP("LDT", "Ladder Top", PatternSentiment.BEARISH_REVERSAL),
  MATCHING_HIGH("MTH", "Matching High", PatternSentiment.BEARISH_REVERSAL),
  UPSIDE_GAP_TWO_CROWS("UGT", "Upside Gap Two Crows", PatternSentiment.BEARISH_REVERSAL),
  IDENTICAL_THREE_CROWS("ITC", "Identical Three Crows", PatternSentiment.BEARISH_REVERSAL),
  DELIBERATION("DLB", "Deliberation", PatternSentiment.BEARISH_REVERSAL),
  ADVANCE_BLOCK("AVB", "Advance Block", PatternSentiment.BEARISH_REVERSAL),
  TWO_CROWS("TWC", "Two Crows", PatternSentiment.BEARISH_REVERSAL),

  // C. Bullish Continuation
  BULLISH_SEPARATING_LINES(
      "SPL", "Bullish Separating Lines", PatternSentiment.BULLISH_CONTINUATION),
  RISING_THREE_METHODS("RTM", "Rising Three Methods", PatternSentiment.BULLISH_CONTINUATION),
  UPSIDE_TASUKI_GAP("UTG", "Upside Tasuki Gap", PatternSentiment.BULLISH_CONTINUATION),
  BULLISH_SIDE_BY_SIDE_WHITE_LINES(
      "SBW", "Bullish Side-by-Side White Lines", PatternSentiment.BULLISH_CONTINUATION),
  BULLISH_THREE_LINE_STRIKE(
      "TLS", "Bullish Three Line Strike", PatternSentiment.BULLISH_CONTINUATION),
  UPSIDE_GAP_THREE_METHODS(
      "UGM", "Upside Gap Three Methods", PatternSentiment.BULLISH_CONTINUATION),
  BULLISH_ON_NECK_LINE("ONL", "Bullish On Neck Line", PatternSentiment.BULLISH_CONTINUATION),
  BULLISH_IN_NECK_LINE("INL", "Bullish In Neck Line", PatternSentiment.BULLISH_CONTINUATION),

  // D. Bearish Continuation
  BEARISH_SEPARATING_LINES(
      "SPL", "Bearish Separating Lines", PatternSentiment.BEARISH_CONTINUATION),
  FALLING_THREE_METHODS("FTM", "Falling Three Methods", PatternSentiment.BEARISH_CONTINUATION),
  DOWNSIDE_TASUKI_GAP("DTG", "Downside Tasuki Gap", PatternSentiment.BEARISH_CONTINUATION),
  BEARISH_SIDE_BY_SIDE_WHITE_LINES(
      "SBW", "Bearish Side-by-Side White Lines", PatternSentiment.BEARISH_CONTINUATION),
  BEARISH_THREE_LINE_STRIKE(
      "TLS", "Bearish Three Line Strike", PatternSentiment.BEARISH_CONTINUATION),
  DOWNSIDE_GAP_THREE_METHODS(
      "DGM", "Downside Gap Three Methods", PatternSentiment.BEARISH_CONTINUATION),
  BEARISH_ON_NECK_LINE("ONL", "Bearish On Neck Line", PatternSentiment.BEARISH_CONTINUATION),
  BEARISH_IN_NECK_LINE("INL", "Bearish In Neck Line", PatternSentiment.BEARISH_CONTINUATION);

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
