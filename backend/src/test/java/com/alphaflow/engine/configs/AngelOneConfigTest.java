package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class AngelOneConfigTest {

  @Test
  void testGettersAndSetters() {
    AngelOneConfig config = new AngelOneConfig();

    config.setApiKey("test-api-key");
    config.setClientCode("test-client-code");
    config.setPassword("test-password");
    config.setTotpKey("test-totp-key");
    config.setLoginUrl("https://login.example.com");
    config.setCandleUrl("https://candle.example.com");
    config.setQuoteUrl("https://quote.example.com");
    config.setDelayMilliseconds(1500L);
    config.setInitialLookbackDays(10);
    config.setTokenTtlSeconds(3600L);
    config.setSessionStartTime(LocalTime.of(9, 15));
    config.setSessionEndTime(LocalTime.of(15, 30));
    config.setBackfillStartDate(LocalDate.of(2015, 1, 1));
    config.setMaxChunkDays(200);

    assertEquals("test-api-key", config.getApiKey());
    assertEquals("test-client-code", config.getClientCode());
    assertEquals("test-password", config.getPassword());
    assertEquals("test-totp-key", config.getTotpKey());
    assertEquals("https://login.example.com", config.getLoginUrl());
    assertEquals("https://candle.example.com", config.getCandleUrl());
    assertEquals("https://quote.example.com", config.getQuoteUrl());
    assertEquals(1500L, config.getDelayMilliseconds());
    assertEquals(10, config.getInitialLookbackDays());
    assertEquals(3600L, config.getTokenTtlSeconds());
    assertEquals(Duration.ofSeconds(3600L), config.getTokenTtl());
    assertEquals(LocalTime.of(9, 15), config.getSessionStartTime());
    assertEquals(LocalTime.of(15, 30), config.getSessionEndTime());
    assertEquals(LocalDate.of(2015, 1, 1), config.getBackfillStartDate());
    assertEquals(200, config.getMaxChunkDays());
  }
}
