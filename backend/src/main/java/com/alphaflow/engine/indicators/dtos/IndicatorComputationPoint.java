package com.alphaflow.engine.indicators.dtos;

import java.math.BigDecimal;
import java.util.Map;

public record IndicatorComputationPoint(
    Map<String, BigDecimal> values, Map<String, BigDecimal> state) {

  public static IndicatorComputationPoint stateless(Map<String, BigDecimal> values) {
    return new IndicatorComputationPoint(values, null);
  }

  public static IndicatorComputationPoint stateful(
      Map<String, BigDecimal> values, Map<String, BigDecimal> state) {
    return new IndicatorComputationPoint(values, state);
  }
}
