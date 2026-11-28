package com.alphaflow.engine.calculators.indicators;

import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.EPOCH;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.bd;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.closes;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.plot;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.walk;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.indicators.MacdIndicator;
import com.alphaflow.engine.indicators.dtos.IndicatorComputationPoint;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MacdIndicatorTest {

  @Test
  void macdConstantSeriesIsZero() {

    double[] flat = new double[60];

    java.util.Arrays.fill(flat, 50.0);

    Map<LocalDate, Map<String, BigDecimal>> result =
        new MacdIndicator()
            .compute(
                closes(flat), IndicatorParams.parse("fast=12,slow=26,signal=9"), PriceSource.CLOSE);

    assertFalse(result.isEmpty());

    result
        .values()
        .forEach(barMap ->
            barMap.forEach((key, val) ->
                assertEquals(
                    0,
                    val.compareTo(bd(0)),
                    "constant series must give zero macd/signal/histogram (" + key + ")")));
  }

  @Test
  void macdEmitsThreePlotsOnceDefined() {

    Map<LocalDate, Map<String, BigDecimal>> result =
        new MacdIndicator()
            .compute(
                walk(80), IndicatorParams.parse("fast=12,slow=26,signal=9"), PriceSource.CLOSE);

    // Last bar (well past warm-up) must carry all three plots.

    LocalDate last = EPOCH.plusDays(79);

    assertNotNull(plot(result, last, "macd"));

    assertNotNull(plot(result, last, "signal"));

    assertNotNull(plot(result, last, "histogram"));

    // histogram == macd - signal at that bar.

    BigDecimal macd = plot(result, last, "macd");

    BigDecimal signal = plot(result, last, "signal");

    BigDecimal hist = plot(result, last, "histogram");

    assertEquals(0, hist.compareTo(macd.subtract(signal)));
  }

  @Test
  void macdDefaultSignalPeriod() {

    Map<LocalDate, Map<String, BigDecimal>> result =
        new MacdIndicator()
            .compute(walk(50), IndicatorParams.parse("fast=12,slow=26"), PriceSource.CLOSE);

    assertFalse(result.isEmpty());
  }

  @Test
  void macdStatefulResumeMatchesFullPassBitForBit() {
    MacdIndicator indicator = new MacdIndicator();
    IndicatorParams params = IndicatorParams.parse("fast=12,slow=26,signal=9");

    assertTrue(indicator.isStateful());
    assertEquals(34, indicator.requiredLookback(params));
    assertFalse(indicator.hasCompleteState(null));
    assertFalse(indicator.hasCompleteState(Map.of()));
    assertFalse(indicator.hasCompleteState(Map.of(MacdIndicator.STATE_FAST_EMA, BigDecimal.ONE)));
    assertFalse(
        indicator.hasCompleteState(
            Map.of(
                MacdIndicator.STATE_FAST_EMA,
                BigDecimal.ONE,
                MacdIndicator.STATE_SLOW_EMA,
                BigDecimal.ONE)));

    List<PriceBar> bars = walk(100);
    Map<LocalDate, IndicatorComputationPoint> fullPass =
        indicator.computeStateful(bars, params, PriceSource.CLOSE, null);

    // Warm-up bar before signal is seeded has incomplete state
    Map<String, BigDecimal> warmupState = fullPass.get(bars.get(25).date()).state();
    assertFalse(indicator.hasCompleteState(warmupState));

    Map<LocalDate, IndicatorComputationPoint> firstHalf =
        indicator.computeStateful(bars.subList(0, 50), params, PriceSource.CLOSE, null);
    Map<String, BigDecimal> state = firstHalf.get(bars.get(49).date()).state();
    assertTrue(indicator.hasCompleteState(state));

    for (int i = 50; i < bars.size(); i++) {
      PriceBar bar = bars.get(i);
      Map<LocalDate, IndicatorComputationPoint> incremental =
          indicator.computeStateful(List.of(bar), params, PriceSource.CLOSE, state);
      IndicatorComputationPoint expected = fullPass.get(bar.date());
      IndicatorComputationPoint actual = incremental.get(bar.date());
      assertEquals(expected.values(), actual.values());
      assertEquals(expected.state(), actual.state());
      state = actual.state();
    }
  }
}
