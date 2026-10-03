package com.alphaflow.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static Timeframe from(String text) {
    if (text == null || text.trim().isEmpty()) {
      return null;
    }
    String normalized = text.trim().toUpperCase();
    for (Timeframe tf : values()) {
      if (tf.name().equals(normalized) || tf.value.equalsIgnoreCase(normalized)) {
        return tf;
      }
    }
    throw new IllegalArgumentException("Unknown timeframe: " + text);
  }
}
