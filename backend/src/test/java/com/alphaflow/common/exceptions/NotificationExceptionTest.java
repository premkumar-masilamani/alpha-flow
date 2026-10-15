package com.alphaflow.common.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class NotificationExceptionTest {

  @Test
  void testConstructorWithMessage() {
    NotificationException exception = new NotificationException("Test error message");
    assertEquals("Test error message", exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void testConstructorWithMessageAndCause() {
    RuntimeException cause = new RuntimeException("Underlying cause");
    NotificationException exception = new NotificationException("Wrapper error message", cause);
    assertEquals("Wrapper error message", exception.getMessage());
    assertSame(cause, exception.getCause());
  }
}
