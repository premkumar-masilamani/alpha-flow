package com.alphaflow.common.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alphaflow.engine.strategies.options.enums.OptionType;
import org.junit.jupiter.api.Test;

class OptionTypeTest {

  @Test
  void testFromValidValues() {
    assertEquals(OptionType.CALL, OptionType.from("CALL"));
    assertEquals(OptionType.CALL, OptionType.from("call"));
    assertEquals(OptionType.CALL, OptionType.from(" CE "));
    assertEquals(OptionType.CALL, OptionType.from("ce"));
    assertEquals(OptionType.PUT, OptionType.from("PUT"));
    assertEquals(OptionType.PUT, OptionType.from("put"));
    assertEquals(OptionType.PUT, OptionType.from(" PE "));
    assertEquals(OptionType.PUT, OptionType.from("pe"));
  }

  @Test
  void testGetCode() {
    assertEquals("CE", OptionType.CALL.getCode());
    assertEquals("PE", OptionType.PUT.getCode());
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
