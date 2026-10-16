package com.alphaflow.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class TelegramMarkdownV2HelperTest {

  @Test
  void testEscapeNullAndEmpty() {
    assertNull(TelegramMarkdownV2Helper.escape(null));
    assertEquals("", TelegramMarkdownV2Helper.escape(""));
  }

  @Test
  void testEscapeWithoutSpecialCharacters() {
    assertEquals("Hello World 123", TelegramMarkdownV2Helper.escape("Hello World 123"));
  }

  @Test
  void testEscapeAllReservedCharacters() {
    String reservedCharacters = "_*[]()~`>#+-=|{}.!";
    String expected = "\\_\\*\\[\\]\\(\\)\\~\\`\\>\\#\\+\\-\\=\\|\\{\\}\\.\\!";
    assertEquals(expected, TelegramMarkdownV2Helper.escape(reservedCharacters));
  }

  @Test
  void testEscapeMixedText() {
    String rawText = "Breakout on AAPL: Target [150.50] (Score: +5.2)!";
    String expected = "Breakout on AAPL: Target \\[150\\.50\\] \\(Score: \\+5\\.2\\)\\!";
    assertEquals(expected, TelegramMarkdownV2Helper.escape(rawText));
  }

  @Test
  void testPrivateConstructor() throws Exception {
    Constructor<TelegramMarkdownV2Helper> constructor =
        TelegramMarkdownV2Helper.class.getDeclaredConstructor();
    assertEquals(Modifier.PRIVATE, constructor.getModifiers() & Modifier.PRIVATE);
    constructor.setAccessible(true);
    TelegramMarkdownV2Helper instance = constructor.newInstance();
    assertNotNull(instance);
  }
}
