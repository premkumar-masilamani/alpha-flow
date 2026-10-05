package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.time.MonthDay;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class YahooFinanceConfigTest {

  @Test
  void testConfigProperties() {
    YahooFinanceConfig config = new YahooFinanceConfig();

    config.setDownloadUrl("https://example.com");
    config.setDelayMilliseconds(1000L);
    config.setMarketCutoffTime(LocalTime.of(17, 0));
    config.setHolidays(List.of("01-01"));

    assertEquals("https://example.com", config.getDownloadUrl());
    assertEquals(1000L, config.getDelayMilliseconds());
    assertEquals(LocalTime.of(17, 0), config.getMarketCutoffTime());
    assertEquals(List.of("01-01"), config.getHolidays());
    assertEquals(Set.of(MonthDay.of(1, 1)), config.getHolidaySet());

    config.setHolidays(null);
    assertTrue(config.getHolidaySet().isEmpty());
  }
}
