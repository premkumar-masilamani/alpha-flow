package com.alphaflow.api.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alphaflow.common.enums.IntradayTimeframe;
import com.alphaflow.common.enums.Timeframe;
import java.time.DayOfWeek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;

class StringToEnumConverterFactoryTest {

  private StringToEnumConverterFactory factory;

  @BeforeEach
  void setUp() {
    factory = new StringToEnumConverterFactory();
  }

  @Test
  void testConvertTimeframeWithFromMethod() {
    Converter<String, Timeframe> converter = factory.getConverter(Timeframe.class);
    assertNotNull(converter);

    assertEquals(Timeframe.DAILY, converter.convert("daily"));
    assertEquals(Timeframe.DAILY, converter.convert("DAILY"));
    assertEquals(Timeframe.WEEKLY, converter.convert("weekly"));
    assertEquals(Timeframe.WEEKLY, converter.convert("WEEKLY"));
  }

  @Test
  void testConvertIntradayTimeframeWithFromMethod() {
    Converter<String, IntradayTimeframe> converter = factory.getConverter(IntradayTimeframe.class);
    assertNotNull(converter);

    assertEquals(IntradayTimeframe._15M, converter.convert("15m"));
    assertEquals(IntradayTimeframe._15M, converter.convert("15M"));
    assertEquals(IntradayTimeframe._15M, converter.convert("_15M"));
  }

  @Test
  void testConvertEnumWithoutFromMethod() {
    Converter<String, DayOfWeek> converter = factory.getConverter(DayOfWeek.class);
    assertNotNull(converter);

    assertEquals(DayOfWeek.MONDAY, converter.convert("monday"));
    assertEquals(DayOfWeek.FRIDAY, converter.convert("FRIDAY"));
  }

  @Test
  void testConvertNullAndEmptyReturnsNull() {
    Converter<String, Timeframe> converter = factory.getConverter(Timeframe.class);
    assertNotNull(converter);

    assertNull(converter.convert(null));
    assertNull(converter.convert(""));
    assertNull(converter.convert("   "));
  }

  @Test
  void testConvertInvalidValueThrowsIllegalArgumentException() {
    Converter<String, Timeframe> converter = factory.getConverter(Timeframe.class);
    assertNotNull(converter);

    assertThrows(IllegalArgumentException.class, () -> converter.convert("monthly"));
  }

  @Test
  void testGetConverterForNonEnumThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> factory.getConverter((Class) String.class));
  }
}
