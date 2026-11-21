package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alphaflow.common.enums.Timeframe;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SupportResistanceConfigTest {

  @Test
  void testConfigProperties() {
    SupportResistanceConfig config = new SupportResistanceConfig();

    assertEquals(14, config.getAtrPeriod());
    config.setAtrPeriod(20);
    assertEquals(20, config.getAtrPeriod());

    assertEquals(0, new BigDecimal("2.0").compareTo(config.getSwingAtrMultiplier()));
    config.setSwingAtrMultiplier(new BigDecimal("2.5"));
    assertEquals(0, new BigDecimal("2.5").compareTo(config.getSwingAtrMultiplier()));

    assertEquals(0, new BigDecimal("1.0").compareTo(config.getZoneAtrMultiplier()));
    config.setZoneAtrMultiplier(new BigDecimal("1.5"));
    assertEquals(0, new BigDecimal("1.5").compareTo(config.getZoneAtrMultiplier()));

    assertEquals(0, new BigDecimal("1.0").compareTo(config.getBreachAtrMultiplier()));
    config.setBreachAtrMultiplier(new BigDecimal("1.25"));
    assertEquals(0, new BigDecimal("1.25").compareTo(config.getBreachAtrMultiplier()));

    assertEquals(1, config.getMaxAllowedFlips());
    config.setMaxAllowedFlips(0);
    assertEquals(0, config.getMaxAllowedFlips());

    assertEquals(1, config.getMaxFalseBreakouts());
    config.setMaxFalseBreakouts(2);
    assertEquals(2, config.getMaxFalseBreakouts());

    assertEquals(2, config.getMinTouchCount());
    config.setMinTouchCount(3);
    assertEquals(3, config.getMinTouchCount());

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

    assertEquals(30, config.getRetentionDaysForTimeframe(Timeframe.DAILY));
    assertEquals(180, config.getRetentionDaysForTimeframe(Timeframe.WEEKLY));

    assertThrows(IllegalArgumentException.class, () -> config.getRetentionDaysForTimeframe(null));
  }
}
