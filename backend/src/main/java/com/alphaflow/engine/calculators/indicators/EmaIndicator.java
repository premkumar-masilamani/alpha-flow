package com.alphaflow.engine.calculators.indicators;

import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * Exponential moving average over a configurable source field, with standard SMA seeding.
 *
 * <p>Recursive: resumes from the persisted EMA value ({@code {"ema": "..."}}) so a single new bar
 * can be computed without replaying history. See {@link EmaAccumulator} for the seeding/recurrence
 * math.
 */
@Component
public class EmaIndicator implements Indicator {

  @Override
  public IndicatorType type() {
    return IndicatorType.EMA;
  }

  @Override
  public IndicatorResult compute(
      List<PriceBar> bars, String priorStateJson, IndicatorParams params, PriceSource source) {
    int period = params.getInt("period");

    EmaAccumulator acc;
    if (priorStateJson != null) {
      BigDecimal ema = StateCodec.decode(priorStateJson).get("ema");
      acc = EmaAccumulator.seeded(period, ema);
    } else {
      acc = EmaAccumulator.fresh(period);
    }

    List<PlotPoint> values = new ArrayList<>();
    for (PriceBar bar : bars) {
      Optional<BigDecimal> ema = acc.next(bar.valueFor(source));
      ema.ifPresent(v -> values.add(new PlotPoint(bar.date(), "value", IndicatorMath.publish(v))));
    }

    String newState = null;
    if (acc.isSeeded()) {
      Map<String, BigDecimal> state = new LinkedHashMap<>();
      state.put("ema", acc.current());
      newState = StateCodec.encode(state);
    }
    return new IndicatorResult(values, newState);
  }
}
