package com.alphaflow.common.utilities;

public final class StringUtil {

  private StringUtil() {}

  public static boolean isNotEmpty(String text) {
    return text != null && !text.isEmpty();
  }

  public static boolean isEmpty(String text) {
    return text == null || text.isEmpty();
  }
}
