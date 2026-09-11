package com.alphaflow.engine.indicators;

import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.EmaAccumulator;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import com.alphaflow.persistence.enums.IndicatorOutputKey;
import com.alphaflow.persistence.enums.IndicatorParamKey;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    int period = params.getInt(IndicatorParamKey.PERIOD);
    log.debug(
        "Computing EMA indicator for {} bars, period={}, source={}", bars.size(), period, source);
    // 1. Instantiate a stateful accumulator for the given period
    EmaAccumulator acc = EmaAccumulator.fresh(period);
    Map<LocalDate, Map<String, BigDecimal>> values = new java.util.LinkedHashMap<>();

    // 2. Stream all price bars chronologically
    for (PriceBar bar : bars) {
      // Extract the value (e.g. CLOSE or VOLUME) and feed it to the accumulator
      Optional<BigDecimal> ema = acc.next(bar.valueFor(source));

      // 3. If the accumulator is seeded, save the published value (4 decimal places)
      ema.ifPresent(
          emaVal ->
              values.put(
                  bar.date(),
                  Map.of(IndicatorOutputKey.VALUE.getValue(), IndicatorMath.publish(emaVal))));
    }

    return values;
  }
}
