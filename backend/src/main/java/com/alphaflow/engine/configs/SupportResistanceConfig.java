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

  private int atrPeriod = 14;
  private BigDecimal swingAtrMultiplier = new BigDecimal("2.0");
  private BigDecimal zoneAtrMultiplier = new BigDecimal("1.0");
  private int minTouchCount = 2;
  private int dailyRetentionDays = 30;
  private int weeklyRetentionDays = 180;

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
