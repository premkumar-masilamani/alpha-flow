package com.alphaflow.api.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alphaflow.persistence.enums.Timeframe;
import org.junit.jupiter.api.Test;

class ApiPropertiesTest {

  @Test
  void testDefaultWindow() {
    ApiProperties props = new ApiProperties();
    assertEquals(180, props.windowFor(Timeframe.DAILY));
    assertEquals(180, props.windowFor(Timeframe.WEEKLY));
  }

  @Test
  void testConfiguredWindow() {
    ApiProperties props = new ApiProperties();
    props.setWindow(100);

    assertEquals(100, props.windowFor(Timeframe.DAILY));
    assertEquals(100, props.windowFor(Timeframe.WEEKLY));
    assertEquals(100, props.getWindow());
  }
}
