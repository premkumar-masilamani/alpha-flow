package com.alphaflow.engine.calculators.indicators;

import static org.junit.jupiter.api.Assertions.*;

import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class IndicatorAAPLTest {

  private static List<PriceBar> aaplBars;

  @BeforeAll
  static void setup() {
    aaplBars = IndicatorTestHelper.loadAaplCsv();
    assertNotNull(aaplBars);
    assertFalse(aaplBars.isEmpty());
  }

  @Test
  void testAaplSMA() {
    IndicatorResult r =
        new SmaIndicator()
            .compute(aaplBars, null, IndicatorParams.parse("period=20"), PriceSource.CLOSE);
    assertFalse(r.values().isEmpty());
    PlotPoint latest = r.values().get(r.values().size() - 1);
    System.out.println(
        "[AAPL-SMA-20] Latest point: Date=" + latest.date() + ", Value=" + latest.value());
    assertEquals(LocalDate.of(2026, 5, 29), latest.date());
    assertEquals(0, latest.value().compareTo(new BigDecimal("297.5415")));
  }

  @Test
  void testAaplEMA() {
    IndicatorResult r =
        new EmaIndicator()
            .compute(aaplBars, null, IndicatorParams.parse("period=20"), PriceSource.CLOSE);
    assertFalse(r.values().isEmpty());
    PlotPoint latest = r.values().get(r.values().size() - 1);
    System.out.println(
        "[AAPL-EMA-20] Latest point: Date=" + latest.date() + ", Value=" + latest.value());
    assertEquals(LocalDate.of(2026, 5, 29), latest.date());
    assertEquals(0, latest.value().compareTo(new BigDecimal("297.8875")));
  }

  @Test
  void testAaplRSI() {
    IndicatorResult r =
        new RsiIndicator()
            .compute(aaplBars, null, IndicatorParams.parse("period=14"), PriceSource.CLOSE);
    assertFalse(r.values().isEmpty());
    PlotPoint latest = r.values().get(r.values().size() - 1);
    System.out.println(
        "[AAPL-RSI-14] Latest point: Date=" + latest.date() + ", Value=" + latest.value());
    assertEquals(LocalDate.of(2026, 5, 29), latest.date());
    assertEquals(0, latest.value().compareTo(new BigDecimal("78.7683")));
  }

  @Test
  void testAaplMACD() {
    IndicatorResult r =
        new MacdIndicator()
            .compute(
                aaplBars,
                null,
                IndicatorParams.parse("fast=12,slow=26,signal=9"),
                PriceSource.CLOSE);
    assertFalse(r.values().isEmpty());

    PlotPoint macd = IndicatorTestHelper.plot(r.values(), LocalDate.of(2026, 5, 29), "macd");
    PlotPoint signal = IndicatorTestHelper.plot(r.values(), LocalDate.of(2026, 5, 29), "signal");
    PlotPoint hist = IndicatorTestHelper.plot(r.values(), LocalDate.of(2026, 5, 29), "histogram");

    System.out.println(
        "[AAPL-MACD] Latest point: MACD="
            + macd.value()
            + ", Signal="
            + signal.value()
            + ", Hist="
            + hist.value());
    assertEquals(0, macd.value().compareTo(new BigDecimal("10.3917")));
    assertEquals(0, signal.value().compareTo(new BigDecimal("9.7772")));
    assertEquals(0, hist.value().compareTo(new BigDecimal("0.6144")));
  }

  @Test
  void testAaplStochastic() {
    IndicatorResult r =
        new StochasticIndicator()
            .compute(
                aaplBars,
                null,
                IndicatorParams.parse("k=14,kSmooth=3,dSmooth=3"),
                PriceSource.CLOSE);
    assertFalse(r.values().isEmpty());

    PlotPoint kVal = IndicatorTestHelper.plot(r.values(), LocalDate.of(2026, 5, 29), "k");
    PlotPoint dVal = IndicatorTestHelper.plot(r.values(), LocalDate.of(2026, 5, 29), "d");

    System.out.println(
        "[AAPL-STOCHASTIC] Latest point: %K=" + kVal.value() + ", %D=" + dVal.value());
    assertEquals(0, kVal.value().compareTo(new BigDecimal("92.0455")));
    assertEquals(0, dVal.value().compareTo(new BigDecimal("91.6978")));
  }
}
