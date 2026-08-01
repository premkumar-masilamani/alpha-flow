package com.alphaflow.engine.calculators.indicators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alphaflow.engine.indicators.BollingerBandsIndicator;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BollingerBandsIndicatorTest {

  @Test
  void testBollingerBandsComputesCorrectValues() {
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
        new BollingerBandsIndicator()
            .compute(bars, IndicatorParams.parse("period=3,stdDev=2"), PriceSource.CLOSE);

    // No output before window size is met
    assertNull(result.get(epoch));
    assertNull(result.get(epoch.plusDays(1)));

    // Output at window size
    Map<String, BigDecimal> pt = result.get(epoch.plusDays(2));
    assertNotNull(pt);

    // Prices: 10, 20, 30
    // SMA = 20.0000
    // Variance = 66.6667
    // stdDev = 8.1650
    // Upper = 36.3299
    // Lower = 3.6701
    // Bandwidth = 1.6330

    assertEquals(0, pt.get("middle").compareTo(BigDecimal.valueOf(20.0000)));
    assertEquals(0, pt.get("upper").compareTo(BigDecimal.valueOf(36.3299)));
    assertEquals(0, pt.get("lower").compareTo(BigDecimal.valueOf(3.6701)));
    assertEquals(0, pt.get("bandwidth").compareTo(BigDecimal.valueOf(1.6330)));
  }

  @Test
  void testConstantPriceZeroStdDev() {
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
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100)),
            new PriceBar(
                epoch.plusDays(2),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100)));

    Map<LocalDate, Map<String, BigDecimal>> result =
        new BollingerBandsIndicator()
            .compute(bars, IndicatorParams.parse("period=3,stdDev=2"), PriceSource.CLOSE);

    Map<String, BigDecimal> pt = result.get(epoch.plusDays(2));
    assertNotNull(pt);

    assertEquals(0, pt.get("middle").compareTo(BigDecimal.valueOf(10.0000)));
    assertEquals(0, pt.get("upper").compareTo(BigDecimal.valueOf(10.0000)));
    assertEquals(0, pt.get("lower").compareTo(BigDecimal.valueOf(10.0000)));
    assertEquals(0, pt.get("bandwidth").compareTo(BigDecimal.valueOf(0.0000)));
  }

  @Test
  void testCustomMultiplier() {
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
        new BollingerBandsIndicator()
            .compute(bars, IndicatorParams.parse("period=3,stdDev=3"), PriceSource.CLOSE);

    Map<String, BigDecimal> pt = result.get(epoch.plusDays(2));
    assertNotNull(pt);

    // stdDev multiplier = 3
    assertEquals(0, pt.get("middle").compareTo(BigDecimal.valueOf(20.0000)));
    assertEquals(0, pt.get("upper").compareTo(BigDecimal.valueOf(44.4949)));
    assertEquals(0, pt.get("lower").compareTo(BigDecimal.valueOf(-4.4949)));
    assertEquals(0, pt.get("bandwidth").compareTo(BigDecimal.valueOf(2.4495)));
  }

  @Test
  void testMissingMultiplierThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new BollingerBandsIndicator()
                .compute(List.of(), IndicatorParams.parse("period=20"), PriceSource.CLOSE));
  }

  @Test
  void testInvalidPeriodThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new BollingerBandsIndicator()
                .compute(List.of(), IndicatorParams.parse("period=0,stdDev=2"), PriceSource.CLOSE));
  }
}
