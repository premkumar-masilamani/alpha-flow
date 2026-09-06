package com.alphaflow.persistence.enums;

public enum PatternSentiment {
  BULLISH_REVERSAL,
  BEARISH_REVERSAL,
  BULLISH_CONTINUATION,
  BEARISH_CONTINUATION,

  // Legacy sentiments maintained for database backward compatibility
  @Deprecated
  BULL,
  @Deprecated
  BEAR
}
