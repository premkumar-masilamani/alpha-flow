package com.alphaflow.engine.indicators;

import com.alphaflow.engine.indicators.dtos.IndicatorComputationPoint;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface Indicator {

  IndicatorType type();

  Map<LocalDate, Map<String, BigDecimal>> compute(
      List<PriceBar> bars, IndicatorParams params, PriceSource source);

  default boolean isStateful() {
    return false;
  }

  int requiredLookback(IndicatorParams params);

  default boolean hasCompleteState(Map<String, BigDecimal> state) {
    return true;
  }

  default Map<LocalDate, IndicatorComputationPoint> computeStateful(
      List<PriceBar> bars,
      IndicatorParams params,
      PriceSource source,
      Map<String, BigDecimal> initialState) {
    Map<LocalDate, Map<String, BigDecimal>> raw = compute(bars, params, source);
    Map<LocalDate, IndicatorComputationPoint> result = new LinkedHashMap<>(raw.size());
    for (Map.Entry<LocalDate, Map<String, BigDecimal>> entry : raw.entrySet()) {
      result.put(entry.getKey(), IndicatorComputationPoint.stateless(entry.getValue()));
    }
    return result;
  }
}
