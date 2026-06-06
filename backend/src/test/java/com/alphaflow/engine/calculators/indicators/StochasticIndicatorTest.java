package com.alphaflow.engine.calculators.indicators;

import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class StochasticIndicatorTest {

  @Test
  void stochasticStaysInRange() {

    List<PlotPoint> r =
        new StochasticIndicator()
            .compute(
                walk(100), IndicatorParams.parse("k=14,kSmooth=3,dSmooth=3"), PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    r.forEach(
        p -> {
          assertTrue(p.value().compareTo(BigDecimal.ZERO) >= 0);

          assertTrue(p.value().compareTo(IndicatorMath.HUNDRED) <= 0);
        });
  }

  @Test
  void stochasticZeroRangeDoesNotThrow() {

    // Flat price series (highestHigh == lowestLow) should result in %K and %D being 0, not throwing
    // ArithmeticException

    List<PlotPoint> r =
        new StochasticIndicator()
            .compute(
                closes(
                    100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100,
                    100, 100, 100, 100),
                IndicatorParams.parse("k=14,kSmooth=3,dSmooth=3"),
                PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    r.forEach(
        p ->
            assertEquals(0, p.value().compareTo(bd(0)), "Stochastic on flat prices must output 0"));
  }
}
