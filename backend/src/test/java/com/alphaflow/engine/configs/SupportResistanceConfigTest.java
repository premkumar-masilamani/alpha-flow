package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SupportResistanceConfigTest {

  @Test
  void testConfigConstants() {
    assertEquals(14, SupportResistanceConfig.ATR_PERIOD);
    assertEquals(0, new BigDecimal("2.0").compareTo(SupportResistanceConfig.SWING_ATR_MULTIPLIER));
    assertEquals(0, new BigDecimal("1.0").compareTo(SupportResistanceConfig.ZONE_ATR_MULTIPLIER));
    assertEquals(0, new BigDecimal("1.0").compareTo(SupportResistanceConfig.BREACH_ATR_MULTIPLIER));
    assertEquals(1, SupportResistanceConfig.MAX_ALLOWED_FLIPS);
    assertEquals(1, SupportResistanceConfig.MAX_FALSE_BREAKOUTS);
    assertEquals(2, SupportResistanceConfig.MIN_TOUCH_COUNT);
  }
}
