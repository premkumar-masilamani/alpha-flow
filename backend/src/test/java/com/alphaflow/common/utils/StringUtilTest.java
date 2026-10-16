package com.alphaflow.common.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

class StringUtilTest {

  @Test
  void testPrivateConstructor() throws Exception {
    Constructor<StringUtil> constructor = StringUtil.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    StringUtil instance = constructor.newInstance();
    assertNotNull(instance);
  }

  @Test
  void testIsNotEmpty() {
    assertFalse(StringUtil.isNotEmpty(null));
    assertFalse(StringUtil.isNotEmpty(""));
    assertTrue(StringUtil.isNotEmpty("   "));
    assertTrue(StringUtil.isNotEmpty("hello"));
  }

  @Test
  void testIsEmpty() {
    assertTrue(StringUtil.isEmpty(null));
    assertTrue(StringUtil.isEmpty(""));
    assertFalse(StringUtil.isEmpty("   "));
    assertFalse(StringUtil.isEmpty("hello"));
  }
}
