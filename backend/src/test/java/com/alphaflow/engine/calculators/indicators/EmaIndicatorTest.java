package com.alphaflow.engine.calculators.indicators;

import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.EPOCH;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.bd;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.closes;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.plot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alphaflow.engine.indicators.EmaIndicator;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.utils.EmaAccumulator;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmaIndicatorTest {

  @Test
  void emaReferenceValues() {

    // EMA-3, k = 2/4 = 0.5, seed = SMA(1,2,3)=2; then 4*.5+2*.5=3; 5*.5+3*.5=4.

    Map<LocalDate, Map<String, BigDecimal>> r =
        new EmaIndicator()
            .compute(closes(1, 2, 3, 4, 5), IndicatorParams.parse("period=3"), PriceSource.CLOSE);

    assertEquals(3, r.size());

    assertEquals(0, plot(r, EPOCH.plusDays(2), "value").compareTo(bd(2)));

    assertEquals(0, plot(r, EPOCH.plusDays(3), "value").compareTo(bd(3)));

    assertEquals(0, plot(r, EPOCH.plusDays(4), "value").compareTo(bd(4)));
  }

  @Test
  void emaRejectsInvalidPeriod() {

    assertThrows(IllegalArgumentException.class, () -> EmaAccumulator.fresh(0));

    assertThrows(IllegalArgumentException.class, () -> EmaAccumulator.fresh(-5));
  }
}
