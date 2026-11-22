package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class YahooFinanceConfigTest {

  @Test
  void testConfigConstants() {
    assertEquals(
        "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?period1={start}&period2={end}&interval=1d",
        YahooFinanceConfig.DOWNLOAD_URL);
    assertEquals(1000L, YahooFinanceConfig.DELAY_MILLISECONDS);
    assertEquals(LocalDate.of(1900, 1, 1), YahooFinanceConfig.BACKFILL_START_DATE);
  }
}
