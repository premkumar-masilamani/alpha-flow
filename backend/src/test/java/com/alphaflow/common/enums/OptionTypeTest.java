package com.alphaflow.common.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OptionTypeTest {

  @Test
  void testFromValidValues() {
    assertEquals(OptionType.CE, OptionType.from("CE"));
    assertEquals(OptionType.CE, OptionType.from("ce"));
    assertEquals(OptionType.CE, OptionType.from(" CE "));
    assertEquals(OptionType.PE, OptionType.from("PE"));
    assertEquals(OptionType.PE, OptionType.from("pe"));
    assertEquals(OptionType.PE, OptionType.from(" PE "));
  }

  @Test
  void testFromUnknownValue() {
    assertThrows(IllegalArgumentException.class, () -> OptionType.from("UNKNOWN"));
  }

  @Test
  void testFromNullOrBlank() {
    assertThrows(IllegalArgumentException.class, () -> OptionType.from(null));
    assertThrows(IllegalArgumentException.class, () -> OptionType.from(""));
    assertThrows(IllegalArgumentException.class, () -> OptionType.from("   "));
  }
}
