package com.alphaflow.engine.calculators.indicators;

import static org.junit.jupiter.api.Assertions.*;

import com.alphaflow.engine.indicators.EMAIndicator;
import com.alphaflow.engine.indicators.MACDIndicator;
import com.alphaflow.engine.indicators.RSIIndicator;
import com.alphaflow.engine.indicators.SMAIndicator;
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

    Map<LocalDate, Map<String, BigDecimal>> r =
        new SMAIndicator().compute(aaplBars, IndicatorParams.parse("period=20"), PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    LocalDate latestDate = LocalDate.of(2026, 5, 29);
    BigDecimal latestVal = IndicatorTestHelper.plot(r, latestDate, "value");

    System.out.println("[AAPL-SMA-20] Latest point: Date=" + latestDate + ", Value=" + latestVal);

    assertEquals(0, latestVal.compareTo(new BigDecimal("297.5415")));
  }

  @Test
  void testAaplEMA() {

    Map<LocalDate, Map<String, BigDecimal>> r =
        new EMAIndicator().compute(aaplBars, IndicatorParams.parse("period=20"), PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    LocalDate latestDate = LocalDate.of(2026, 5, 29);
    BigDecimal latestVal = IndicatorTestHelper.plot(r, latestDate, "value");

    System.out.println("[AAPL-EMA-20] Latest point: Date=" + latestDate + ", Value=" + latestVal);

    assertEquals(0, latestVal.compareTo(new BigDecimal("297.8875")));
  }

  @Test
  void testAaplRSI() {

    Map<LocalDate, Map<String, BigDecimal>> r =
        new RSIIndicator().compute(aaplBars, IndicatorParams.parse("period=14"), PriceSource.CLOSE);

    assertFalse(r.isEmpty());

    LocalDate latestDate = LocalDate.of(2026, 5, 29);
    BigDecimal latestVal = IndicatorTestHelper.plot(r, latestDate, "value");

    System.out.println("[AAPL-RSI-14] Latest point: Date=" + latestDate + ", Value=" + latestVal);

    assertEquals(0, latestVal.compareTo(new BigDecimal("78.7683")));
  }

  @Test
  void testAaplMACD() {

    Map<LocalDate, Map<String, BigDecimal>> r =
        new MACDIndicator()
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

    BigDecimal kVal = IndicatorTestHelper.plot(r, latestDate, "k");

    BigDecimal dVal = IndicatorTestHelper.plot(r, latestDate, "d");

    System.out.println("[AAPL-STOCHASTIC] Latest point: %K=" + kVal + ", %D=" + dVal);

    assertEquals(0, kVal.compareTo(new BigDecimal("92.0455")));

    assertEquals(0, dVal.compareTo(new BigDecimal("91.6978")));
  }
}
