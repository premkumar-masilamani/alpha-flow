package com.alphaflow.engine.calculators.indicators;

import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

import com.alphaflow.persistence.enums.PriceSource;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmaIndicatorTest {

  @Test
  void emaReferenceValues() {

    // EMA-3, k = 2/4 = 0.5, seed = SMA(1,2,3)=2; then 4*.5+2*.5=3; 5*.5+3*.5=4.

    List<PlotPoint> r =
        new EmaIndicator()
            .compute(closes(1, 2, 3, 4, 5), IndicatorParams.parse("period=3"), PriceSource.CLOSE);

    assertEquals(3, r.size());

    assertEquals(0, plot(r, EPOCH.plusDays(2), "value").value().compareTo(bd(2)));

    assertEquals(0, plot(r, EPOCH.plusDays(3), "value").value().compareTo(bd(3)));

    assertEquals(0, plot(r, EPOCH.plusDays(4), "value").value().compareTo(bd(4)));
  }

  @Test
  void emaRejectsInvalidPeriod() {

    assertThrows(IllegalArgumentException.class, () -> EmaAccumulator.fresh(0));

    assertThrows(IllegalArgumentException.class, () -> EmaAccumulator.fresh(-5));
  }
}
