package com.alphaflow.engine.calculators.indicators;


import com.alphaflow.persistence.enums.PriceSource;

import org.junit.jupiter.api.Test;


import java.math.BigDecimal;

import java.time.LocalDate;


import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.*;

import static org.junit.jupiter.api.Assertions.*;


class MacdIndicatorTest {


  @Test

  void macdConstantSeriesIsZero() {

    double[] flat = new double[60];

    java.util.Arrays.fill(flat, 50.0);

    IndicatorResult r = new MacdIndicator().compute(

        closes(flat), null, IndicatorParams.parse("fast=12,slow=26,signal=9"), PriceSource.CLOSE);

    assertFalse(r.values().isEmpty());

    r.values().forEach(p -> assertEquals(0, p.value().compareTo(bd(0)),

        "constant series must give zero macd/signal/histogram (" + p.outputName() + ")"));

  }


  @Test

  void macdEmitsThreePlotsOnceDefined() {

    IndicatorResult r = new MacdIndicator().compute(

        walk(80), null, IndicatorParams.parse("fast=12,slow=26,signal=9"), PriceSource.CLOSE);

    // Last bar (well past warm-up) must carry all three plots.

    LocalDate last = EPOCH.plusDays(79);

    assertNotNull(plot(r.values(), last, "macd"));

    assertNotNull(plot(r.values(), last, "signal"));

    assertNotNull(plot(r.values(), last, "histogram"));

    // histogram == macd - signal at that bar.

    BigDecimal macd = plot(r.values(), last, "macd").value();

    BigDecimal signal = plot(r.values(), last, "signal").value();

    BigDecimal hist = plot(r.values(), last, "histogram").value();

    assertEquals(0, hist.compareTo(macd.subtract(signal)));

  }


  @Test

  void macdResumeMatchesBackfill() {

    assertRecursiveResumeMatchesBackfill(

        new MacdIndicator(), IndicatorParams.parse("fast=12,slow=26,signal=9"), 45, 60, 75);

  }


  @Test

  void macdDefaultSignalPeriod() {

    IndicatorResult r = new MacdIndicator().compute(

        walk(50), null, IndicatorParams.parse("fast=12,slow=26"), PriceSource.CLOSE);

    assertFalse(r.values().isEmpty());

  }


  @Test

  void macdWarmupPhaseProducesNoState() {

    // fast=12, slow=26, signal=9 -> needs at least 26 + 9 - 1 = 34 bars to seed signal EMA

    // 30 bars is not enough to seed the signal EMA, so newStateJson should be null.

    IndicatorResult r = new MacdIndicator().compute(

        walk(30), null, IndicatorParams.parse("fast=12,slow=26,signal=9"), PriceSource.CLOSE);

    assertNull(r.newStateJson());

  }

}

