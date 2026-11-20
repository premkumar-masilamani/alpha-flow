package com.alphaflow.engine.schedulers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class SchedulerUtilsTest {

  @Test
  void testConstructorIsPrivate() throws Exception {
    Constructor<SchedulerUtils> constructor = SchedulerUtils.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    constructor.setAccessible(true);
    InvocationTargetException exception =
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    assertTrue(exception.getCause() instanceof UnsupportedOperationException);
    assertEquals(
        "This is a utility class and cannot be instantiated",
        exception.getCause().getMessage());
  }

  @Test
  void testFormatDuration() {
    String formatted = SchedulerUtils.formatDuration(65432);
    assertEquals("1 m 5 s (65432 ms)", formatted);

    String zero = SchedulerUtils.formatDuration(0);
    assertEquals("0 m 0 s (0 ms)", zero);
  }
}
