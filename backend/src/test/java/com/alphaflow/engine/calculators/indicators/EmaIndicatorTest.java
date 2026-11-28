package com.alphaflow.engine.calculators.indicators;

import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.EPOCH;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.bd;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.closes;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.plot;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.walk;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.indicators.EmaIndicator;
import com.alphaflow.engine.indicators.dtos.IndicatorComputationPoint;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.EmaAccumulator;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmaIndicatorTest {

  @Test
  void emaReferenceValues() {

    // EMA-3, k = 2/4 = 0.5, seed = SMA(1,2,3)=2; then 4*.5+2*.5=3; 5*.5+3*.5=4.

    Map<LocalDate, Map<String, BigDecimal>> result =
        new EmaIndicator()
            .compute(closes(1, 2, 3, 4, 5), IndicatorParams.parse("period=3"), PriceSource.CLOSE);

    assertEquals(3, result.size());

    assertEquals(0, plot(result, EPOCH.plusDays(2), "value").compareTo(bd(2)));

    assertEquals(0, plot(result, EPOCH.plusDays(3), "value").compareTo(bd(3)));

    assertEquals(0, plot(result, EPOCH.plusDays(4), "value").compareTo(bd(4)));
  }

  @Test
  void emaRejectsInvalidPeriod() {

    assertThrows(IllegalArgumentException.class, () -> EmaAccumulator.fresh(0));

    assertThrows(IllegalArgumentException.class, () -> EmaAccumulator.fresh(-5));

    assertThrows(IllegalArgumentException.class, () -> EmaAccumulator.seeded(5, null));
  }

  @Test
  void emaStatefulResumeMatchesFullPassBitForBit() {
    EmaIndicator indicator = new EmaIndicator();
    IndicatorParams params = IndicatorParams.parse("period=13");

    assertTrue(indicator.isStateful());
    assertEquals(13, indicator.requiredLookback(params));
    assertFalse(indicator.hasCompleteState(null));
    assertFalse(indicator.hasCompleteState(Map.of()));

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
