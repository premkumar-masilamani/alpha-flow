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
        "https://api.telegram.org/bot{ApiToken}/sendMessage?chat_id={chatId}&text={message}&parse_mode=MarkdownV2",
        config.getMessageUrl());
    assertEquals(4096, config.getMaxMessageLength());

    config.setApiToken("test-bot-token");
    config.setChatId("123456789");
    config.setMessageUrl("https://example.com/bot{ApiToken}/sendMessage");
    config.setMaxMessageLength(2000);

    assertEquals("test-bot-token", config.getApiToken());
    assertEquals("123456789", config.getChatId());
    assertEquals("https://example.com/bot{ApiToken}/sendMessage", config.getMessageUrl());
    assertEquals(2000, config.getMaxMessageLength());
  }
}
