package com.alphaflow.engine.indicators.dtos;

import com.alphaflow.persistence.enums.IndicatorParamKey;
import java.util.Map;
import java.util.TreeMap;
import lombok.Getter;

@Getter
public final class IndicatorParams {

  private final Map<String, Integer> values;

  private IndicatorParams(Map<String, Integer> values) {
    this.values = values;
  }

  public static IndicatorParams of(Map<String, Integer> values) {
    return new IndicatorParams(new TreeMap<>(values));
  }

  public static IndicatorParams parse(String canonical) {
    Map<String, Integer> parsed = new TreeMap<>();
    if (canonical != null && !canonical.isBlank()) {
      for (String pair : canonical.split(",")) {
        String[] kv = pair.split("=", 2);
        if (kv.length != 2) {
          throw new IllegalArgumentException(
              "Malformed param pair: '" + pair + "' in '" + canonical + "'");
        }
        parsed.put(kv[0].trim(), Integer.valueOf(kv[1].trim()));
      }
    }
    return new IndicatorParams(parsed);
  }

  public int getInt(String name) {
    Integer val = values.get(name);
    if (val == null) {
      throw new IllegalArgumentException("Missing required param '" + name + "' in " + canonical());
    }
    return val;
  }

  public int getInt(IndicatorParamKey key) {
    return getInt(key.getValue());
  }

  public int getInt(String name, int defaultValue) {
    return values.getOrDefault(name, defaultValue);
  }

  public int getInt(IndicatorParamKey key, int defaultValue) {
    return getInt(key.getValue(), defaultValue);
  }

  public String canonical() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Integer> entry : values.entrySet()) {
      if (!sb.isEmpty()) {
        sb.append(',');
      }
      sb.append(entry.getKey()).append('=').append(entry.getValue());
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return canonical();
  }
}
