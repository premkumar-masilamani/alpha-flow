package com.alphaflow.engine.calculators.indicators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.alphaflow.engine.indicators.EmaIndicator;
import com.alphaflow.engine.indicators.MacdIndicator;
import com.alphaflow.engine.indicators.RsiIndicator;
import com.alphaflow.engine.indicators.SmaIndicator;
import com.alphaflow.engine.indicators.StochasticIndicator;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class IndicatorAaplTest {

  private static List<PriceBar> aaplBars;

  @BeforeAll
  static void setup() {

    aaplBars = IndicatorTestHelper.loadAaplCsv();

    assertNotNull(aaplBars);

    assertFalse(aaplBars.isEmpty());
  }

  @Test
  void testAaplSma() {

    Map<LocalDate, Map<String, BigDecimal>> r =
        new SmaIndicator().compute(aaplBars, IndicatorParams.parse("period=20"), PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    LocalDate latestDate = LocalDate.of(2026, 5, 29);
    BigDecimal latestVal = IndicatorTestHelper.plot(r, latestDate, "value");

    System.out.println("[AAPL-SMA-20] Latest point: Date=" + latestDate + ", Value=" + latestVal);

    assertEquals(0, latestVal.compareTo(new BigDecimal("297.5415")));
  }

  @Test
  void testAaplEma() {

    Map<LocalDate, Map<String, BigDecimal>> r =
        new EmaIndicator().compute(aaplBars, IndicatorParams.parse("period=20"), PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    LocalDate latestDate = LocalDate.of(2026, 5, 29);
    BigDecimal latestVal = IndicatorTestHelper.plot(r, latestDate, "value");

    System.out.println("[AAPL-EMA-20] Latest point: Date=" + latestDate + ", Value=" + latestVal);

    assertEquals(0, latestVal.compareTo(new BigDecimal("297.8875")));
  }

  @Test
  void testAaplRsi() {

    Map<LocalDate, Map<String, BigDecimal>> r =
        new RsiIndicator().compute(aaplBars, IndicatorParams.parse("period=14"), PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    LocalDate latestDate = LocalDate.of(2026, 5, 29);
    BigDecimal latestVal = IndicatorTestHelper.plot(r, latestDate, "value");

    System.out.println("[AAPL-RSI-14] Latest point: Date=" + latestDate + ", Value=" + latestVal);

    assertEquals(0, latestVal.compareTo(new BigDecimal("78.7683")));
  }

  @Test
  void testAaplMacd() {

    Map<LocalDate, Map<String, BigDecimal>> r =
        new MacdIndicator()
            .compute(
                aaplBars, IndicatorParams.parse("fast=12,slow=26,signal=9"), PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    LocalDate latestDate = LocalDate.of(2026, 5, 29);

    BigDecimal macd = IndicatorTestHelper.plot(r, latestDate, "macd");

    BigDecimal signal = IndicatorTestHelper.plot(r, latestDate, "signal");

    BigDecimal hist = IndicatorTestHelper.plot(r, latestDate, "histogram");

    System.out.println(
        "[AAPL-MACD] Latest point: MACD=" + macd + ", Signal=" + signal + ", Hist=" + hist);

    assertEquals(0, macd.compareTo(new BigDecimal("10.3917")));

    assertEquals(0, signal.compareTo(new BigDecimal("9.7772")));

    assertEquals(0, hist.compareTo(new BigDecimal("0.6144")));
  }

  @Test
  void testAaplStochastic() {

    Map<LocalDate, Map<String, BigDecimal>> r =
        new StochasticIndicator()
            .compute(
                aaplBars, IndicatorParams.parse("k=14,kSmooth=3,dSmooth=3"), PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    LocalDate latestDate = LocalDate.of(2026, 5, 29);

    BigDecimal stochK = IndicatorTestHelper.plot(r, latestDate, "k");

    BigDecimal stochD = IndicatorTestHelper.plot(r, latestDate, "d");

    System.out.println("[AAPL-STOCHASTIC] Latest point: %K=" + stochK + ", %D=" + stochD);

    assertEquals(0, stochK.compareTo(new BigDecimal("92.0455")));

    assertEquals(0, stochD.compareTo(new BigDecimal("91.6978")));
  }
}
