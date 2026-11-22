package com.alphaflow.engine.configs;

import com.alphaflow.common.enums.Timeframe;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SupportResistanceConfig {

  public static final int ATR_PERIOD = 14;
  public static final BigDecimal SWING_ATR_MULTIPLIER = new BigDecimal("2.0");
  public static final BigDecimal ZONE_ATR_MULTIPLIER = new BigDecimal("1.0");
  public static final BigDecimal BREACH_ATR_MULTIPLIER = new BigDecimal("1.0");
  public static final int MAX_ALLOWED_FLIPS = 1;
  public static final int MAX_FALSE_BREAKOUTS = 1;
  public static final int MIN_TOUCH_COUNT = 2;
  public static final int DAILY_RETENTION_DAYS = 30;
  public static final int WEEKLY_RETENTION_DAYS = 180;

  public static int getRetentionDaysForTimeframe(Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return DAILY_RETENTION_DAYS;
    } else if (timeframe == Timeframe.WEEKLY) {
      return WEEKLY_RETENTION_DAYS;
    } else {
      log.error("Unsupported timeframe for retention lookup: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }
}
