package com.alphaflow.engine.calculators.indicators;

import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.EPOCH;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.bd;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.closes;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.plot;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alphaflow.engine.indicators.SmaIndicator;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SmaIndicatorTest {

  @Test
  void smaReferenceValues() {

    Map<LocalDate, Map<String, BigDecimal>> result =
        new SmaIndicator()
            .compute(closes(1, 2, 3, 4, 5), IndicatorParams.parse("period=3"), PriceSource.CLOSE);

    // Defined from the 3rd bar: avg(1,2,3)=2, avg(2,3,4)=3, avg(3,4,5)=4.

    assertEquals(3, result.size());

    assertEquals(0, plot(result, EPOCH.plusDays(2), "value").compareTo(bd(2)));

    assertEquals(0, plot(result, EPOCH.plusDays(3), "value").compareTo(bd(3)));

    assertEquals(0, plot(result, EPOCH.plusDays(4), "value").compareTo(bd(4)));
  }

  @Test
  void smaOnVolumeSource() {

    // volume = 1000 + dayOffset -> [1000,1001,1002]; SMA-2 -> 1000.5, 1001.5

    Map<LocalDate, Map<String, BigDecimal>> result =
        new SmaIndicator()
            .compute(closes(1, 2, 3), IndicatorParams.parse("period=2"), PriceSource.VOLUME);

    assertEquals(2, result.size());

    assertEquals(0, plot(result, EPOCH.plusDays(1), "value").compareTo(BigDecimal.valueOf(1000.5)));

    assertEquals(0, plot(result, EPOCH.plusDays(2), "value").compareTo(BigDecimal.valueOf(1001.5)));
  }
}
