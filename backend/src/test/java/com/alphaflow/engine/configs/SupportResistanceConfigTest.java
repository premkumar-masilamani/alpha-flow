package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alphaflow.common.enums.Timeframe;
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
    assertEquals(30, SupportResistanceConfig.DAILY_RETENTION_DAYS);
    assertEquals(180, SupportResistanceConfig.WEEKLY_RETENTION_DAYS);
  }

  @Test
  void testGetRetentionDaysForTimeframe() {
    assertEquals(30, SupportResistanceConfig.getRetentionDaysForTimeframe(Timeframe.DAILY));
    assertEquals(180, SupportResistanceConfig.getRetentionDaysForTimeframe(Timeframe.WEEKLY));

    assertThrows(
        IllegalArgumentException.class,
        () -> SupportResistanceConfig.getRetentionDaysForTimeframe(null));
  }
}
