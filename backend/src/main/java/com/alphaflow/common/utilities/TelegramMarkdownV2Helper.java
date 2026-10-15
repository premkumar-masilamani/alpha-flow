package com.alphaflow.common.utilities;

import java.util.Set;

public final class TelegramMarkdownV2Helper {

  private static final Set<Character> RESERVED_CHARACTERS =
      Set.of(
          '_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!');

  private TelegramMarkdownV2Helper() {}

  public static String escape(String text) {
    if (text != null && !text.isEmpty()) {
      StringBuilder builder = new StringBuilder(text.length() * 2);
      for (int index = 0; index < text.length(); index++) {
        char character = text.charAt(index);
        if (RESERVED_CHARACTERS.contains(character)) {
          builder.append('\\');
        }
        builder.append(character);
      }
      return builder.toString();
    } else {
      return text;
    }
  }
}
