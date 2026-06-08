package com.alphaflow.engine.calculators.indicators;

import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Moving Average Convergence Divergence: three chained EMAs over a source field. */
@Component
@Slf4j
public class MacdIndicator implements Indicator {

  @Override
  public IndicatorType type() {
    return IndicatorType.MACD;
  }

  @Override
  public Map<LocalDate, Map<String, BigDecimal>> compute(
      List<PriceBar> bars, IndicatorParams params, PriceSource source) {
    int fastPeriod = params.getInt("fast");
    int slowPeriod = params.getInt("slow");
    int signalPeriod = params.getInt("signal", 9);

    log.debug(
        "Computing MACD indicator for {} bars, fast={}, slow={}, signal={}, source={}",
        bars.size(),
        fastPeriod,
        slowPeriod,
        signalPeriod,
        source);

    EmaAccumulator fast = EmaAccumulator.fresh(fastPeriod);
    EmaAccumulator slow = EmaAccumulator.fresh(slowPeriod);
    EmaAccumulator signal = EmaAccumulator.fresh(signalPeriod);

    Map<LocalDate, Map<String, BigDecimal>> values = new java.util.LinkedHashMap<>();

    for (PriceBar bar : bars) {
      BigDecimal v = bar.valueFor(source);
      fast.next(v);
      slow.next(v);

      if (!fast.isSeeded() || !slow.isSeeded()) {
        continue;
      }

      BigDecimal macd = IndicatorMath.internal(fast.current().subtract(slow.current()));
      Map<String, BigDecimal> barValues = new LinkedHashMap<>();
      barValues.put("macd", IndicatorMath.publish(macd));

      Optional<BigDecimal> signalEma = signal.next(macd);
      if (signalEma.isPresent()) {
        BigDecimal signalVal = signalEma.get();
        barValues.put("signal", IndicatorMath.publish(signalVal));

        BigDecimal histogram = IndicatorMath.internal(macd.subtract(signalVal));
        barValues.put("histogram", IndicatorMath.publish(histogram));
      }
      values.put(bar.date(), barValues);
    }

    return values;
  }
}
