package com.alphaflow.engine.configs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChartPatternConfigTest {

  @Test
  void testConfigProperties() {
    ChartPatternConfig config = new ChartPatternConfig();

    assertFalse(config.isEnabled());
    config.setEnabled(true);
    assertTrue(config.isEnabled());
  }
}
