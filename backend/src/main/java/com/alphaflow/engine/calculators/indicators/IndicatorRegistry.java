package com.alphaflow.engine.calculators.indicators;

import com.alphaflow.persistence.enums.IndicatorType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Resolves an {@link IndicatorType} to its {@link Indicator} implementation.
 *
 * <p>Spring injects every {@link Indicator} bean; adding a new indicator family is just a new bean
 * —
 *
 * <p>it registers itself here automatically with no change to the engine.
 */
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
