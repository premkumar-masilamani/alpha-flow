package com.alphaflow.common.enums;

import lombok.Getter;

@Getter
public enum Timeframe {
  DAILY("DAILY"),
  WEEKLY("WEEKLY"),
  _15M("15M");

  private final String value;

  Timeframe(String value) {
    this.value = value;
  }
}
