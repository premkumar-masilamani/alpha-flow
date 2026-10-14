package com.alphaflow.common.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TimeframeTest {

  @Test
  void testFromValidValues() {
    assertEquals(Timeframe.DAILY, Timeframe.from("daily"));
    assertEquals(Timeframe.DAILY, Timeframe.from("DAILY"));
    assertEquals(Timeframe.WEEKLY, Timeframe.from("weekly"));
    assertEquals(Timeframe.WEEKLY, Timeframe.from("WEEKLY"));
  }

  @Test
  void testFromNullAndEmpty() {
    assertNull(Timeframe.from(null));
    assertNull(Timeframe.from(""));
    assertNull(Timeframe.from("   "));
  }

  @Test
  void testFromInvalidThrows() {
    assertThrows(IllegalArgumentException.class, () -> Timeframe.from("15M"));
    assertThrows(IllegalArgumentException.class, () -> Timeframe.from("MONTHLY"));
  }

  @Test
  void testGetValue() {
    assertEquals("DAILY", Timeframe.DAILY.getValue());
    assertEquals("WEEKLY", Timeframe.WEEKLY.getValue());
  }
}
