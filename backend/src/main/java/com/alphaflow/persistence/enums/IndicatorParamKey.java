package com.alphaflow.persistence.enums;

import lombok.Getter;

@Getter
public enum IndicatorParamKey {
  PERIOD("period"),
  FAST("fast"),
  SLOW("slow"),
  SIGNAL("signal"),
  K("k"),
  K_SMOOTH("kSmooth"),
  D_SMOOTH("dSmooth"),
  STD_DEV("stdDev");

  private final String value;

  IndicatorParamKey(String value) {
    this.value = value;
  }
}
