package com.alphaflow.engine.calculators.indicators;

import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;

/** Moving Average Convergence Divergence: three chained EMAs over a source field. */
@Component
public class MacdIndicator implements Indicator {

  @Override
  public IndicatorType type() {
    return IndicatorType.MACD;
  }

  @Override
  public List<PlotPoint> compute(List<PriceBar> bars, IndicatorParams params, PriceSource source) {
    int fastPeriod = params.getInt("fast");
    int slowPeriod = params.getInt("slow");
    int signalPeriod = params.getInt("signal", 9);

    EmaAccumulator fast = EmaAccumulator.fresh(fastPeriod);
    EmaAccumulator slow = EmaAccumulator.fresh(slowPeriod);
    EmaAccumulator signal = EmaAccumulator.fresh(signalPeriod);

    List<PlotPoint> values = new ArrayList<>();

    for (PriceBar bar : bars) {
      BigDecimal v = bar.valueFor(source);
      fast.next(v);
      slow.next(v);

      if (!fast.isSeeded() || !slow.isSeeded()) {
        continue;
      }

      BigDecimal macd = IndicatorMath.internal(fast.current().subtract(slow.current()));
      values.add(new PlotPoint(bar.date(), "macd", IndicatorMath.publish(macd)));

      Optional<BigDecimal> signalEma = signal.next(macd);
      if (signalEma.isPresent()) {
        BigDecimal signalVal = signalEma.get();
        values.add(new PlotPoint(bar.date(), "signal", IndicatorMath.publish(signalVal)));

        BigDecimal histogram = IndicatorMath.internal(macd.subtract(signalVal));
        values.add(new PlotPoint(bar.date(), "histogram", IndicatorMath.publish(histogram)));
      }
    }

    return values;
  }
}
