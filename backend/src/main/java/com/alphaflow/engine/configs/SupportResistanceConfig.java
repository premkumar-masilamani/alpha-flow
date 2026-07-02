package com.alphaflow.engine.configs;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class SupportResistanceConfig {

  @Value("${alphaflow.sr.daily.window:10}")
  private int dailyWindow;

  @Value("${alphaflow.sr.daily.horizontal-min-touches:3}")
  private int dailyHorizontalMinTouches;

  @Value("${alphaflow.sr.daily.max-breaks:2}")
  private int dailyMaxBreaks;

  @Value("${alphaflow.sr.weekly.window:5}")
  private int weeklyWindow;

  @Value("${alphaflow.sr.weekly.horizontal-min-touches:2}")
  private int weeklyHorizontalMinTouches;

  @Value("${alphaflow.sr.weekly.max-breaks:4}")
  private int weeklyMaxBreaks;

  @Value("${alphaflow.sr.daily.cb-horizontal-pct:20}")
  private int dailyCbHorizontalPct;

  @Value("${alphaflow.sr.daily.cb-angular-pct:35}")
  private int dailyCbAngularPct;

  @Value("${alphaflow.sr.weekly.cb-horizontal-pct:50}")
  private int weeklyCbHorizontalPct;

  @Value("${alphaflow.sr.weekly.cb-angular-pct:75}")
  private int weeklyCbAngularPct;

  @Value("${alphaflow.sr.daily.tolerance-pct:1.0}")
  private double dailyTolerancePct;

  @Value("${alphaflow.sr.daily.proximity-pct:1.0}")
  private double dailyProximityPct;

  @Value("${alphaflow.sr.daily.angular-min-touches:4}")
  private int dailyAngularMinTouches;

  @Value("${alphaflow.sr.weekly.tolerance-pct:1.0}")
  private double weeklyTolerancePct;

  @Value("${alphaflow.sr.weekly.proximity-pct:1.0}")
  private double weeklyProximityPct;

  @Value("${alphaflow.sr.weekly.angular-min-touches:4}")
  private int weeklyAngularMinTouches;
}
