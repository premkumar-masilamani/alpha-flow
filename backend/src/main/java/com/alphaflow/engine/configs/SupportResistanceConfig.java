package com.alphaflow.engine.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "alphaflow.support-resistance")
@Data
public class SupportResistanceConfig {

  private int dailyRetentionDays;
  private int weeklyRetentionDays;
  private int maxAllowedBuckets;
  private Double rangePct;
}
