package com.alphaflow.engine.indicators.utils;

import com.alphaflow.engine.indicators.Indicator;
import com.alphaflow.persistence.enums.IndicatorType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class IndicatorRegistry {

  private final Map<IndicatorType, Indicator> byType = new EnumMap<>(IndicatorType.class);

  public IndicatorRegistry(List<Indicator> indicators) {
    for (Indicator indicator : indicators) {
      Indicator existing = byType.put(indicator.type(), indicator);
      if (existing != null) {
        throw new IllegalStateException(
            "Duplicate Indicator beans for type "
                + indicator.type()
                + ": "
                + existing.getClass().getName()
                + " and "
                + indicator.getClass().getName());
      }
    }
  }

  public Indicator get(IndicatorType type) {
    Indicator indicator = byType.get(type);
    if (indicator == null) {
      throw new IllegalArgumentException("No Indicator registered for type " + type);
    }
    return indicator;
  }
}
