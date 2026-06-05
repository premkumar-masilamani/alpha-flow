package com.alphaflow.api.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * API read tunables. {@code window} caps how many of the most recent bars the OHLCV and indicator
 *
 * <p>endpoints return, per timeframe, so chart overlays line up with the candles they sit on. Bound
 */
@Configuration
@ConfigurationProperties(prefix = "alphaflow.chart")
@Data
public class ChartConfig {
  private int window = 180;
}
