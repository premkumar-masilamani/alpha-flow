package com.alphaflow.engine.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alphaflow.engine.structure.dtos.SwingPoint;
import com.alphaflow.engine.structure.enums.DowTrend;
import com.alphaflow.engine.structure.enums.SwingType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DowTrendClassifierTest {

  private final DowTrendClassifier classifier = new DowTrendClassifier();

  private SwingPoint swing(int barIndex, String price, SwingType type) {
    return new SwingPoint(
        barIndex,
        LocalDate.of(2026, 1, 1).plusDays(barIndex),
        new BigDecimal(price),
        type,
        new BigDecimal("2.0000"));
  }

  @Test
  void testInvalidOrInsufficientInputsReturnSideways() {
    List<SwingPoint> shortSwings =
        List.of(
            swing(1, "100", SwingType.LOW),
            swing(2, "110", SwingType.HIGH),
            swing(3, "105", SwingType.LOW));

    assertEquals(DowTrend.SIDEWAYS, classifier.classifyPriorTrend(null, 4));
    assertEquals(DowTrend.SIDEWAYS, classifier.classifyPriorTrend(shortSwings, 3));
    assertEquals(DowTrend.SIDEWAYS, classifier.classifyPriorTrend(shortSwings, 5));
  }

  @Test
  void testUptrendStartingWithLow() {
    List<SwingPoint> swings =
        List.of(
            swing(1, "100", SwingType.LOW),
            swing(2, "110", SwingType.HIGH),
            swing(3, "105", SwingType.LOW),
            swing(4, "120", SwingType.HIGH),
            swing(5, "115", SwingType.LOW));

    assertEquals(DowTrend.UPTREND, classifier.classifyPriorTrend(swings, 4));
  }

  @Test
  void testUptrendStartingWithHigh() {
    List<SwingPoint> swings =
        List.of(
            swing(1, "110", SwingType.HIGH),
            swing(2, "100", SwingType.LOW),
            swing(3, "120", SwingType.HIGH),
            swing(4, "108", SwingType.LOW),
            swing(5, "125", SwingType.HIGH));

    assertEquals(DowTrend.UPTREND, classifier.classifyPriorTrend(swings, 4));
  }

  @Test
  void testDowntrendStartingWithHigh() {
    List<SwingPoint> swings =
        List.of(
            swing(1, "120", SwingType.HIGH),
            swing(2, "110", SwingType.LOW),
            swing(3, "115", SwingType.HIGH),
            swing(4, "100", SwingType.LOW),
            swing(5, "108", SwingType.HIGH));

    assertEquals(DowTrend.DOWNTREND, classifier.classifyPriorTrend(swings, 4));
  }

  @Test
  void testDowntrendStartingWithLow() {
    List<SwingPoint> swings =
        List.of(
            swing(1, "110", SwingType.LOW),
            swing(2, "120", SwingType.HIGH),
            swing(3, "100", SwingType.LOW),
            swing(4, "115", SwingType.HIGH),
            swing(5, "95", SwingType.LOW));

    assertEquals(DowTrend.DOWNTREND, classifier.classifyPriorTrend(swings, 4));
  }

  @Test
  void testMixedSwingsReturnSideways() {
    // Higher High + Lower Low (expanding range)
    List<SwingPoint> expanding =
        List.of(
            swing(1, "110", SwingType.HIGH),
            swing(2, "100", SwingType.LOW),
            swing(3, "120", SwingType.HIGH),
            swing(4, "95", SwingType.LOW));
    assertEquals(DowTrend.SIDEWAYS, classifier.classifyPriorTrend(expanding, 4));

    // Lower High + Higher Low (contracting range)
    List<SwingPoint> contracting =
        List.of(
            swing(1, "120", SwingType.HIGH),
            swing(2, "100", SwingType.LOW),
            swing(3, "115", SwingType.HIGH),
            swing(4, "105", SwingType.LOW));
    assertEquals(DowTrend.SIDEWAYS, classifier.classifyPriorTrend(contracting, 4));
  }
}
