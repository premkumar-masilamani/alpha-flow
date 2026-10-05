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
}
