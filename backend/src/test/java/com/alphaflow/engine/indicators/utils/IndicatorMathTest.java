package com.alphaflow.engine.indicators.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.indicators.dtos.PriceBar;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class IndicatorMathTest {

  @Test
  void testConstructorIsPrivate() throws Exception {
    Constructor<IndicatorMath> constructor = IndicatorMath.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    constructor.setAccessible(true);
    InvocationTargetException exception =
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    assertTrue(exception.getCause() instanceof UnsupportedOperationException);
    assertEquals(
        "This is a utility class and cannot be instantiated", exception.getCause().getMessage());
  }

  @Test
  void testInternalAndPublishScaling() {
    BigDecimal input = new BigDecimal("10.123456789012345");
    BigDecimal internal = IndicatorMath.internal(input);
    assertEquals(IndicatorMath.INTERNAL_SCALE, internal.scale());

    BigDecimal published = IndicatorMath.publish(input);
    assertEquals(IndicatorMath.PUBLISHED_SCALE, published.scale());
    assertEquals(new BigDecimal("10.1235"), published);
  }

  @Test
  void testDivideAndAverage() {
    BigDecimal quotient = IndicatorMath.divide(new BigDecimal("10"), new BigDecimal("3"));
    assertEquals(IndicatorMath.INTERNAL_SCALE, quotient.scale());

    BigDecimal avg =
        IndicatorMath.average(
            List.of(new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30")));
    assertEquals(0, avg.compareTo(new BigDecimal("20")));
  }

  @Test
  void testMinAndMax() {
    assertNull(IndicatorMath.min(List.of()));
    assertNull(IndicatorMath.max(List.of()));

    List<BigDecimal> values =
        List.of(new BigDecimal("15"), new BigDecimal("5"), new BigDecimal("25"));
    assertEquals(new BigDecimal("5"), IndicatorMath.min(values));
    assertEquals(new BigDecimal("25"), IndicatorMath.max(values));
  }

  @Test
  void testCalculateTrueRange() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    PriceBar firstBar =
        new PriceBar(
            date.minusDays(1),
            new BigDecimal("100"),
            new BigDecimal("105"),
            new BigDecimal("98"),
            new BigDecimal("102"),
            new BigDecimal("1000"));
    assertEquals(new BigDecimal("7"), IndicatorMath.calculateTrueRange(firstBar, null));

    PriceBar normalBar =
        new PriceBar(
            date,
            new BigDecimal("100"),
            new BigDecimal("110"),
            new BigDecimal("95"),
            new BigDecimal("105"),
            new BigDecimal("1000"));
    assertEquals(new BigDecimal("15"), IndicatorMath.calculateTrueRange(normalBar, firstBar));

    PriceBar gapUpBar =
        new PriceBar(
            date,
            new BigDecimal("115"),
            new BigDecimal("120"),
            new BigDecimal("110"),
            new BigDecimal("118"),
            new BigDecimal("1000"));
    assertEquals(new BigDecimal("18"), IndicatorMath.calculateTrueRange(gapUpBar, firstBar));

    PriceBar gapDownBar =
        new PriceBar(
            date,
            new BigDecimal("85"),
            new BigDecimal("90"),
            new BigDecimal("80"),
            new BigDecimal("82"),
            new BigDecimal("1000"));
    assertEquals(new BigDecimal("22"), IndicatorMath.calculateTrueRange(gapDownBar, firstBar));
  }

  @Test
  void testCalculateWilderAtrSeries() {
    assertTrue(IndicatorMath.calculateWilderAtrSeries(null, 3).isEmpty());
    assertTrue(IndicatorMath.calculateWilderAtrSeries(List.of(), 3).isEmpty());

    LocalDate date = LocalDate.of(2026, 1, 1);
    List<PriceBar> bars =
        List.of(
            new PriceBar(
                date,
                new BigDecimal("100"),
                new BigDecimal("110"),
                new BigDecimal("100"),
                new BigDecimal("105"),
                BigDecimal.TEN), // TR = 10
            new PriceBar(
                date.plusDays(1),
                new BigDecimal("105"),
                new BigDecimal("115"),
                new BigDecimal("105"),
                new BigDecimal("110"),
                BigDecimal.TEN), // TR = 10
            new PriceBar(
                date.plusDays(2),
                new BigDecimal("110"),
                new BigDecimal("120"),
                new BigDecimal("110"),
                new BigDecimal("115"),
                BigDecimal.TEN), // TR = 10 -> Seed ATR(3) = 10
            new PriceBar(
                date.plusDays(3),
                new BigDecimal("115"),
                new BigDecimal("139"),
                new BigDecimal("115"),
                new BigDecimal("130"),
                BigDecimal.TEN)); // TR = 24 -> Wilder ATR = (10*2 + 24)/3 = 44/3

    assertTrue(IndicatorMath.calculateWilderAtrSeries(bars, 0).isEmpty());
    assertTrue(IndicatorMath.calculateWilderAtrSeries(bars, 5).isEmpty());

    List<BigDecimal> atrSeries = IndicatorMath.calculateWilderAtrSeries(bars, 3);
    assertEquals(4, atrSeries.size());
    assertNull(atrSeries.get(0));
    assertNull(atrSeries.get(1));
    assertEquals(0, new BigDecimal("10").compareTo(atrSeries.get(2)));
    assertEquals(new BigDecimal("14.6667"), IndicatorMath.publish(atrSeries.get(3)));
  }
}
