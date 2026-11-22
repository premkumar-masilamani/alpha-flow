package com.alphaflow.engine.configs;

import com.alphaflow.common.enums.Timeframe;
import java.math.BigDecimal;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@Slf4j
public class SupportResistanceConfig {

  public static final int ATR_PERIOD = 14;
  public static final BigDecimal SWING_ATR_MULTIPLIER = new BigDecimal("2.0");
  public static final BigDecimal ZONE_ATR_MULTIPLIER = new BigDecimal("1.0");
  public static final BigDecimal BREACH_ATR_MULTIPLIER = new BigDecimal("1.0");
  public static final int MAX_ALLOWED_FLIPS = 1;
  public static final int MAX_FALSE_BREAKOUTS = 1;
  public static final int MIN_TOUCH_COUNT = 2;
  public static final int DAILY_RETENTION_DAYS = 30;
  public static final int WEEKLY_RETENTION_DAYS = 180;

  private int atrPeriod = ATR_PERIOD;
  private BigDecimal swingAtrMultiplier = SWING_ATR_MULTIPLIER;
  private BigDecimal zoneAtrMultiplier = ZONE_ATR_MULTIPLIER;
  private BigDecimal breachAtrMultiplier = BREACH_ATR_MULTIPLIER;
  private int maxAllowedFlips = MAX_ALLOWED_FLIPS;
  private int maxFalseBreakouts = MAX_FALSE_BREAKOUTS;
  private int minTouchCount = MIN_TOUCH_COUNT;
  private int dailyRetentionDays = DAILY_RETENTION_DAYS;
  private int weeklyRetentionDays = WEEKLY_RETENTION_DAYS;

  public int getRetentionDaysForTimeframe(Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyRetentionDays;
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyRetentionDays;
    } else {
      log.error("Unsupported timeframe for retention lookup: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }
}
