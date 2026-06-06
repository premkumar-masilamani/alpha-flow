package com.alphaflow.engine.calculators.indicators;

import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;

/** Exponential moving average over a configurable source field, with standard SMA seeding. */
@Component
public class EmaIndicator implements Indicator {

  @Override
  public IndicatorType type() {
    return IndicatorType.EMA;
  }

  @Override
  public List<PlotPoint> compute(List<PriceBar> bars, IndicatorParams params, PriceSource source) {
    int period = params.getInt("period");
    EmaAccumulator acc = EmaAccumulator.fresh(period);
    List<PlotPoint> values = new ArrayList<>();

    for (PriceBar bar : bars) {
      Optional<BigDecimal> ema = acc.next(bar.valueFor(source));
      ema.ifPresent(v -> values.add(new PlotPoint(bar.date(), "value", IndicatorMath.publish(v))));
    }

    return values;
  }
}
