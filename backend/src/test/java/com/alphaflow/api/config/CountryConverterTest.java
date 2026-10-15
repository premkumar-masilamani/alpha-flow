package com.alphaflow.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alphaflow.persistence.enums.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CountryConverterTest {

  private CountryConverter countryConverter;

  @BeforeEach
  void setUp() {
    countryConverter = new CountryConverter();
  }

  @Test
  void testConvertValidCountry() {
    assertEquals(Country.US, countryConverter.convert("us"));
    assertEquals(Country.IN, countryConverter.convert("in"));
    assertEquals(Country.US, countryConverter.convert("US"));
    assertEquals(Country.IN, countryConverter.convert("  IN  "));
  }

  @Test
  void testConvertNullAndEmpty() {
    assertNull(countryConverter.convert(null));
    assertNull(countryConverter.convert(""));
    assertNull(countryConverter.convert("   "));
  }

  @Test
  void testConvertInvalidCountry() {
    assertThrows(IllegalArgumentException.class, () -> countryConverter.convert("invalid"));
  }
}
