package com.alphaflow.api.utils;

import com.alphaflow.common.enums.Timeframe;

public class APIUtil {

  public static Timeframe parseTimeframe(String timeframe) {
    if (timeframe == null || timeframe.trim().isEmpty()) {
      return Timeframe.DAILY; // Default
    }
    String tf = timeframe.trim().toLowerCase();
    return switch (tf) {
      case "d", "daily" -> Timeframe.DAILY;
      case "w", "weekly" -> Timeframe.WEEKLY;
      case "m", "monthly" -> throw new UnsupportedOperationException(
          "Monthly timeframe not yet supported");
      default -> throw new IllegalArgumentException(
          "Invalid timeframe: '" + timeframe + "' (expected d, w, or m)");
    };
  }
}
