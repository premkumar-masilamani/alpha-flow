package com.alphaflow.engine.calculators.indicators;

import static org.junit.jupiter.api.Assertions.*;

import com.alphaflow.engine.indicators.VwbbIndicator;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VwbbIndicatorTest {

  @Test
  void testVwbbComputesCorrectValues() {
    LocalDate epoch = LocalDate.of(2020, 1, 1);
    List<PriceBar> bars =
        List.of(
            new PriceBar(
                epoch,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100)),
            new PriceBar(
                epoch.plusDays(1),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(200)),
            new PriceBar(
                epoch.plusDays(2),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(300)));

    Map<LocalDate, Map<String, BigDecimal>> result =
        new VwbbIndicator().compute(bars, IndicatorParams.parse("period=3"), PriceSource.CLOSE);

    // No output before window size is met
    assertNull(result.get(epoch));
    assertNull(result.get(epoch.plusDays(1)));

    // Output at window size
    Map<String, BigDecimal> pt = result.get(epoch.plusDays(2));
    assertNotNull(pt);

    // Expected values based on manual math:
    // sum(V) = 600
    // sum(P * V) = 1000 + 4000 + 9000 = 14000
    // VWMA = 14000 / 600 = 23.3333333333
    // Variance = (100 * (10 - 23.3333)^2 + 200 * (20 - 23.3333)^2 + 300 * (30 - 23.3333)^2) / 600
    //          = (100 * 177.7778 + 200 * 11.1111 + 300 * 44.4444) / 600
    //          = (17777.78 + 2222.22 + 13333.32) / 600 = 33333.32 / 600 = 55.5555555556
    // StdDev = sqrt(55.5555555556) = 7.4535599
    // 2 * StdDev = 14.9071198
    // Upper = 23.3333 + 14.9071 = 38.2404
    // Middle = 23.3333
    // Lower = 23.3333 - 14.9071 = 8.4262
    // Depending on full internal precision, rounding to 4 decimals yields:
    // Upper = 38.2405, Middle = 23.3333, Lower = 8.4262

    assertEquals(0, pt.get("middle").compareTo(BigDecimal.valueOf(23.3333)));
    assertEquals(0, pt.get("upper").compareTo(BigDecimal.valueOf(38.2405)));
    assertEquals(0, pt.get("lower").compareTo(BigDecimal.valueOf(8.4262)));
  }

  @Test
  void testVwbbMixedVolume() {
    LocalDate epoch = LocalDate.of(2020, 1, 1);
    List<PriceBar> bars =
        List.of(
            new PriceBar(
                epoch,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100)),
            new PriceBar(
                epoch.plusDays(1),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(20),
                BigDecimal.ZERO),
            new PriceBar(
                epoch.plusDays(2),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(300)));

    Map<LocalDate, Map<String, BigDecimal>> result =
        new VwbbIndicator().compute(bars, IndicatorParams.parse("period=3"), PriceSource.CLOSE);

    Map<String, BigDecimal> pt = result.get(epoch.plusDays(2));
    assertNotNull(pt);

    // Mixed unweighted fallback logic check:
    // sum(V) = 400
    // sum(P * V) = 1000 + 0 + 9000 = 10000
    // VWMA = 25.0
    // Variance = (100 * (10-25)^2 + 300 * (30-25)^2) / 400 = (22500 + 7500) / 400 = 75.0
    // StdDev = sqrt(75) = 8.6603
    // 2 * StdDev = 17.3205
    // Upper = 42.3205, Middle = 25.0000, Lower = 7.6795

    assertEquals(0, pt.get("middle").compareTo(BigDecimal.valueOf(25.0000)));
    assertEquals(0, pt.get("upper").compareTo(BigDecimal.valueOf(42.3205)));
    assertEquals(0, pt.get("lower").compareTo(BigDecimal.valueOf(7.6795)));
  }

  @Test
  void testVwbbCustomMultiplier() {
    LocalDate epoch = LocalDate.of(2020, 1, 1);
    List<PriceBar> bars =
        List.of(
            new PriceBar(
                epoch,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100)),
            new PriceBar(
                epoch.plusDays(1),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(200)),
            new PriceBar(
                epoch.plusDays(2),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(300)));

    Map<LocalDate, Map<String, BigDecimal>> result =
        new VwbbIndicator()
            .compute(bars, IndicatorParams.parse("period=3,stdDev=3"), PriceSource.CLOSE);

    Map<String, BigDecimal> pt = result.get(epoch.plusDays(2));
    assertNotNull(pt);

    // Using multiplier = 3
    // VWMA = 23.3333
    // StdDev = 7.4536
    // 3 * StdDev = 22.3607
    // Upper = 23.3333 + 22.3607 = 45.6940
    // Lower = 23.3333 - 22.3607 = 0.9726
    // Exact internal rounding calculation yields:
    // Upper = 45.6940, Middle = 23.3333, Lower = 0.9727

    assertEquals(0, pt.get("middle").compareTo(BigDecimal.valueOf(23.3333)));
    assertEquals(0, pt.get("upper").compareTo(BigDecimal.valueOf(45.6940)));
    assertEquals(0, pt.get("lower").compareTo(BigDecimal.valueOf(0.9727)));
  }

  @Test
  void testVwbbZeroVolumeFallback() {
    LocalDate epoch = LocalDate.of(2020, 1, 1);
    List<PriceBar> bars =
        List.of(
            new PriceBar(
                epoch,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.ZERO),
            new PriceBar(
                epoch.plusDays(1),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(20),
                BigDecimal.ZERO),
            new PriceBar(
                epoch.plusDays(2),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(30),
                BigDecimal.ZERO));

    Map<LocalDate, Map<String, BigDecimal>> result =
        new VwbbIndicator().compute(bars, IndicatorParams.parse("period=3"), PriceSource.CLOSE);

    Map<String, BigDecimal> pt = result.get(epoch.plusDays(2));
    assertNotNull(pt);

    // Unweighted fallback:
    // SMA = 20.0
    // Variance = ( (10-20)^2 + (20-20)^2 + (30-20)^2 ) / 3 = (100 + 100) / 3 = 66.6667
    // StdDev = sqrt(66.6667) = 8.1650
    // 2 * StdDev = 16.3299
    // Upper = 36.3299
    // Middle = 20.0000
    // Lower = 3.6701

    assertEquals(0, pt.get("middle").compareTo(BigDecimal.valueOf(20.0000)));
    assertEquals(0, pt.get("upper").compareTo(BigDecimal.valueOf(36.3299)));
    assertEquals(0, pt.get("lower").compareTo(BigDecimal.valueOf(3.6701)));
  }

  @Test
  void testInvalidPeriodThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new VwbbIndicator()
                .compute(List.of(), IndicatorParams.parse("period=0"), PriceSource.CLOSE));
  }
}
