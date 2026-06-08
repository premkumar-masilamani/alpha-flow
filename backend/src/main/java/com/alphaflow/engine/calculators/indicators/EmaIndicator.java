package com.alphaflow.engine.calculators.indicators;

import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Exponential moving average over a configurable source field, with standard SMA seeding. */
@Component
@Slf4j
public class EmaIndicator implements Indicator {

  @Override
  public IndicatorType type() {
    return IndicatorType.EMA;
  }

  @Override
  public Map<LocalDate, Map<String, BigDecimal>> compute(
      List<PriceBar> bars, IndicatorParams params, PriceSource source) {
    int period = params.getInt("period");
    log.debug(
        "Computing EMA indicator for {} bars, period={}, source={}", bars.size(), period, source);
    EmaAccumulator acc = EmaAccumulator.fresh(period);
    Map<LocalDate, Map<String, BigDecimal>> values = new java.util.LinkedHashMap<>();

    for (PriceBar bar : bars) {
      Optional<BigDecimal> ema = acc.next(bar.valueFor(source));
      ema.ifPresent(v -> values.put(bar.date(), Map.of("value", IndicatorMath.publish(v))));
    }

    return values;
  }
}
