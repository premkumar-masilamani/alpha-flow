package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  @Test
  void testHasValidCredentials() {
    AngelOneConfig config = new AngelOneConfig();
    config.setApiKey("api");
    config.setClientCode("client");
    config.setPassword("pass");
    config.setTotpKey("totp");
    assertTrue(config.hasValidCredentials());

    // apiKey null or blank
    config.setApiKey(null);
    assertFalse(config.hasValidCredentials());
    config.setApiKey("   ");
    assertFalse(config.hasValidCredentials());
    config.setApiKey("api");

    // clientCode null or blank
    config.setClientCode(null);
    assertFalse(config.hasValidCredentials());
    config.setClientCode("   ");
    assertFalse(config.hasValidCredentials());
    config.setClientCode("client");

    // password null or blank
    config.setPassword(null);
    assertFalse(config.hasValidCredentials());
    config.setPassword("   ");
    assertFalse(config.hasValidCredentials());
    config.setPassword("pass");

    // totpKey null or blank
    config.setTotpKey(null);
    assertFalse(config.hasValidCredentials());
    config.setTotpKey("   ");
    assertFalse(config.hasValidCredentials());
    config.setTotpKey("totp");
  }
}
