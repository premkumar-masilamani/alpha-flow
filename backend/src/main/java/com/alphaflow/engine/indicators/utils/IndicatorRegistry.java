package com.alphaflow.engine.indicators.utils;

import com.alphaflow.engine.indicators.Indicator;
import com.alphaflow.persistence.enums.IndicatorType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IndicatorRegistry {

  private final Map<IndicatorType, Indicator> byType = new EnumMap<>(IndicatorType.class);

  public IndicatorRegistry(List<Indicator> indicators) {
    for (Indicator indicator : indicators) {
      Indicator existing = byType.put(indicator.type(), indicator);
      if (existing != null) {
        log.error(
            "Duplicate Indicator beans registered for type {}: {} and {}",
            indicator.type(),
            existing.getClass().getName(),
            indicator.getClass().getName());
        throw new IllegalStateException(
            "Duplicate Indicator beans for type "
                + indicator.type()
                + ": "
                + existing.getClass().getName()
                + " and "
                + indicator.getClass().getName());
      }
    }
    log.info("Registered {} technical indicator implementations.", byType.size());
  }

  public Indicator get(IndicatorType type) {
    Indicator indicator = byType.get(type);
    if (indicator == null) {
      log.error("No Indicator implementation registered for type {}", type);
      throw new IllegalArgumentException("No Indicator registered for type " + type);
    }
    return indicator;
  }
}
