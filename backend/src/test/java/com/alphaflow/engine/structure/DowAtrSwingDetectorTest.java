package com.alphaflow.engine.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import com.alphaflow.engine.structure.dtos.SwingPoint;
import com.alphaflow.engine.structure.enums.SwingType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DowAtrSwingDetectorTest {

  private static final LocalDate EPOCH = LocalDate.of(2026, 1, 1);
  private final DowAtrSwingDetector detector = new DowAtrSwingDetector();

  private PriceBar bar(int dayOffset, String open, String high, String low, String close) {
    return new PriceBar(
        EPOCH.plusDays(dayOffset),
        new BigDecimal(open),
        new BigDecimal(high),
        new BigDecimal(low),
        new BigDecimal(close),
        BigDecimal.TEN);
  }

  @Test
  void testInvalidAndInsufficientInputsReturnEmpty() {
    List<PriceBar> shortBars =
        List.of(
            bar(0, "100", "105", "95", "100"),
            bar(1, "100", "105", "95", "100"),
            bar(2, "100", "105", "95", "100"));
    List<BigDecimal> shortAtr = IndicatorMath.calculateWilderAtrSeries(shortBars, 3);

    assertTrue(detector.detectSwings(null, shortAtr, 3, new BigDecimal("2.0")).isEmpty());
    assertTrue(detector.detectSwings(shortBars, null, 3, new BigDecimal("2.0")).isEmpty());
    assertTrue(detector.detectSwings(shortBars, shortAtr, 0, new BigDecimal("2.0")).isEmpty());
    assertTrue(detector.detectSwings(shortBars, shortAtr, 3, null).isEmpty());
    assertTrue(detector.detectSwings(shortBars, shortAtr, 3, BigDecimal.ZERO).isEmpty());
    assertTrue(detector.detectSwings(shortBars, shortAtr, 3, new BigDecimal("-1.0")).isEmpty());
    assertTrue(detector.detectSwings(shortBars, shortAtr, 3, new BigDecimal("2.0")).isEmpty());

    List<PriceBar> fourBars =
        List.of(
            bar(0, "100", "105", "95", "100"),
            bar(1, "100", "105", "95", "100"),
            bar(2, "100", "105", "95", "100"),
            bar(3, "100", "105", "95", "100"));
    assertTrue(detector.detectSwings(fourBars, shortAtr, 3, new BigDecimal("2.0")).isEmpty());
  }

  @Test
  void testFlatZeroAtrBarsEmitNoSwings() {
    List<PriceBar> flatBars = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      flatBars.add(bar(i, "100", "100", "100", "100"));
    }

    List<BigDecimal> atrSeries = IndicatorMath.calculateWilderAtrSeries(flatBars, 3);
    List<SwingPoint> swings = detector.detectSwings(flatBars, atrSeries, 3, new BigDecimal("2.0"));
    assertTrue(swings.isEmpty());
  }

  @Test
  void testDetectsAlternatingHighLowHighSwingsWithCloseConfirmation() {
    // Seed period = 3 with steady 10-point bars -> ATR(3) stays ~10, 2.0 * ATR ~ 20
    List<PriceBar> bars = new ArrayList<>();
    bars.add(bar(0, "100", "105", "95", "100"));
    bars.add(bar(1, "100", "105", "95", "100"));
    bars.add(bar(2, "100", "105", "95", "100")); // seedIndex=2, ATR=10

    // Undetermined wave: bar 3 extends high to 112 without reversing
    bars.add(bar(3, "102", "112", "102", "110"));
    // Bar 4 stays inside (neither new high nor new low, no reversal)
    bars.add(bar(4, "108", "110", "100", "104"));
    // Gradual 3-bar decline (keeps ATR ~ 10): bar 5 -> 96, bar 6 -> 88 (112 - 88 = 24 >= 2*ATR)
    bars.add(bar(5, "104", "104", "94", "96"));
    bars.add(bar(6, "96", "96", "86", "88")); // confirms HIGH at bar 3 (price 112)
    // Now in LOW wave: bar 7 extends low to 78
    bars.add(bar(7, "88", "88", "78", "80"));
    // Bar 8 mild bounce (no new low, close 86 - 78 = 8 < 2*ATR -> no reversal)
    bars.add(bar(8, "80", "88", "80", "86"));
    // Gradual 2-bar rally: bar 9 -> 95, bar 10 -> 104 (104 - 78 = 26 >= 2*ATR)
    bars.add(bar(9, "86", "96", "86", "95"));
    bars.add(bar(10, "95", "105", "95", "104")); // confirms LOW at bar 7 (price 78)
    // Now in HIGH wave: bar 11 extends high to 114, bar 12 extends high to 122
    bars.add(bar(11, "104", "114", "104", "112"));
    bars.add(bar(12, "112", "122", "112", "120"));
    // Bar 13 mild pullback (no new high, no reversal)
    bars.add(bar(13, "120", "121", "113", "115"));
    // Gradual drop: bar 14 -> 106, bar 15 -> 97 (122 - 97 = 25 >= 2*ATR) -> confirms HIGH at bar 12
    bars.add(bar(14, "115", "115", "105", "106"));
    bars.add(bar(15, "106", "106", "96", "97"));

    List<BigDecimal> atrSeries = IndicatorMath.calculateWilderAtrSeries(bars, 3);
    List<SwingPoint> swings = detector.detectSwings(bars, atrSeries, 3, new BigDecimal("2.0"));

    assertEquals(3, swings.size());
    assertEquals(SwingType.HIGH, swings.get(0).type());
    assertEquals(3, swings.get(0).barIndex());
    assertEquals(EPOCH.plusDays(3), swings.get(0).date());
    assertEquals(0, new BigDecimal("112").compareTo(swings.get(0).price()));
    assertNotNull(swings.get(0).atrAtSwing());

    assertEquals(SwingType.LOW, swings.get(1).type());
    assertEquals(7, swings.get(1).barIndex());
    assertEquals(0, new BigDecimal("78").compareTo(swings.get(1).price()));

    assertEquals(SwingType.HIGH, swings.get(2).type());
    assertEquals(12, swings.get(2).barIndex());
    assertEquals(0, new BigDecimal("122").compareTo(swings.get(2).price()));
  }

  @Test
  void testInitialWaveConfirmedAsLowFirst() {
    List<PriceBar> bars = new ArrayList<>();
    bars.add(bar(0, "100", "105", "95", "100"));
    bars.add(bar(1, "100", "105", "95", "100"));
    bars.add(bar(2, "100", "105", "95", "100")); // seed ATR = 10

    // Bar 3 extends low to 88 without reversing up
    bars.add(bar(3, "98", "98", "88", "90"));
    // Gradual 3-bar rally (steady 10-pt bars, ATR stays ~10):
    bars.add(bar(4, "90", "100", "90", "99"));
    bars.add(bar(5, "99", "109", "99", "108"));
    bars.add(bar(6, "108", "118", "108", "116")); // 116 - 88 = 28 >= 2*ATR -> confirms LOW at bar 3

    List<BigDecimal> atrSeries = IndicatorMath.calculateWilderAtrSeries(bars, 3);
    List<SwingPoint> swings = detector.detectSwings(bars, atrSeries, 3, new BigDecimal("2.0"));

    assertEquals(1, swings.size());
    assertEquals(SwingType.LOW, swings.getFirst().type());
    assertEquals(3, swings.getFirst().barIndex());
    assertEquals(0, new BigDecimal("88").compareTo(swings.getFirst().price()));
  }

  @Test
  void testWickExceedingThresholdWithoutCloseDoesNotTriggerReversal() {
    List<PriceBar> bars = new ArrayList<>();
    bars.add(bar(0, "100", "105", "95", "100"));
    bars.add(bar(1, "100", "105", "95", "100"));
    bars.add(bar(2, "100", "105", "95", "100")); // seed ATR = 10

    // Huge intraday dip to 60, but closes at 101 (105 - 101 = 4 < 2*ATR) -> no confirmed swing
    bars.add(bar(3, "100", "105", "60", "101"));

    List<BigDecimal> atrSeries = IndicatorMath.calculateWilderAtrSeries(bars, 3);
    List<SwingPoint> swings = detector.detectSwings(bars, atrSeries, 3, new BigDecimal("2.0"));
    assertTrue(swings.isEmpty());
  }

  @Test
  void testSidewaysConsolidationProducesNoArtificialSwings() {
    List<PriceBar> bars = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      bars.add(bar(i, "100", "105", "95", "100"));
    }

    List<BigDecimal> atrSeries = IndicatorMath.calculateWilderAtrSeries(bars, 3);
    List<SwingPoint> swings = detector.detectSwings(bars, atrSeries, 3, new BigDecimal("2.0"));
    assertTrue(swings.isEmpty());
  }
}
