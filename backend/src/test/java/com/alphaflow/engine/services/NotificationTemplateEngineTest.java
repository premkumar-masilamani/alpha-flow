package com.alphaflow.engine.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationTemplateEngineTest {

  private NotificationTemplateEngine engine;

  @BeforeEach
  void setUp() {
    engine = new NotificationTemplateEngine();
  }

  @Test
  void testRenderValidTemplateWithVariables() {
    Map<String, String> vars =
        Map.of(
            "ticker", "NIFTY50\\.NS",
            "currentPrice", "24770",
            "previousDayLow", "24800",
            "timestamp", "2026-10-17 18:30:00");

    String result = engine.render("notifications/bear_breach.md", vars);
    assertNotNull(result);
    assertTrue(result.contains("BEARISH ALERT"));
    assertTrue(result.contains("NIFTY50\\.NS"));
    assertTrue(result.contains("24770"));
    assertTrue(result.contains("24800"));
  }

  @Test
  void testRenderNullOrEmptyVariables() {
    String resultWithNull = engine.render("notifications/bear_breach.md", null);
    assertNotNull(resultWithNull);
    assertTrue(resultWithNull.contains("BEARISH ALERT"));

    String resultWithEmpty = engine.render("notifications/bear_breach.md", Collections.emptyMap());
    assertNotNull(resultWithEmpty);
    assertTrue(resultWithEmpty.contains("BEARISH ALERT"));
  }

  @Test
  void testRenderWithNullValueRendersEmptyString() {
    Map<String, String> vars = Collections.singletonMap("ticker", null);
    String result = engine.render("notifications/bear_breach.md", vars);
    assertNotNull(result);
    assertTrue(result.contains("*Ticker:* \n"));
  }

  @Test
  void testTemplateNotFoundThrowsException() {
    Map<String, String> emptyMap = Map.of();
    assertThrows(
        IllegalArgumentException.class,
        () -> engine.render("notifications/non_existent.md", emptyMap));
  }

  @Test
  void testClearCache() {
    engine.render("notifications/bear_breach.md", Map.of("ticker", "NIFTY"));
    assertDoesNotThrow(() -> engine.clearCache());
    String resultAfterClear = engine.render("notifications/bear_breach.md", Map.of("ticker", "NIFTY"));
    assertNotNull(resultAfterClear);
  }
}
