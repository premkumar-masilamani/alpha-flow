package com.alphaflow.api.configs;

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

    private int window;
}
