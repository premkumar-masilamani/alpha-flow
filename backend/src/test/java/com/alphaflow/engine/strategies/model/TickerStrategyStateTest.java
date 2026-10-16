package com.alphaflow.engine.strategies.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TickerStrategyStateTest {

  @Test
  void testNoArgsConstructorAndSetters() {
    TickerStrategyState state = new TickerStrategyState();
    LocalDate sessionDate = LocalDate.of(2026, 10, 16);
    BigDecimal price = new BigDecimal("24000.50");

    state.setSessionDate(sessionDate);
    state.setBearBreachNotified(true);
    state.setBearLowestPrice(price);
    state.setBearReversalNotified(true);
    state.setBullBreachNotified(true);
    state.setBullHighestPrice(price);
    state.setBullReversalNotified(true);

    assertEquals(sessionDate, state.getSessionDate());
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
    TickerStrategyState state = new TickerStrategyState(sessionDate);

    assertEquals(sessionDate, state.getSessionDate());
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
    BigDecimal lowestPrice = new BigDecimal("23900.00");
    BigDecimal highestPrice = new BigDecimal("24600.00");

    TickerStrategyState state =
        TickerStrategyState.builder()
            .sessionDate(sessionDate)
            .bearBreachNotified(true)
            .bearLowestPrice(lowestPrice)
            .bearReversalNotified(false)
            .bullBreachNotified(false)
            .bullHighestPrice(highestPrice)
            .bullReversalNotified(true)
            .build();

    assertEquals(sessionDate, state.getSessionDate());
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

    TickerStrategyState state =
        TickerStrategyState.builder()
            .sessionDate(initialDate)
            .bearBreachNotified(true)
            .bearLowestPrice(new BigDecimal("23950.00"))
            .bearReversalNotified(true)
            .bullBreachNotified(true)
            .bullHighestPrice(new BigDecimal("24550.00"))
            .bullReversalNotified(true)
            .build();

    state.resetForNewSession(nextDate);

    assertEquals(nextDate, state.getSessionDate());
    assertFalse(state.isBearBreachNotified());
    assertNull(state.getBearLowestPrice());
    assertFalse(state.isBearReversalNotified());
    assertFalse(state.isBullBreachNotified());
    assertNull(state.getBullHighestPrice());
    assertFalse(state.isBullReversalNotified());
  }
}
