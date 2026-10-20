package com.alphaflow.engine.strategies.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class Intraday70PBounceStrategyStateTest {

  @Test
  void testNoArgsConstructorAndSetters() {
    Intraday70PBounceStrategyState state = new Intraday70PBounceStrategyState();
    LocalDate sessionDate = LocalDate.of(2026, 10, 16);
    LocalDate previousTradingDate = LocalDate.of(2026, 10, 15);
    BigDecimal price = new BigDecimal("24000.50");
    BigDecimal high = new BigDecimal("24500.00");
    BigDecimal low = new BigDecimal("23900.00");

    state.setSessionDate(sessionDate);
    state.setPreviousTradingDate(previousTradingDate);
    state.setPreviousDayHigh(high);
    state.setPreviousDayLow(low);
    state.setBearBreachNotified(true);
    state.setBearLowestPrice(price);
    state.setBearReversalNotified(true);
    state.setBullBreachNotified(true);
    state.setBullHighestPrice(price);
    state.setBullReversalNotified(true);

    assertEquals(sessionDate, state.getSessionDate());
    assertEquals(previousTradingDate, state.getPreviousTradingDate());
    assertEquals(high, state.getPreviousDayHigh());
    assertEquals(low, state.getPreviousDayLow());
    assertTrue(state.isBearBreachNotified());
    assertEquals(price, state.getBearLowestPrice());
    assertTrue(state.isBearReversalNotified());
    assertTrue(state.isBullBreachNotified());
    assertEquals(price, state.getBullHighestPrice());
    assertTrue(state.isBullReversalNotified());
  }

  @Test
  void testSessionDateConstructor() {
    LocalDate sessionDate = LocalDate.of(2026, 10, 16);
    Intraday70PBounceStrategyState state = new Intraday70PBounceStrategyState(sessionDate);

    assertEquals(sessionDate, state.getSessionDate());
    assertNull(state.getPreviousTradingDate());
    assertNull(state.getPreviousDayHigh());
    assertNull(state.getPreviousDayLow());
    assertFalse(state.isBearBreachNotified());
    assertNull(state.getBearLowestPrice());
    assertFalse(state.isBearReversalNotified());
    assertFalse(state.isBullBreachNotified());
    assertNull(state.getBullHighestPrice());
    assertFalse(state.isBullReversalNotified());
  }

  @Test
  void testSessionAndPreviousDayConstructor() {
    LocalDate sessionDate = LocalDate.of(2026, 10, 16);
    LocalDate previousTradingDate = LocalDate.of(2026, 10, 15);
    BigDecimal high = new BigDecimal("24500.00");
    BigDecimal low = new BigDecimal("23900.00");

    Intraday70PBounceStrategyState state =
        new Intraday70PBounceStrategyState(sessionDate, previousTradingDate, high, low);

    assertEquals(sessionDate, state.getSessionDate());
    assertEquals(previousTradingDate, state.getPreviousTradingDate());
    assertEquals(high, state.getPreviousDayHigh());
    assertEquals(low, state.getPreviousDayLow());
    assertFalse(state.isBearBreachNotified());
    assertNull(state.getBearLowestPrice());
    assertFalse(state.isBearReversalNotified());
    assertFalse(state.isBullBreachNotified());
    assertNull(state.getBullHighestPrice());
    assertFalse(state.isBullReversalNotified());
  }

  @Test
  void testAllArgsConstructorAndBuilder() {
    LocalDate sessionDate = LocalDate.of(2026, 10, 16);
    LocalDate previousTradingDate = LocalDate.of(2026, 10, 15);
    BigDecimal high = new BigDecimal("24500.00");
    BigDecimal low = new BigDecimal("23900.00");
    BigDecimal lowestPrice = new BigDecimal("23900.00");
    BigDecimal highestPrice = new BigDecimal("24600.00");

    Intraday70PBounceStrategyState state =
        Intraday70PBounceStrategyState.builder()
            .sessionDate(sessionDate)
            .previousTradingDate(previousTradingDate)
            .previousDayHigh(high)
            .previousDayLow(low)
            .bearBreachNotified(true)
            .bearLowestPrice(lowestPrice)
            .bearReversalNotified(false)
            .bullBreachNotified(false)
            .bullHighestPrice(highestPrice)
            .bullReversalNotified(true)
            .build();

    assertEquals(sessionDate, state.getSessionDate());
    assertEquals(previousTradingDate, state.getPreviousTradingDate());
    assertEquals(high, state.getPreviousDayHigh());
    assertEquals(low, state.getPreviousDayLow());
    assertTrue(state.isBearBreachNotified());
    assertEquals(lowestPrice, state.getBearLowestPrice());
    assertFalse(state.isBearReversalNotified());
    assertFalse(state.isBullBreachNotified());
    assertEquals(highestPrice, state.getBullHighestPrice());
    assertTrue(state.isBullReversalNotified());
  }

  @Test
  void testResetForNewSession() {
    LocalDate initialDate = LocalDate.of(2026, 10, 15);
    LocalDate nextDate = LocalDate.of(2026, 10, 16);

    Intraday70PBounceStrategyState state =
        Intraday70PBounceStrategyState.builder()
            .sessionDate(initialDate)
            .previousTradingDate(LocalDate.of(2026, 10, 14))
            .previousDayHigh(new BigDecimal("24500.00"))
            .previousDayLow(new BigDecimal("23900.00"))
            .bearBreachNotified(true)
            .bearLowestPrice(new BigDecimal("23950.00"))
            .bearReversalNotified(true)
            .bullBreachNotified(true)
            .bullHighestPrice(new BigDecimal("24550.00"))
            .bullReversalNotified(true)
            .build();

    state.resetForNewSession(nextDate);

    assertEquals(nextDate, state.getSessionDate());
    assertNull(state.getPreviousTradingDate());
    assertNull(state.getPreviousDayHigh());
    assertNull(state.getPreviousDayLow());
    assertFalse(state.isBearBreachNotified());
    assertNull(state.getBearLowestPrice());
    assertFalse(state.isBearReversalNotified());
    assertFalse(state.isBullBreachNotified());
    assertNull(state.getBullHighestPrice());
    assertFalse(state.isBullReversalNotified());
  }
}
