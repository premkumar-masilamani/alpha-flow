package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class YahooFinanceConfigTest {

  @Test
  void testConfigProperties() {
    YahooFinanceConfig config = new YahooFinanceConfig();

    config.setDownloadUrl("https://example.com");
    config.setDelayMilliseconds(1000L);
    config.setMarketTimezone("America/New_York");
    config.setMarketCutoffTime(LocalTime.of(17, 0));
    LocalDate holiday = LocalDate.of(2026, 1, 1);
    config.setHolidays(List.of(holiday));

    assertEquals("https://example.com", config.getDownloadUrl());
    assertEquals(1000L, config.getDelayMilliseconds());
    assertEquals("America/New_York", config.getMarketTimezone());
    assertEquals(LocalTime.of(17, 0), config.getMarketCutoffTime());
    assertEquals(List.of(holiday), config.getHolidays());
    assertEquals(Set.of(holiday), config.getHolidaySet());

    config.setHolidays(null);
    assertTrue(config.getHolidaySet().isEmpty());
  }
}
