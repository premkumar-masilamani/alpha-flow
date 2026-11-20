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
import java.util.ArrayList;
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
        "This is a utility class and cannot be instantiated",
        exception.getCause().getMessage());
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
    PriceBar previous =
        new PriceBar(
            date.minusDays(1),
            new BigDecimal("100"),
            new BigDecimal("105"),
            new BigDecimal("98"),
            new BigDecimal("102"),
            new BigDecimal("1000"));

    // Case 1: high - low is greatest (110 - 95 = 15) vs |110 - 102| = 8, |95 - 102| = 7
    PriceBar bar1 =
        new PriceBar(
            date,
            new BigDecimal("100"),
            new BigDecimal("110"),
            new BigDecimal("95"),
            new BigDecimal("105"),
            new BigDecimal("1000"));
    assertEquals(new BigDecimal("15"), IndicatorMath.calculateTrueRange(bar1, previous));

    // Case 2: |high - prevClose| is greatest (gap up: high=120, low=110, prevClose=102 -> 18)
    PriceBar bar2 =
        new PriceBar(
            date,
            new BigDecimal("115"),
            new BigDecimal("120"),
            new BigDecimal("110"),
            new BigDecimal("118"),
            new BigDecimal("1000"));
    assertEquals(new BigDecimal("18"), IndicatorMath.calculateTrueRange(bar2, previous));

    // Case 3: |low - prevClose| is greatest (gap down: high=90, low=80, prevClose=102 -> 22)
    PriceBar bar3 =
        new PriceBar(
            date,
            new BigDecimal("85"),
            new BigDecimal("90"),
            new BigDecimal("80"),
            new BigDecimal("82"),
            new BigDecimal("1000"));
    assertEquals(new BigDecimal("22"), IndicatorMath.calculateTrueRange(bar3, previous));
  }

  @Test
  void testCalculateAtr() {
    assertEquals(BigDecimal.ZERO, IndicatorMath.calculateAtr(null, 14));
    assertEquals(BigDecimal.ZERO, IndicatorMath.calculateAtr(List.of(), 14));
    assertEquals(BigDecimal.ZERO, IndicatorMath.calculateAtr(List.of(), 0));

    LocalDate date = LocalDate.of(2026, 1, 1);
    List<PriceBar> bars = new ArrayList<>();
    for (int i = 0; i < 15; i++) {
      bars.add(
          new PriceBar(
              date.plusDays(i),
              new BigDecimal("100"),
              new BigDecimal("105"),
              new BigDecimal("95"),
              new BigDecimal("100"),
              new BigDecimal("1000")));
    }

    BigDecimal atr = IndicatorMath.calculateAtr(bars, 14);
    assertEquals(0, atr.compareTo(new BigDecimal("10.0000")));

    // When bars size is equal to 1 and period is 1, count is 0 because loop starts at max(1, 1 - 1) = 1
    PriceBar singleBar =
        new PriceBar(
            date,
            new BigDecimal("100"),
            new BigDecimal("105"),
            new BigDecimal("95"),
            new BigDecimal("100"),
            new BigDecimal("1000"));
    assertEquals(BigDecimal.ZERO, IndicatorMath.calculateAtr(List.of(singleBar), 1));
  }
}
