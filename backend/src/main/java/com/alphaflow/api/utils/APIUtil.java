package com.alphaflow.api.utils;

import com.alphaflow.persistence.enums.Timeframe;

public class APIUtil {

  public static Timeframe parseTimeframe(String timeframe) {
    try {
      return Timeframe.valueOf(timeframe.trim().toUpperCase());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new IllegalArgumentException(
          "Invalid timeframe: '" + timeframe + "' (expected DAILY or WEEKLY)");
    }
  }
}
