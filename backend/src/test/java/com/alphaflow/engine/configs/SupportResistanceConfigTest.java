package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SupportResistanceConfigTest {

  @Test
  void testConfigProperties() {
    SupportResistanceConfig config = new SupportResistanceConfig();
    assertEquals(30, config.getRetentionDays());

    config.setRetentionDays(60);
    assertEquals(60, config.getRetentionDays());

    assertEquals(500, config.getMaxAllowedBuckets());
    config.setMaxAllowedBuckets(250);
    assertEquals(250, config.getMaxAllowedBuckets());
  }
}
