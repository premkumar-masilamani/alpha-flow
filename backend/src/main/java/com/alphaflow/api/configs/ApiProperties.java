package com.alphaflow.api.configs;

import com.alphaflow.persistence.enums.Timeframe;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

/**
 * API read tunables. {@code window} caps how many of the most recent bars the OHLCV and indicator
 * endpoints return, per timeframe, so chart overlays line up with the candles they sit on. Bound
 * from {@code alphaflow.api.window.<timeframe>}; falls back to {@link #DEFAULT_WINDOW} when unset.
 */
@Configuration
@ConfigurationProperties(prefix = "alphaflow.api")
@Data
public class ApiProperties {

    private static final int DEFAULT_WINDOW = 180;

    private Map<Timeframe, Integer> window = new EnumMap<>(Timeframe.class);

    public int windowFor(Timeframe timeframe) {
        return window.getOrDefault(timeframe, DEFAULT_WINDOW);
    }
}
