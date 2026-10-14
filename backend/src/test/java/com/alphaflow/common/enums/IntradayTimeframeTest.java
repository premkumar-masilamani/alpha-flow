package com.alphaflow.common.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class IntradayTimeframeTest {

  @Test
  void testFromValidValues() {
    assertEquals(IntradayTimeframe._15M, IntradayTimeframe.from("15m"));
    assertEquals(IntradayTimeframe._15M, IntradayTimeframe.from("15M"));
    assertEquals(IntradayTimeframe._15M, IntradayTimeframe.from("_15M"));
  }

  @Test
  void testFromNullAndEmpty() {
    assertNull(IntradayTimeframe.from(null));
    assertNull(IntradayTimeframe.from(""));
    assertNull(IntradayTimeframe.from("   "));
  }

  @Test
  void testFromInvalidThrows() {
    assertThrows(IllegalArgumentException.class, () -> IntradayTimeframe.from("DAILY"));
    assertThrows(IllegalArgumentException.class, () -> IntradayTimeframe.from("5M"));
  }

  @Test
  void testGetValue() {
    assertEquals("15M", IntradayTimeframe._15M.getValue());
  }
}
