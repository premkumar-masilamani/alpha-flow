package com.alphaflow.engine.calculators.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LevelTypeTest {

  @Test
  void testFromValidValues() {
    assertEquals(LevelType.SUPPORT, LevelType.from("SUPPORT"));
    assertEquals(LevelType.SUPPORT, LevelType.from("support"));
    assertEquals(LevelType.RESISTANCE, LevelType.from("RESISTANCE"));
    assertEquals(LevelType.RESISTANCE, LevelType.from("resistance"));
  }

  @Test
  void testFromNullAndEmpty() {
    assertNull(LevelType.from(null));
    assertNull(LevelType.from(""));
    assertNull(LevelType.from("   "));
  }

  @Test
  void testFromInvalidThrows() {
    assertThrows(IllegalArgumentException.class, () -> LevelType.from("INVALID"));
  }
}
