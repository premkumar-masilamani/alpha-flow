package com.alphaflow.engine.calculators.indicators;

import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MacdIndicatorTest {

  @Test
  void macdConstantSeriesIsZero() {

    double[] flat = new double[60];

    java.util.Arrays.fill(flat, 50.0);

    List<PlotPoint> r =
        new MacdIndicator()
            .compute(
                closes(flat), IndicatorParams.parse("fast=12,slow=26,signal=9"), PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    r.forEach(
        p ->
            assertEquals(
                0,
                p.value().compareTo(bd(0)),
                "constant series must give zero macd/signal/histogram (" + p.outputName() + ")"));
  }

  @Test
  void macdEmitsThreePlotsOnceDefined() {

    List<PlotPoint> r =
        new MacdIndicator()
            .compute(
                walk(80), IndicatorParams.parse("fast=12,slow=26,signal=9"), PriceSource.CLOSE);

    // Last bar (well past warm-up) must carry all three plots.

    LocalDate last = EPOCH.plusDays(79);

    assertNotNull(plot(r, last, "macd"));

    assertNotNull(plot(r, last, "signal"));

    assertNotNull(plot(r, last, "histogram"));

    // histogram == macd - signal at that bar.

    BigDecimal macd = plot(r, last, "macd").value();

    BigDecimal signal = plot(r, last, "signal").value();

    BigDecimal hist = plot(r, last, "histogram").value();

    assertEquals(0, hist.compareTo(macd.subtract(signal)));
  }

  @Test
  void macdDefaultSignalPeriod() {

    List<PlotPoint> r =
        new MacdIndicator()
            .compute(walk(50), IndicatorParams.parse("fast=12,slow=26"), PriceSource.CLOSE);

    assertFalse(r.isEmpty());
  }
}
