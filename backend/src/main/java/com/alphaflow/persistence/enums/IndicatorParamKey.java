package com.alphaflow.persistence.enums;

import lombok.Getter;

/**
 * Represents the configuration parameter keys stored inside the JSONB params database column of the
 * indicator_definitions table.
 */
@Getter
public enum IndicatorParamKey {
  PERIOD("period"),
  FAST("fast"),
  SLOW("slow"),
  SIGNAL("signal"),
  K("k"),
  K_SMOOTH("kSmooth"),
  D_SMOOTH("dSmooth");

  private final String value;

  IndicatorParamKey(String value) {
    this.value = value;
  }
}
