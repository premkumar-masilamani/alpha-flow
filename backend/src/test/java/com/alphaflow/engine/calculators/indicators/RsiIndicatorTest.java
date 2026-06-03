package com.alphaflow.engine.calculators.indicators;

import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RsiIndicatorTest {

  @Test
  void rsiAllGainsIsHundred() {

    IndicatorResult r =
        new RsiIndicator()
            .compute(
                closes(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16),
                null,
                IndicatorParams.parse("period=14"),
                PriceSource.CLOSE);

    assertFalse(r.values().isEmpty());

    r.values()
        .forEach(p -> assertEquals(0, p.value().compareTo(bd(100)), "all-gains RSI must be 100"));
  }

  @Test
  void rsiAllLossesIsZero() {

    IndicatorResult r =
        new RsiIndicator()
            .compute(
                closes(16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1),
                null,
                IndicatorParams.parse("period=14"),
                PriceSource.CLOSE);

    assertFalse(r.values().isEmpty());

    r.values()
        .forEach(p -> assertEquals(0, p.value().compareTo(bd(0)), "all-losses RSI must be 0"));
  }

  @Test
  void rsiStaysInRange() {

    IndicatorResult r =
        new RsiIndicator()
            .compute(walk(100), null, IndicatorParams.parse("period=14"), PriceSource.CLOSE);

    r.values()
        .forEach(
            p -> {
              assertTrue(p.value().compareTo(BigDecimal.ZERO) >= 0);

              assertTrue(p.value().compareTo(IndicatorMath.HUNDRED) <= 0);
            });
  }

  @Test
  void rsiResumeMatchesBackfill() {

    assertRecursiveResumeMatchesBackfill(
        new RsiIndicator(), IndicatorParams.parse("period=14"), 30, 50, 70);
  }

  @Test
  void rsiFlatPricesOutputHundred() {

    IndicatorResult r =
        new RsiIndicator()
            .compute(
                closes(
                    10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10),
                null,
                IndicatorParams.parse("period=14"),
                PriceSource.CLOSE);

    assertFalse(r.values().isEmpty());

    r.values()
        .forEach(p -> assertEquals(0, p.value().compareTo(bd(100)), "flat prices RSI must be 100"));
  }

  @Test
  void rsiWarmupPhaseProducesNoState() {

    // Only 5 bars for period = 14: seeded remains false

    IndicatorResult r =
        new RsiIndicator()
            .compute(
                closes(10, 11, 12, 13, 14),
                null,
                IndicatorParams.parse("period=14"),
                PriceSource.CLOSE);

    assertTrue(r.values().isEmpty());

    assertNull(r.newStateJson());
  }

  @Test
  void rsiResumeWithPartialSeedingState() {

    // Prior state has avgGain but missing avgLoss

    String priorState = "{\"avgGain\":\"1.5\",\"prevClose\":\"10.0\"}";

    IndicatorResult r =
        new RsiIndicator()
            .compute(
                closes(11, 12, 13),
                priorState,
                IndicatorParams.parse("period=3"),
                PriceSource.CLOSE);

    // It should NOT treat it as seeded, but instead fall back to seeding

    // Since we only pass 3 bars, and prevClose is 10.0, we get 3 deltas:

    // 11-10=1 (gain), 12-11=1 (gain), 13-12=1 (gain)

    // Since period=3, it should seed on the 3rd bar!

    assertEquals(1, r.values().size());

    assertNotNull(r.newStateJson());
  }
}
