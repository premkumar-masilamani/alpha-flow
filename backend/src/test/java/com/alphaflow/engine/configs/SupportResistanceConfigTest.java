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

    assertEquals(0.25, config.getRangePct());
    config.setRangePct(0.30);
    assertEquals(0.30, config.getRangePct());

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
