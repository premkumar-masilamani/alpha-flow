package com.alphaflow.engine.calculators.indicators;

import java.util.Map;
import java.util.TreeMap;

/**
 * Parsed indicator parameters (integer periods) plus their canonical string form.
 *
 * <p>The canonical string (e.g. {@code "period=14"}, {@code "fast=12,signal=9,slow=26"}) is what
 * gets
 *
 * <p>stored in the {@code params} column and forms part of an indicator's natural key. Keys are
 * sorted
 *
 * <p>so the same logical configuration always serializes to the exact same string regardless of how
 * the
 *
 * <p>source map was built (e.g. config-binding order) — this keeps the natural key stable across
 * runs.
 *
 * <p>Lookups are by name, so ordering never affects computation.
 */
public final class IndicatorParams {

  private final Map<String, Integer> values;

  private IndicatorParams(Map<String, Integer> values) {

    this.values = values;
  }

  /** Builds from a map of name → period (key order is irrelevant; canonical form is sorted). */
  public static IndicatorParams of(Map<String, Integer> values) {

    return new IndicatorParams(new TreeMap<>(values));
  }

  /** Parses a canonical string like {@code "fast=12,signal=9,slow=26"}. */
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

  /** Required integer param; throws if absent. */
  public int getInt(String name) {

    Integer v = values.get(name);

    if (v == null) {

      throw new IllegalArgumentException("Missing required param '" + name + "' in " + canonical());
    }

    return v;
  }

  /** Optional integer param with a fallback default. */
  public int getInt(String name, int defaultValue) {

    return values.getOrDefault(name, defaultValue);
  }

  /** The stable canonical string used for storage and as part of the natural key. */
  public String canonical() {

    StringBuilder sb = new StringBuilder();

    for (Map.Entry<String, Integer> e : values.entrySet()) {

      if (!sb.isEmpty()) {

        sb.append(',');
      }

      sb.append(e.getKey()).append('=').append(e.getValue());
    }

    return sb.toString();
  }

  @Override
  public String toString() {

    return canonical();
  }
}
