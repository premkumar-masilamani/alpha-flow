package com.alphaflow.api.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alphaflow.common.enums.Timeframe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimeframeConverterTest {

  private TimeframeConverter converter;

  @BeforeEach
  void setUp() {
    converter = new TimeframeConverter();
  }

  @Test
  void testConvertStandardTimeframes() {
    assertEquals(Timeframe.DAILY, converter.convert("daily"));
    assertEquals(Timeframe.DAILY, converter.convert("DAILY"));
    assertEquals(Timeframe.WEEKLY, converter.convert("weekly"));
    assertEquals(Timeframe.WEEKLY, converter.convert("WEEKLY"));
  }

  @Test
  void testConvertInvalidThrows() {
    assertThrows(IllegalArgumentException.class, () -> converter.convert("15m"));
    assertThrows(IllegalArgumentException.class, () -> converter.convert("monthly"));
  }

  @Test
  void testConvertNullAndEmpty() {
    assertNull(converter.convert(null));
    assertNull(converter.convert(""));
    assertNull(converter.convert("   "));
  }
}
