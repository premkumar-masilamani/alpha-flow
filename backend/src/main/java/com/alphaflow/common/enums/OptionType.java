package com.alphaflow.common.enums;

public enum OptionType {
  CE,
  PE;

  public static OptionType from(String value) {
    if (value != null && !value.isBlank()) {
      String normalized = value.trim().toUpperCase();
      for (OptionType type : values()) {
        if (type.name().equals(normalized)) {
          return type;
        }
      }
      throw new IllegalArgumentException("Unknown option type: " + value);
    } else {
      throw new IllegalArgumentException("Option type cannot be null or blank");
    }
  }
}
