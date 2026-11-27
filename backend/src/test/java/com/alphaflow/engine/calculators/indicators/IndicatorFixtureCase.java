package com.alphaflow.engine.calculators.indicators;

import com.alphaflow.engine.indicators.Indicator;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.persistence.enums.PriceSource;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public record IndicatorFixtureCase(
    String name,
    Indicator indicator,
    IndicatorParams params,
    PriceSource source,
    Map<String, String> outputKeyToColumn) {

  public IndicatorFixtureCase {
    if (StringUtils.isNotBlank(name)
        && indicator != null
        && params != null
        && source != null
        && outputKeyToColumn != null
        && !outputKeyToColumn.isEmpty()) {
      outputKeyToColumn = Map.copyOf(outputKeyToColumn);
    } else {
      throw new IllegalArgumentException(
          "IndicatorFixtureCase requires non-blank name, non-null indicator/params/source, "
              + "and non-empty outputKeyToColumn");
    }
  }
}
