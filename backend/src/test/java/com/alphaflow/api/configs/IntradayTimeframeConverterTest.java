package com.alphaflow.api.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alphaflow.common.enums.IntradayTimeframe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntradayTimeframeConverterTest {

  private IntradayTimeframeConverter converter;

  @BeforeEach
  void setUp() {
    converter = new IntradayTimeframeConverter();
  }

  @Test
  void testConvertFifteenMinute() {
    assertEquals(IntradayTimeframe._15M, converter.convert("15m"));
    assertEquals(IntradayTimeframe._15M, converter.convert("15M"));
    assertEquals(IntradayTimeframe._15M, converter.convert("_15M"));
  }

  @Test
  void testConvertNullAndEmpty() {
    assertNull(converter.convert(null));
    assertNull(converter.convert(""));
    assertNull(converter.convert("   "));
  }

  @Test
  void testConvertUnknownThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> converter.convert("daily"));
    assertThrows(IllegalArgumentException.class, () -> converter.convert("1h"));
  }
}
