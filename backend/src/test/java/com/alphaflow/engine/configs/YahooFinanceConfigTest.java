package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class YahooFinanceConfigTest {

  @Test
  void testConfigProperties() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    config.setDownloadUrl("https://example.com");
    config.setDelayMilliseconds(1000L);

    assertEquals("https://example.com", config.getDownloadUrl());
    assertEquals(1000L, config.getDelayMilliseconds());
  }
}
