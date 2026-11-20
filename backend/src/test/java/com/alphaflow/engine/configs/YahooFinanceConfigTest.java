package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class YahooFinanceConfigTest {

  @Test
  void testConfigProperties() {
    YahooFinanceConfig config = new YahooFinanceConfig();

    config.setDownloadUrl("https://example.com");
    config.setDelayMilliseconds(1000L);
    config.setBackfillStartDate(LocalDate.of(2000, 1, 1));

    assertEquals("https://example.com", config.getDownloadUrl());
    assertEquals(1000L, config.getDelayMilliseconds());
    assertEquals(LocalDate.of(2000, 1, 1), config.getBackfillStartDate());
  }
}
