package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TelegramConfigTest {

  @Test
  void testConfigPropertiesAndDefaults() {
    TelegramConfig config = new TelegramConfig();

    assertNull(config.getApiToken());
    assertNull(config.getChatId());
    assertEquals(
        "https://api.telegram.org/bot{TELEGRAM_API_TOKEN}/sendMessage?chat_id={TELEGRAM_CHAT_ID}&text={MESSAGE}&parse_mode=MarkdownV2",
        config.getMessageUrl());
    assertEquals(4096, config.getMaxMessageLength());

    config.setApiToken("test-bot-token");
    config.setChatId("123456789");
    config.setMessageUrl("https://example.com/bot{TELEGRAM_API_TOKEN}/sendMessage");
    config.setMaxMessageLength(2000);

    assertEquals("test-bot-token", config.getApiToken());
    assertEquals("123456789", config.getChatId());
    assertEquals("https://example.com/bot{TELEGRAM_API_TOKEN}/sendMessage", config.getMessageUrl());
    assertEquals(2000, config.getMaxMessageLength());
    config.validate();
  }

  @Test
  void testValidateThrowsWhenApiTokenMissingOrUnexpanded() {
    TelegramConfig config = new TelegramConfig();
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class, config::validate);

    config.setApiToken("  ");
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class, config::validate);

    config.setApiToken("${TELEGRAM_API_TOKEN}");
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class, config::validate);
  }

  @Test
  void testValidateThrowsWhenChatIdMissingOrUnexpanded() {
    TelegramConfig config = new TelegramConfig();
    config.setApiToken("valid-token");
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class, config::validate);

    config.setChatId("");
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class, config::validate);

    config.setChatId("${TELEGRAM_CHAT_ID}");
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class, config::validate);
  }
}
