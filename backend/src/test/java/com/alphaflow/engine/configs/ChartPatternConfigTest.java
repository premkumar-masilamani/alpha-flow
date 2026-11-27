package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alphaflow.common.enums.Timeframe;
import org.junit.jupiter.api.Test;

class ChartPatternConfigTest {

  @Test
  void testGetRetentionDaysForTimeframe() {
    assertEquals(
        ChartPatternConfig.DAILY_RETENTION_DAYS,
        ChartPatternConfig.getRetentionDaysForTimeframe(Timeframe.DAILY));
    assertEquals(
        ChartPatternConfig.WEEKLY_RETENTION_DAYS,
        ChartPatternConfig.getRetentionDaysForTimeframe(Timeframe.WEEKLY));
  }

  @Test
  void testGetRetentionDaysForUnsupportedTimeframeThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ChartPatternConfig.getRetentionDaysForTimeframe(null));
  }
}
