package com.alphaflow.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum NotificationStatus {
  SENT("SENT"),
  FAILED("FAILED");

  private final String value;

  NotificationStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static NotificationStatus from(String text) {
    if (text != null && !text.trim().isEmpty()) {
      String normalized = text.trim().toUpperCase();
      for (NotificationStatus status : values()) {
        if (status.name().equals(normalized)) {
          return status;
        }
      }
      throw new IllegalArgumentException("Unknown notification status: " + text);
    } else {
      return null;
    }
  }
}
