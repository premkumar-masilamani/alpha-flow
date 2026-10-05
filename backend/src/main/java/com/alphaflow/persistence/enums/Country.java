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
}
