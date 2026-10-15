package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TelegramConfigTest {

  @Test
  void testConfigProperties() {
    TelegramConfig config = new TelegramConfig();

    assertNull(config.getApiToken());
    assertNull(config.getChatId());
    assertNull(config.getMessageUrl());

    config.setApiToken("test-bot-token");
    config.setChatId("123456789");
    config.setMessageUrl(
        "https://api.telegram.org/bot{apiToken}/sendMessage?chat_id={chatId}&text={message}&parse_mode=MarkdownV2");

    assertEquals("test-bot-token", config.getApiToken());
    assertEquals("123456789", config.getChatId());
    assertEquals(
        "https://api.telegram.org/bot{apiToken}/sendMessage?chat_id={chatId}&text={message}&parse_mode=MarkdownV2",
        config.getMessageUrl());
  }
}
