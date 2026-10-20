package com.alphaflow.engine.strategies.options.enums;

public enum OptionType {
  CALL("CE"),
  PUT("PE");

  private final String code;

  OptionType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static OptionType from(String value) {
    if (value != null && !value.isBlank()) {
      String normalized = value.trim().toUpperCase();
      for (OptionType type : values()) {
        if (type.name().equals(normalized) || type.code.equals(normalized)) {
          return type;
        }
      }
      throw new IllegalArgumentException("Unknown option type: " + value);
    } else {
      throw new IllegalArgumentException("Option type cannot be null or blank");
    }
  }
}
