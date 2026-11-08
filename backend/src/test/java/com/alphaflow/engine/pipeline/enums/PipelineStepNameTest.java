package com.alphaflow.engine.pipeline.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PipelineStepNameTest {

  @Test
  void testFromValidStepNamesCaseInsensitiveAndTrimmed() {
    for (PipelineStepName stepName : PipelineStepName.values()) {
      assertEquals(stepName, PipelineStepName.from(stepName.name()));
      assertEquals(stepName, PipelineStepName.from("  " + stepName.name().toLowerCase() + "  "));
    }
  }

  @Test
  void testFromNullBlankOrUnknownThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> PipelineStepName.from(null));
    assertThrows(IllegalArgumentException.class, () -> PipelineStepName.from(""));
    assertThrows(IllegalArgumentException.class, () -> PipelineStepName.from("   "));
    assertThrows(IllegalArgumentException.class, () -> PipelineStepName.from("UNKNOWN_STEP"));
  }
}
