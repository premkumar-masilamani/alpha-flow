package com.alphaflow.engine.calculators.indicators;

import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.bd;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.closes;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.walk;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.indicators.RsiIndicator;
import com.alphaflow.engine.indicators.dtos.IndicatorComputationPoint;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RsiIndicatorTest {

  @Test
  void rsiAllGainsIsHundred() {

    Map<LocalDate, Map<String, BigDecimal>> result =
        new RsiIndicator()
            .compute(
                closes(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16),
                IndicatorParams.parse("period=14"),
                PriceSource.CLOSE);

    assertFalse(result.isEmpty());

    result
        .values()
        .forEach(barMap ->
            assertEquals(0, barMap.get("value").compareTo(bd(100)), "all-gains RSI must be 100"));
  }

  @Test
  void rsiAllLossesIsZero() {

    Map<LocalDate, Map<String, BigDecimal>> result =
        new RsiIndicator()
            .compute(
                closes(16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1),
                IndicatorParams.parse("period=14"),
                PriceSource.CLOSE);

    assertFalse(result.isEmpty());

    result
        .values()
        .forEach(barMap ->
            assertEquals(0, barMap.get("value").compareTo(bd(0)), "all-losses RSI must be 0"));
  }

  @Test
  void rsiStaysInRange() {

    Map<LocalDate, Map<String, BigDecimal>> result =
        new RsiIndicator()
            .compute(walk(100), IndicatorParams.parse("period=14"), PriceSource.CLOSE);

    result
        .values()
        .forEach(barMap -> {
          BigDecimal val = barMap.get("value");

          assertTrue(val.compareTo(BigDecimal.ZERO) >= 0);

          assertTrue(val.compareTo(IndicatorMath.HUNDRED) <= 0);
        });
  }

  @Test
  void rsiFlatPricesOutputHundred() {

    Map<LocalDate, Map<String, BigDecimal>> result =
        new RsiIndicator()
            .compute(
                closes(
                    10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10),
                IndicatorParams.parse("period=14"),
                PriceSource.CLOSE);

    assertFalse(result.isEmpty());

    result
        .values()
        .forEach(barMap ->
            assertEquals(0, barMap.get("value").compareTo(bd(100)), "flat prices RSI must be 100"));
  }

  @Test
  void rsiStatefulResumeMatchesFullPassBitForBit() {
    RsiIndicator indicator = new RsiIndicator();
    IndicatorParams params = IndicatorParams.parse("period=14");

    assertTrue(indicator.isStateful());
    assertEquals(15, indicator.requiredLookback(params));
    assertThrows(
        IllegalArgumentException.class,
        () -> indicator.compute(walk(5), IndicatorParams.parse("period=0"), PriceSource.CLOSE));
    assertFalse(indicator.hasCompleteState(null));
    assertFalse(indicator.hasCompleteState(Map.of()));
    assertFalse(indicator.hasCompleteState(Map.of(RsiIndicator.STATE_AVG_GAIN, BigDecimal.ONE)));
    assertFalse(
        indicator.hasCompleteState(
            Map.of(
                RsiIndicator.STATE_AVG_GAIN,
                BigDecimal.ONE,
                RsiIndicator.STATE_AVG_LOSS,
                BigDecimal.ONE)));

    List<PriceBar> bars = walk(100);
    Map<LocalDate, IndicatorComputationPoint> fullPass =
        indicator.computeStateful(bars, params, PriceSource.CLOSE, null);

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
