package com.alphaflow.engine.configs;

import lombok.Data;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class SupportResistanceConfig {

  private Double bucketWidthPct = 0.01;
  private Double rangePct = 0.25;
  private int maxAllowedBuckets = 500;
  private int breakoutConfirmationBars = 2;
  private int maxFalseBreakouts = 1;
  private int dailyRetentionDays = 30;
  private int weeklyRetentionDays = 180;
}
