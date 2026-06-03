package com.alphaflow.engine.calculators.indicators;


import com.alphaflow.persistence.enums.IndicatorType;

import com.alphaflow.persistence.enums.PriceSource;

import org.springframework.stereotype.Component;


import java.math.BigDecimal;

import java.util.*;


/**

 * Moving Average Convergence Divergence: three chained EMAs over a source field.

 * <ul>

 *   <li>{@code macd} = fastEMA − slowEMA (defined once the slow EMA is seeded)</li>

 *   <li>{@code signal} = EMA(signalPeriod) of the macd line</li>

 *   <li>{@code histogram} = macd − signal</li>

 * </ul>

 * Recursive: state is {@code {"fastEma","slowEma","signalEma"}}. State (and therefore a resume

 * checkpoint) is only produced once the signal EMA is seeded — before that the warm-up is recomputed

 * each run — which avoids having to serialize the signal EMA's partial seeding window.

 */

@Component

public class MacdIndicator implements Indicator {


  @Override

  public IndicatorType type() {

    return IndicatorType.MACD;

  }


  @Override

  public IndicatorResult compute(List<PriceBar> bars, String priorStateJson, IndicatorParams params, PriceSource source) {

    int fastPeriod = params.getInt("fast");

    int slowPeriod = params.getInt("slow");

    int signalPeriod = params.getInt("signal", 9);


    EmaAccumulator fast;

    EmaAccumulator slow;

    EmaAccumulator signal;

    if (priorStateJson != null) {

      Map<String, BigDecimal> prior = StateCodec.decode(priorStateJson);

      fast = EmaAccumulator.seeded(fastPeriod, prior.get("fastEma"));

      slow = EmaAccumulator.seeded(slowPeriod, prior.get("slowEma"));

      signal = EmaAccumulator.seeded(signalPeriod, prior.get("signalEma"));

    } else {

      fast = EmaAccumulator.fresh(fastPeriod);

      slow = EmaAccumulator.fresh(slowPeriod);

      signal = EmaAccumulator.fresh(signalPeriod);

    }


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


    String newState = null;

    if (signal.isSeeded()) {

      Map<String, BigDecimal> state = new LinkedHashMap<>();

      state.put("fastEma", fast.current());

      state.put("slowEma", slow.current());

      state.put("signalEma", signal.current());

      newState = StateCodec.encode(state);

    }

    return new IndicatorResult(values, newState);

  }

}

