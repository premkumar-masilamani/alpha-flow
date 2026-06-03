package com.alphaflow.api.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChartConfigTest {

  @Test
  void testDefaultWindow() {
    ChartConfig props = new ChartConfig();

    assertEquals(180, props.getWindow());

    assertEquals(180, props.getWindow());
  }

  @Test
  void testConfiguredWindow() {
    ChartConfig props = new ChartConfig();

    props.setWindow(100);

    assertEquals(100, props.getWindow());

    assertEquals(100, props.getWindow());

    assertEquals(100, props.getWindow());
  }
}
