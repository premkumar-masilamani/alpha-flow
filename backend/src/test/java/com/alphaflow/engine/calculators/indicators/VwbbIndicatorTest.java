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
                BigDecimal.valueOf(10), // Open
                BigDecimal.valueOf(12), // High
                BigDecimal.valueOf(8), // Low
                BigDecimal.valueOf(10), // Close
                BigDecimal.valueOf(100)), // Vol
            new PriceBar(
                epoch.plusDays(1),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(22),
                BigDecimal.valueOf(18),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(200)),
            new PriceBar(
                epoch.plusDays(2),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(32),
                BigDecimal.valueOf(28),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(300)));

    // Parameters must explicitly specify period and stdDev
    Map<LocalDate, Map<String, BigDecimal>> result =
        new VwbbIndicator()
            .compute(bars, IndicatorParams.parse("period=3,stdDev=2"), PriceSource.CLOSE);

    // No output before window size is met
    assertNull(result.get(epoch));
    assertNull(result.get(epoch.plusDays(1)));

    // Output at window size
    Map<String, BigDecimal> pt = result.get(epoch.plusDays(2));
    assertNotNull(pt);

    // Expected values based on manual math with Typical Price (TP = (12+8+10)/3 = 10):
    // vwap = 23.3333
    // stdDev = 7.4536
    // Upper = 38.2405
    // Lower = 8.4262
    // Bandwidth = 1.2778

    assertEquals(0, pt.get("middle").compareTo(BigDecimal.valueOf(23.3333)));
    assertEquals(0, pt.get("upper").compareTo(BigDecimal.valueOf(38.2405)));
    assertEquals(0, pt.get("lower").compareTo(BigDecimal.valueOf(8.4262)));
    assertEquals(0, pt.get("bandwidth").compareTo(BigDecimal.valueOf(1.2778)));
  }

  @Test
  void testVwbbMixedVolume() {
    LocalDate epoch = LocalDate.of(2020, 1, 1);
    List<PriceBar> bars =
        List.of(
            new PriceBar(
                epoch,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(12),
                BigDecimal.valueOf(8),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100)),
            new PriceBar(
                epoch.plusDays(1),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(22),
                BigDecimal.valueOf(18),
                BigDecimal.valueOf(20),
                BigDecimal.ZERO),
            new PriceBar(
                epoch.plusDays(2),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(32),
                BigDecimal.valueOf(28),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(300)));

    Map<LocalDate, Map<String, BigDecimal>> result =
        new VwbbIndicator()
            .compute(bars, IndicatorParams.parse("period=3,stdDev=2"), PriceSource.CLOSE);

    Map<String, BigDecimal> pt = result.get(epoch.plusDays(2));
    assertNotNull(pt);

    // TP values: 10, 20, 30
    // sum(V) = 400
    // sum(TP * V) = 1000 + 0 + 9000 = 10000
    // vwap = 25.0000
    // sum(TP^2 * V) = 100 * 100 + 0 + 900 * 300 = 10000 + 270000 = 280000
    // Variance = 280000 / 400 - 625 = 700 - 625 = 75.0
    // stdDev = sqrt(75) = 8.6603
    // Upper = 25 + 17.3205 = 42.3205
    // Lower = 25 - 17.3205 = 7.6795
    // Bandwidth = 34.6410 / 25 = 1.3856

    assertEquals(0, pt.get("middle").compareTo(BigDecimal.valueOf(25.0000)));
    assertEquals(0, pt.get("upper").compareTo(BigDecimal.valueOf(42.3205)));
    assertEquals(0, pt.get("lower").compareTo(BigDecimal.valueOf(7.6795)));
    assertEquals(0, pt.get("bandwidth").compareTo(BigDecimal.valueOf(1.3856)));
  }

  @Test
  void testVwbbCustomMultiplier() {
    LocalDate epoch = LocalDate.of(2020, 1, 1);
    List<PriceBar> bars =
        List.of(
            new PriceBar(
                epoch,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(12),
                BigDecimal.valueOf(8),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100)),
            new PriceBar(
                epoch.plusDays(1),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(22),
                BigDecimal.valueOf(18),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(200)),
            new PriceBar(
                epoch.plusDays(2),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(32),
                BigDecimal.valueOf(28),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(300)));

    Map<LocalDate, Map<String, BigDecimal>> result =
        new VwbbIndicator()
            .compute(bars, IndicatorParams.parse("period=3,stdDev=3"), PriceSource.CLOSE);

    Map<String, BigDecimal> pt = result.get(epoch.plusDays(2));
    assertNotNull(pt);

    // Using multiplier = 3
    // vwap = 23.3333
    // stdDev = 7.4536
    // Upper = 23.3333 + 22.3607 = 45.6940
    // Lower = 23.3333 - 22.3607 = 0.9727
    // Bandwidth = 44.7214 / 23.3333 = 1.9166

    assertEquals(0, pt.get("middle").compareTo(BigDecimal.valueOf(23.3333)));
    assertEquals(0, pt.get("upper").compareTo(BigDecimal.valueOf(45.6940)));
    assertEquals(0, pt.get("lower").compareTo(BigDecimal.valueOf(0.9727)));
    assertEquals(0, pt.get("bandwidth").compareTo(BigDecimal.valueOf(1.9166)));
  }

  @Test
  void testVwbbZeroVolumeFallback() {
    LocalDate epoch = LocalDate.of(2020, 1, 1);
    List<PriceBar> bars =
        List.of(
            new PriceBar(
                epoch,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(12),
                BigDecimal.valueOf(8),
                BigDecimal.valueOf(10),
                BigDecimal.ZERO),
            new PriceBar(
                epoch.plusDays(1),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(22),
                BigDecimal.valueOf(18),
                BigDecimal.valueOf(20),
                BigDecimal.ZERO),
            new PriceBar(
                epoch.plusDays(2),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(32),
                BigDecimal.valueOf(28),
                BigDecimal.valueOf(30),
                BigDecimal.ZERO));

    Map<LocalDate, Map<String, BigDecimal>> result =
        new VwbbIndicator()
            .compute(bars, IndicatorParams.parse("period=3,stdDev=2"), PriceSource.CLOSE);

    Map<String, BigDecimal> pt = result.get(epoch.plusDays(2));
    assertNotNull(pt);

    // Unweighted fallback:
    // vwap = 20.0
    // Variance = 66.6667
    // stdDev = 8.1650
    // Upper = 36.3299
    // Lower = 3.6701
    // Bandwidth = 32.6598 / 20 = 1.6330

    assertEquals(0, pt.get("middle").compareTo(BigDecimal.valueOf(20.0000)));
    assertEquals(0, pt.get("upper").compareTo(BigDecimal.valueOf(36.3299)));
    assertEquals(0, pt.get("lower").compareTo(BigDecimal.valueOf(3.6701)));
    assertEquals(0, pt.get("bandwidth").compareTo(BigDecimal.valueOf(1.6330)));
  }

  @Test
  void testMissingMultiplierThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new VwbbIndicator()
                .compute(List.of(), IndicatorParams.parse("period=20"), PriceSource.CLOSE));
  }

  @Test
  void testInvalidPeriodThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new VwbbIndicator()
                .compute(List.of(), IndicatorParams.parse("period=0,stdDev=2"), PriceSource.CLOSE));
  }
}
