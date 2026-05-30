package com.alphaflow.engine.calculators.indicators;

import com.alphaflow.persistence.enums.PriceSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

class StochasticIndicatorTest {

    @Test
    void stochasticStaysInRange() {
        IndicatorResult r = new StochasticIndicator().compute(
                walk(100), null, IndicatorParams.parse("k=14,kSmooth=3,dSmooth=3"), PriceSource.CLOSE);
        assertFalse(r.values().isEmpty());
        r.values().forEach(p -> {
            assertTrue(p.value().compareTo(BigDecimal.ZERO) >= 0);
            assertTrue(p.value().compareTo(IndicatorMath.HUNDRED) <= 0);
        });
    }

    @Test
    void stochasticResumeMatchesBackfill() {
        assertWindowedResumeMatchesBackfill(
                new StochasticIndicator(), IndicatorParams.parse("k=14,kSmooth=3,dSmooth=3"), 25, 50, 75);
    }

    @Test
    void stochasticZeroRangeDoesNotThrow() {
        // Flat price series (highestHigh == lowestLow) should result in %K and %D being 0, not throwing ArithmeticException
        IndicatorResult r = new StochasticIndicator().compute(
                closes(100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100),
                null, IndicatorParams.parse("k=14,kSmooth=3,dSmooth=3"), PriceSource.CLOSE);
        assertFalse(r.values().isEmpty());
        r.values().forEach(p -> assertEquals(0, p.value().compareTo(bd(0)), "Stochastic on flat prices must output 0"));
    }

    @Test
    void testRequiresState() {
        assertFalse(new StochasticIndicator().requiresState());
    }
}
