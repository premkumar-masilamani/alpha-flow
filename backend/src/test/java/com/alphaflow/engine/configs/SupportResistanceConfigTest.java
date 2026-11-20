package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SupportResistanceConfigTest {

  @Test
  void testConfigProperties() {
    SupportResistanceConfig config = new SupportResistanceConfig();

    assertEquals(0.01, config.getBucketWidthPct());
    config.setBucketWidthPct(0.02);
    assertEquals(0.02, config.getBucketWidthPct());

    assertEquals(500, config.getMaxAllowedBuckets());
    config.setMaxAllowedBuckets(250);
    assertEquals(250, config.getMaxAllowedBuckets());

    assertEquals(2, config.getBreakoutConfirmationBars());
    config.setBreakoutConfirmationBars(3);
    assertEquals(3, config.getBreakoutConfirmationBars());

    assertEquals(1, config.getMaxFalseBreakouts());
    config.setMaxFalseBreakouts(2);
    assertEquals(2, config.getMaxFalseBreakouts());

    assertEquals(30, config.getDailyRetentionDays());
    config.setDailyRetentionDays(45);
    assertEquals(45, config.getDailyRetentionDays());

    assertEquals(180, config.getWeeklyRetentionDays());
    config.setWeeklyRetentionDays(90);
    assertEquals(90, config.getWeeklyRetentionDays());

    assertEquals(14, config.getAtrPeriod());
    config.setAtrPeriod(20);
    assertEquals(20, config.getAtrPeriod());

    assertEquals(0.5, config.getAtrMultiplier());
    config.setAtrMultiplier(0.75);
    assertEquals(0.75, config.getAtrMultiplier());

    assertEquals(0.5, config.getStrideRatio());
    config.setStrideRatio(0.25);
    assertEquals(0.25, config.getStrideRatio());

    assertEquals(3, config.getMinTouchCount());
    config.setMinTouchCount(4);
    assertEquals(4, config.getMinTouchCount());

    assertEquals(0.5, config.getNmsOverlapThreshold());
    config.setNmsOverlapThreshold(0.6);
    assertEquals(0.6, config.getNmsOverlapThreshold());
  }

  @Test
  void testGetRetentionDaysForTimeframe() {
    SupportResistanceConfig config = new SupportResistanceConfig();
    config.setDailyRetentionDays(30);
    config.setWeeklyRetentionDays(180);

    assertEquals(
        30, config.getRetentionDaysForTimeframe(com.alphaflow.common.enums.Timeframe.DAILY));
    assertEquals(
        180, config.getRetentionDaysForTimeframe(com.alphaflow.common.enums.Timeframe.WEEKLY));

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class, () -> config.getRetentionDaysForTimeframe(null));
  }
}
