package com.alphaflow.engine.configs;

import com.alphaflow.common.enums.Timeframe;
import lombok.Data;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class SupportResistanceConfig {

  private Double bucketWidthPct = 0.01;
  private int maxAllowedBuckets = 500;
  private int breakoutConfirmationBars = 2;
  private int maxFalseBreakouts = 1;
  private int dailyRetentionDays = 30;
  private int weeklyRetentionDays = 180;
  private int atrPeriod = 14;
  private double atrMultiplier = 0.5;
  private double strideRatio = 0.5;
  private int minTouchCount = 3;
  private double nmsOverlapThreshold = 0.5;

  public int getRetentionDaysForTimeframe(Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyRetentionDays;
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyRetentionDays;
    } else {
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }
}
