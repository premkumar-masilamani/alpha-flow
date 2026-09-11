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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    // 1. Setup and parameter retrieval
    int fastPeriod = params.getInt(IndicatorParamKey.FAST);
    int slowPeriod = params.getInt(IndicatorParamKey.SLOW);
    int signalPeriod = params.getInt(IndicatorParamKey.SIGNAL, 9);

    log.debug(
        "Computing MACD indicator for {} bars, fast={}, slow={}, signal={}, source={}",
        bars.size(),
        fastPeriod,
        slowPeriod,
        signalPeriod,
        source);

    // Create stateful fast, slow, and signal accumulators
    EmaAccumulator fast = EmaAccumulator.fresh(fastPeriod);
    EmaAccumulator slow = EmaAccumulator.fresh(slowPeriod);
    EmaAccumulator signal = EmaAccumulator.fresh(signalPeriod);

    Map<LocalDate, Map<String, BigDecimal>> values = new java.util.LinkedHashMap<>();

    for (PriceBar bar : bars) {
      BigDecimal value = bar.valueFor(source);
      // Feed values to fast and slow EMAs
      fast.next(value);
      slow.next(value);

      // 2. Seeding stage: wait until both fast & slow EMAs are seeded (usually limited by slow
      // period)
      if (!fast.isSeeded() || !slow.isSeeded()) {
        continue;
      }

      // 3. Calculate MACD Line = Fast EMA - Slow EMA (calculated at 12 decimals internal scale)
      BigDecimal macd = IndicatorMath.internal(fast.current().subtract(slow.current()));
      Map<String, BigDecimal> barValues = new LinkedHashMap<>();
      barValues.put(IndicatorOutputKey.MACD.getValue(), IndicatorMath.publish(macd));

      // Feed MACD line value into the signal EMA accumulator
      Optional<BigDecimal> signalEma = signal.next(macd);

      // 4. Seeding stage for signal EMA: wait until signal EMA is seeded
      if (signalEma.isPresent()) {
        BigDecimal signalVal = signalEma.get();
        barValues.put(IndicatorOutputKey.SIGNAL.getValue(), IndicatorMath.publish(signalVal));

        // Calculate Histogram = MACD Line - Signal Line
        BigDecimal histogram = IndicatorMath.internal(macd.subtract(signalVal));
        barValues.put(IndicatorOutputKey.HISTOGRAM.getValue(), IndicatorMath.publish(histogram));
      }
      values.put(bar.date(), barValues);
    }

    return values;
  }
}
