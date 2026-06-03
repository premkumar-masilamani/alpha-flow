package com.alphaflow.api.configs;

import com.alphaflow.persistence.enums.Timeframe;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * API read tunables. {@code window} caps how many of the most recent bars the OHLCV and indicator
 *
 * <p>endpoints return, per timeframe, so chart overlays line up with the candles they sit on. Bound
 *
 * <p>from {@code alphaflow.api.window}; falls back to {@link #DEFAULT_WINDOW} when unset.
 */
@Configuration
@ConfigurationProperties(prefix = "alphaflow.api")
@Data
public class ApiProperties {

  private static final int DEFAULT_WINDOW = 180;

  private Object window = DEFAULT_WINDOW;

  public int windowFor(Timeframe timeframe) {
    if (window instanceof java.util.Map) {
      java.util.Map<?, ?> map = (java.util.Map<?, ?>) window;
      Object val = map.get(timeframe);
      if (val == null) {
        val = map.get(timeframe.name());
      }
      if (val == null) {
        val = map.get(timeframe.name().toLowerCase());
      }
      if (val instanceof Number) {
        return ((Number) val).intValue();
      } else if (val instanceof String) {
        try {
          return Integer.parseInt((String) val);
        } catch (NumberFormatException ignored) {
          // Ignored intentionally
        }
      }
    } else if (window instanceof Number) {
      return ((Number) window).intValue();
    } else if (window instanceof String) {
      try {
        return Integer.parseInt((String) window);
      } catch (NumberFormatException ignored) {
        // Ignored intentionally
      }
    }
    return DEFAULT_WINDOW;
  }
}
