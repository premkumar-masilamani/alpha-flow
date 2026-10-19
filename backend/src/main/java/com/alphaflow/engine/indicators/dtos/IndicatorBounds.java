package com.alphaflow.engine.indicators.dtos;

import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class IndicatorBounds {

  private final Integer lower;
  private final Integer upper;

  private IndicatorBounds(Integer lower, Integer upper) {
    this.lower = lower;
    this.upper = upper;
  }

  public static IndicatorBounds of(Integer lower, Integer upper) {
    return new IndicatorBounds(lower, upper);
  }

  public static IndicatorBounds of(Map<String, Integer> map) {
    if (map == null || map.isEmpty()) {
      return null;
    }
    return new IndicatorBounds(map.get("lower"), map.get("upper"));
  }

  public static IndicatorBounds parse(String canonical) {
    if (canonical == null || canonical.isBlank()) {
      return null;
    }
    Integer lower = null;
    Integer upper = null;
    for (String pair : canonical.split(",")) {
      String[] kv = pair.split("=", 2);
      if (kv.length != 2) {
        throw new IllegalArgumentException(
            "Malformed bounds pair: '" + pair + "' in '" + canonical + "'");
      }
      String key = kv[0].trim();
      int val = Integer.parseInt(kv[1].trim());
      if ("lower".equalsIgnoreCase(key)) {
        lower = val;
      } else if ("upper".equalsIgnoreCase(key)) {
        upper = val;
      }
    }
    return new IndicatorBounds(lower, upper);
  }

  public String canonical() {
    StringBuilder sb = new StringBuilder();
    if (lower != null) {
      sb.append("lower=").append(lower);
    }
    if (upper != null) {
      if (!sb.isEmpty()) {
        sb.append(',');
      }
      sb.append("upper=").append(upper);
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return canonical();
  }
}
