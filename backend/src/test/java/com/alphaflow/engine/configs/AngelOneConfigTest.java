package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
  void testValidateSuccess() {
    AngelOneConfig config = new AngelOneConfig();
    config.setApiKey("test-api-key");
    config.setClientCode("test-client-code");
    config.setPassword("test-password");
    config.setTotpKey("test-totp-key");

    assertDoesNotThrow(config::validate);
  }

  @Test
  void testValidateApiKeyFailures() {
    AngelOneConfig config = new AngelOneConfig();
    config.setClientCode("test-client-code");
    config.setPassword("test-password");
    config.setTotpKey("test-totp-key");

    config.setApiKey(null);
    IllegalStateException ex1 = assertThrows(IllegalStateException.class, config::validate);
    assertTrue(ex1.getMessage().contains("alphaflow.angelone.apiKey"));

    config.setApiKey("   ");
    IllegalStateException ex2 = assertThrows(IllegalStateException.class, config::validate);
    assertTrue(ex2.getMessage().contains("alphaflow.angelone.apiKey"));

    config.setApiKey("${ANGEL_ONE_API_KEY}");
    IllegalStateException ex3 = assertThrows(IllegalStateException.class, config::validate);
    assertTrue(ex3.getMessage().contains("alphaflow.angelone.apiKey"));
  }

  @Test
  void testValidateClientCodeFailures() {
    AngelOneConfig config = new AngelOneConfig();
    config.setApiKey("test-api-key");
    config.setPassword("test-password");
    config.setTotpKey("test-totp-key");

    config.setClientCode(null);
    IllegalStateException ex1 = assertThrows(IllegalStateException.class, config::validate);
    assertTrue(ex1.getMessage().contains("alphaflow.angelone.clientCode"));

    config.setClientCode("   ");
    IllegalStateException ex2 = assertThrows(IllegalStateException.class, config::validate);
    assertTrue(ex2.getMessage().contains("alphaflow.angelone.clientCode"));

    config.setClientCode("${ANGEL_ONE_CLIENT_CODE}");
    IllegalStateException ex3 = assertThrows(IllegalStateException.class, config::validate);
    assertTrue(ex3.getMessage().contains("alphaflow.angelone.clientCode"));
  }

  @Test
  void testValidatePasswordFailures() {
    AngelOneConfig config = new AngelOneConfig();
    config.setApiKey("test-api-key");
    config.setClientCode("test-client-code");
    config.setTotpKey("test-totp-key");

    config.setPassword(null);
    IllegalStateException ex1 = assertThrows(IllegalStateException.class, config::validate);
    assertTrue(ex1.getMessage().contains("alphaflow.angelone.password"));

    config.setPassword("   ");
    IllegalStateException ex2 = assertThrows(IllegalStateException.class, config::validate);
    assertTrue(ex2.getMessage().contains("alphaflow.angelone.password"));

    config.setPassword("${ANGEL_ONE_PASSWORD}");
    IllegalStateException ex3 = assertThrows(IllegalStateException.class, config::validate);
    assertTrue(ex3.getMessage().contains("alphaflow.angelone.password"));
  }

  @Test
  void testValidateTotpKeyFailures() {
    AngelOneConfig config = new AngelOneConfig();
    config.setApiKey("test-api-key");
    config.setClientCode("test-client-code");
    config.setPassword("test-password");

    config.setTotpKey(null);
    IllegalStateException ex1 = assertThrows(IllegalStateException.class, config::validate);
    assertTrue(ex1.getMessage().contains("alphaflow.angelone.totpKey"));

    config.setTotpKey("   ");
    IllegalStateException ex2 = assertThrows(IllegalStateException.class, config::validate);
    assertTrue(ex2.getMessage().contains("alphaflow.angelone.totpKey"));

    config.setTotpKey("${ANGEL_ONE_TOTP_KEY}");
    IllegalStateException ex3 = assertThrows(IllegalStateException.class, config::validate);
    assertTrue(ex3.getMessage().contains("alphaflow.angelone.totpKey"));
  }
}
