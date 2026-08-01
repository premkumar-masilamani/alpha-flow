package com.alphaflow.engine.calculators.indicators;

import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.EPOCH;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.bd;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.closes;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.plot;
import static com.alphaflow.engine.calculators.indicators.IndicatorTestHelper.walk;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.alphaflow.engine.indicators.MacdIndicator;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MacdIndicatorTest {

  @Test
  void macdConstantSeriesIsZero() {

    double[] flat = new double[60];

    java.util.Arrays.fill(flat, 50.0);

    Map<LocalDate, Map<String, BigDecimal>> r =
        new MacdIndicator()
            .compute(
                closes(flat), IndicatorParams.parse("fast=12,slow=26,signal=9"), PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    r.values()
        .forEach(
            m ->
                m.forEach(
                    (k, val) ->
                        assertEquals(
                            0,
                            val.compareTo(bd(0)),
                            "constant series must give zero macd/signal/histogram (" + k + ")")));
  }

  @Test
  void macdEmitsThreePlotsOnceDefined() {

    Map<LocalDate, Map<String, BigDecimal>> r =
        new MacdIndicator()
            .compute(
                walk(80), IndicatorParams.parse("fast=12,slow=26,signal=9"), PriceSource.CLOSE);

    // Last bar (well past warm-up) must carry all three plots.

    LocalDate last = EPOCH.plusDays(79);

    assertNotNull(plot(r, last, "macd"));

    assertNotNull(plot(r, last, "signal"));

    assertNotNull(plot(r, last, "histogram"));

    // histogram == macd - signal at that bar.

    BigDecimal macd = plot(r, last, "macd");

    BigDecimal signal = plot(r, last, "signal");

    BigDecimal hist = plot(r, last, "histogram");

    assertEquals(0, hist.compareTo(macd.subtract(signal)));
  }

  @Test
  void macdDefaultSignalPeriod() {

    Map<LocalDate, Map<String, BigDecimal>> r =
        new MacdIndicator()
            .compute(walk(50), IndicatorParams.parse("fast=12,slow=26"), PriceSource.CLOSE);

    assertFalse(r.isEmpty());
  }
}
