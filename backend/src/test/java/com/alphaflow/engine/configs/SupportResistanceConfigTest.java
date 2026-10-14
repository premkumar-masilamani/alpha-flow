package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SupportResistanceConfigTest {

  @Test
  void testConfigProperties() {
    SupportResistanceConfig config = new SupportResistanceConfig();
    assertEquals(0, config.getDailyRetentionDays());
    config.setDailyRetentionDays(30);
    assertEquals(30, config.getDailyRetentionDays());

    assertEquals(0, config.getWeeklyRetentionDays());
    config.setWeeklyRetentionDays(180);
    assertEquals(180, config.getWeeklyRetentionDays());

    assertEquals(0, config.getMaxAllowedBuckets());
    config.setMaxAllowedBuckets(500);
    assertEquals(500, config.getMaxAllowedBuckets());

    org.junit.jupiter.api.Assertions.assertNull(config.getRangePct());
    config.setRangePct(0.25);
    assertEquals(0.25, config.getRangePct());
  }
}
