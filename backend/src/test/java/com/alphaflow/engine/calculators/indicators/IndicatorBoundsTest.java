package com.alphaflow.engine.calculators.indicators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alphaflow.engine.indicators.dtos.IndicatorBounds;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IndicatorBoundsTest {

  @Test
  void testOfValues() {
    IndicatorBounds bounds = IndicatorBounds.of(20, 80);
    assertNotNull(bounds);
    assertEquals(20, bounds.getLower());
    assertEquals(80, bounds.getUpper());
    assertEquals("lower=20,upper=80", bounds.canonical());
    assertEquals("lower=20,upper=80", bounds.toString());
  }

  @Test
  void testOfMap() {
    assertNull(IndicatorBounds.of((Map<String, Integer>) null));
    assertNull(IndicatorBounds.of(Map.of()));

    IndicatorBounds bounds = IndicatorBounds.of(Map.of("lower", 30, "upper", 70));
    assertNotNull(bounds);
    assertEquals(30, bounds.getLower());
    assertEquals(70, bounds.getUpper());
    assertEquals("lower=30,upper=70", bounds.canonical());
  }

  @Test
  void testParse() {
    assertNull(IndicatorBounds.parse(null));
    assertNull(IndicatorBounds.parse(""));
    assertNull(IndicatorBounds.parse("   "));

    IndicatorBounds bounds = IndicatorBounds.parse("lower=25, upper=75");
    assertNotNull(bounds);
    assertEquals(25, bounds.getLower());
    assertEquals(75, bounds.getUpper());
    assertEquals("lower=25,upper=75", bounds.canonical());

    IndicatorBounds reversed = IndicatorBounds.parse("upper=85,lower=15");
    assertNotNull(reversed);
    assertEquals(15, reversed.getLower());
    assertEquals(85, reversed.getUpper());
    assertEquals("lower=15,upper=85", reversed.canonical());

    IndicatorBounds withOtherKey = IndicatorBounds.parse("ignored=99,lower=20,upper=80");
    assertNotNull(withOtherKey);
    assertEquals(20, withOtherKey.getLower());
    assertEquals(80, withOtherKey.getUpper());
    assertEquals("lower=20,upper=80", withOtherKey.canonical());
  }

  @Test
  void testCanonicalPartial() {
    IndicatorBounds lowerOnly = IndicatorBounds.of(20, null);
    assertEquals("lower=20", lowerOnly.canonical());

    IndicatorBounds upperOnly = IndicatorBounds.of(null, 80);
    assertEquals("upper=80", upperOnly.canonical());

    IndicatorBounds none = IndicatorBounds.of(null, null);
    assertEquals("", none.canonical());
  }

  @Test
  void testParseMalformed() {
    assertThrows(IllegalArgumentException.class, () -> IndicatorBounds.parse("invalid"));
    assertThrows(NumberFormatException.class, () -> IndicatorBounds.parse("lower=abc,upper=80"));
  }

  @Test
  void testEqualsAndHashCode() {
    IndicatorBounds bounds1 = IndicatorBounds.of(20, 80);
    IndicatorBounds bounds2 = IndicatorBounds.of(20, 80);
    IndicatorBounds bounds3 = IndicatorBounds.of(30, 70);

    assertEquals(bounds1, bounds2);
    assertEquals(bounds1.hashCode(), bounds2.hashCode());
    assertNotEquals(bounds1, bounds3);
    assertNotEquals(bounds1, null);
    assertNotEquals(bounds1, "other");
  }
}
