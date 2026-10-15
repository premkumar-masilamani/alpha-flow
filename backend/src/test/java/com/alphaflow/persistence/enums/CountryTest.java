package com.alphaflow.persistence.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class CountryTest {

  @Test
  void testCountryZoneIdMapping() {
    assertEquals(ZoneId.of("America/New_York"), Country.US.getZoneId());
    assertEquals(ZoneId.of("Asia/Kolkata"), Country.IN.getZoneId());
  }

  @Test
  void testFromValidValues() {
    assertEquals(Country.US, Country.from("US"));
    assertEquals(Country.US, Country.from("us"));
    assertEquals(Country.IN, Country.from("IN"));
    assertEquals(Country.IN, Country.from("in"));
  }

  @Test
  void testFromNullAndEmpty() {
    org.junit.jupiter.api.Assertions.assertNull(Country.from(null));
    org.junit.jupiter.api.Assertions.assertNull(Country.from(""));
    org.junit.jupiter.api.Assertions.assertNull(Country.from("   "));
  }

  @Test
  void testFromInvalidThrows() {
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class, () -> Country.from("XYZ"));
  }
}
