package com.alphaflow.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
  void testConvertFifteenMinuteVariations() {
    assertEquals(Timeframe.FIFTEEN_MINUTE, converter.convert("fifteen_minute"));
    assertEquals(Timeframe.FIFTEEN_MINUTE, converter.convert("FIFTEEN_MINUTE"));
    assertEquals(Timeframe.FIFTEEN_MINUTE, converter.convert("15m"));
    assertEquals(Timeframe.FIFTEEN_MINUTE, converter.convert("15M"));
    assertEquals(Timeframe.FIFTEEN_MINUTE, converter.convert("15min"));
  }

  @Test
  void testConvertNullAndEmpty() {
    assertNull(converter.convert(null));
    assertNull(converter.convert(""));
    assertNull(converter.convert("   "));
  }
}
