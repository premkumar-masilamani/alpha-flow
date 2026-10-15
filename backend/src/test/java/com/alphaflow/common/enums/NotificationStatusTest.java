package com.alphaflow.common.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NotificationStatusTest {

  @Test
  void testFromValidValues() {
    assertEquals(NotificationStatus.SENT, NotificationStatus.from("SENT"));
    assertEquals(NotificationStatus.SENT, NotificationStatus.from("sent"));
    assertEquals(NotificationStatus.FAILED, NotificationStatus.from("FAILED"));
    assertEquals(NotificationStatus.FAILED, NotificationStatus.from("failed"));
  }

  @Test
  void testFromNullAndEmpty() {
    assertNull(NotificationStatus.from(null));
    assertNull(NotificationStatus.from(""));
    assertNull(NotificationStatus.from("   "));
  }

  @Test
  void testFromInvalidThrows() {
    assertThrows(IllegalArgumentException.class, () -> NotificationStatus.from("PENDING"));
    assertThrows(IllegalArgumentException.class, () -> NotificationStatus.from("INVALID"));
  }

  @Test
  void testGetValue() {
    assertEquals("SENT", NotificationStatus.SENT.getValue());
    assertEquals("FAILED", NotificationStatus.FAILED.getValue());
  }
}
