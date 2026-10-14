package com.alphaflow.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Timeframe {
  DAILY("DAILY"),
  WEEKLY("WEEKLY");

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
      if (tf.name().equals(normalized)) {
        return tf;
      }
    }
    throw new IllegalArgumentException("Unknown timeframe: " + text);
  }
}
