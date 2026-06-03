package com.alphaflow.engine.calculators.indicators;

import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Relative Strength Index using Wilder's smoothing.
 *
 * <p>Seeding: the first {@code period} gains/losses are simple-averaged; thereafter Wilder
 * smoothing applies: {@code avg = (avgPrev·(period−1) + current) / period}. {@code RSI = 100 −
 * 100/(1+RS)} with {@code RS = avgGain/avgLoss}; a zero average loss yields RSI 100.
 *
 * <p>Recursive: state is {@code {"avgGain","avgLoss","prevClose"}} — {@code prevClose} (the last
 * source value, named for the usual close source) is needed to compute the next delta on resume.
 */
@Component
public class RsiIndicator implements Indicator {

  private static BigDecimal wilder(BigDecimal avgPrev, BigDecimal current, BigDecimal period) {
    BigDecimal smoothed = avgPrev.multiply(period.subtract(BigDecimal.ONE)).add(current);
    return IndicatorMath.divide(smoothed, period);
  }

  private static BigDecimal rsi(BigDecimal avgGain, BigDecimal avgLoss) {
    if (avgLoss.signum() == 0) {
      return IndicatorMath.publish(IndicatorMath.HUNDRED);
    }
    BigDecimal rs = IndicatorMath.divide(avgGain, avgLoss);
    BigDecimal rsi =
        IndicatorMath.HUNDRED.subtract(
            IndicatorMath.divide(IndicatorMath.HUNDRED, BigDecimal.ONE.add(rs)));
    return IndicatorMath.publish(rsi);
  }

  @Override
  public IndicatorType type() {
    return IndicatorType.RSI;
  }

  @Override
  public IndicatorResult compute(
      List<PriceBar> bars, String priorStateJson, IndicatorParams params, PriceSource source) {
    int period = params.getInt("period");
    if (period < 1) {
      throw new IllegalArgumentException("RSI period must be >= 1. Provided: " + period);
    }
    BigDecimal periodBd = BigDecimal.valueOf(period);

    Map<String, BigDecimal> prior = StateCodec.decode(priorStateJson);
    BigDecimal avgGain = prior.get("avgGain");
    BigDecimal avgLoss = prior.get("avgLoss");
    BigDecimal prevValue = prior.get("prevClose");
    boolean seeded = avgGain != null && avgLoss != null;

    List<BigDecimal> seedGains = new ArrayList<>();
    List<BigDecimal> seedLosses = new ArrayList<>();
    List<PlotPoint> values = new ArrayList<>();

    for (PriceBar bar : bars) {
      BigDecimal value = bar.valueFor(source);
      if (prevValue == null) {
        // First value seen on a cold backfill: no delta yet.
        prevValue = value;
        continue;
      }

      BigDecimal delta = value.subtract(prevValue);
      BigDecimal gain = delta.signum() > 0 ? delta : BigDecimal.ZERO;
      BigDecimal loss = delta.signum() < 0 ? delta.negate() : BigDecimal.ZERO;

      if (!seeded) {
        seedGains.add(gain);
        seedLosses.add(loss);
        if (seedGains.size() == period) {
          avgGain = IndicatorMath.average(seedGains);
          avgLoss = IndicatorMath.average(seedLosses);
          seeded = true;
          values.add(new PlotPoint(bar.date(), "value", rsi(avgGain, avgLoss)));
        }
      } else {
        avgGain = wilder(avgGain, gain, periodBd);
        avgLoss = wilder(avgLoss, loss, periodBd);
        values.add(new PlotPoint(bar.date(), "value", rsi(avgGain, avgLoss)));
      }
      prevValue = value;
    }

    String newState = null;
    if (seeded) {
      Map<String, BigDecimal> state = new LinkedHashMap<>();
      state.put("avgGain", avgGain);
      state.put("avgLoss", avgLoss);
      state.put("prevClose", prevValue);
      newState = StateCodec.encode(state);
    }
    return new IndicatorResult(values, newState);
  }
}
