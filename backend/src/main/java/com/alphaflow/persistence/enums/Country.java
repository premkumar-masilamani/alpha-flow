package com.alphaflow.persistence.enums;

import java.time.ZoneId;

public enum Country {
  US(ZoneId.of("America/New_York")),
  IN(ZoneId.of("Asia/Kolkata"));

  private final ZoneId zoneId;

  Country(ZoneId zoneId) {
    this.zoneId = zoneId;
  }

  public ZoneId getZoneId() {
    return zoneId;
  }

  public static Country from(String text) {
    if (text == null || text.trim().isEmpty()) {
      return null;
    }
    String normalized = text.trim().toUpperCase();
    for (Country country : values()) {
      if (country.name().equals(normalized)) {
        return country;
      }
    }
    throw new IllegalArgumentException("Unknown country: " + text);
  }
}
