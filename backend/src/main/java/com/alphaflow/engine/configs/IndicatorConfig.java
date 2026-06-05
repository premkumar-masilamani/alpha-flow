package com.alphaflow.engine.configs;

import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import com.alphaflow.persistence.enums.Timeframe;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * The global, hardcoded indicator matrix: which (indicator, source, params) combinations are
 *
 * <p>computed on which timeframe. Bound from {@code alphaflow.indicators.*} so the set can be tuned
 * via
 *
 * <p>configuration without recompiling the engine. The indicator math lives in code; only the
 *
 * <p>which-combos-to-run list is externalized here.
 *
 * <pre>
 *
 * alphaflow.indicators.timeframes.daily[0].type=EMA
 *
 * alphaflow.indicators.timeframes.daily[0].source=CLOSE
 *
 * alphaflow.indicators.timeframes.daily[0].params.period=5
 *
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "alphaflow.indicators")
@Data
public class IndicatorConfig {

  /** Indicator definitions keyed by timeframe. */
  private Map<Timeframe, List<IndicatorDefinition>> timeframes = new EnumMap<>(Timeframe.class);

  /** The configured indicators for a timeframe, or an empty list if none. */
  public List<IndicatorDefinition> forTimeframe(Timeframe timeframe) {

    return timeframes.getOrDefault(timeframe, List.of());
  }

  @Data
  public static class IndicatorDefinition {

    private IndicatorType type;

    private PriceSource source = PriceSource.CLOSE;

    /**
     * Integer periods keyed by name, e.g. {@code period=14} or {@code fast=12,slow=26,signal=9}.
     */
    private Map<String, Integer> params = new LinkedHashMap<>();

    private Integer upperBound;

    private Integer lowerBound;
  }
}
