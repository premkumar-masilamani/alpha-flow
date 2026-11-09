package com.alphaflow.persistence.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DataProviderTest {

  @Test
  void testFromValidValue() {
    assertEquals(DataProvider.YAHOO_FINANCE, DataProvider.from("YAHOO_FINANCE"));
    assertEquals(DataProvider.YAHOO_FINANCE, DataProvider.from("yahoo_finance"));
    assertEquals(DataProvider.YAHOO_FINANCE, DataProvider.from("  YAHOO_FINANCE  "));
  }

  @Test
  void testFromNullOrBlank() {
    assertNull(DataProvider.from(null));
    assertNull(DataProvider.from(""));
    assertNull(DataProvider.from("   "));
  }

  @Test
  void testFromUnknownValue() {
    assertThrows(IllegalArgumentException.class, () -> DataProvider.from("ANGEL_ONE"));
    assertThrows(IllegalArgumentException.class, () -> DataProvider.from("UNKNOWN"));
  }
}
