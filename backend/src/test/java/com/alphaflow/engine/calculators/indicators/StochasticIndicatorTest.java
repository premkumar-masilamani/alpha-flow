package com.alphaflow.engine.calculators.indicators;

import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.bd;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.closes;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.walk;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.indicators.StochasticIndicator;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StochasticIndicatorTest {

  @Test
  void stochasticStaysInRange() {

    Map<LocalDate, Map<String, BigDecimal>> r =
        new StochasticIndicator()
            .compute(
                walk(100), IndicatorParams.parse("k=14,kSmooth=3,dSmooth=3"), PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    r.values()
        .forEach(
            m ->
                m.values()
                    .forEach(
                        val -> {
                          assertTrue(val.compareTo(BigDecimal.ZERO) >= 0);

                          assertTrue(val.compareTo(IndicatorMath.HUNDRED) <= 0);
                        }));
  }

  @Test
  void stochasticZeroRangeDoesNotThrow() {

    // Flat price series (highestHigh == lowestLow) should result in %K and %D being 0, not throwing
    // ArithmeticException

    Map<LocalDate, Map<String, BigDecimal>> r =
        new StochasticIndicator()
            .compute(
                closes(
                    100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100,
                    100, 100, 100, 100),
                IndicatorParams.parse("k=14,kSmooth=3,dSmooth=3"),
                PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    r.values()
        .forEach(
            m ->
                m.values()
                    .forEach(
                        val ->
                            assertEquals(
                                0,
                                val.compareTo(bd(0)),
                                "Stochastic on flat prices must output 0")));
  }
}
