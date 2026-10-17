package com.alphaflow.engine.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  void testRenderNullOrEmptyVariablesReturnsTemplate() {
    String rawTemplate = engine.getTemplate("notifications/bear_breach.md");
    assertEquals(rawTemplate, engine.render("notifications/bear_breach.md", null));
    assertEquals(rawTemplate, engine.render("notifications/bear_breach.md", Collections.emptyMap()));
  }

  @Test
  void testRenderWithNullValueReplacesWithEmpty() {
    Map<String, String> vars = Collections.singletonMap("ticker", null);
    String result = engine.render("notifications/bear_breach.md", vars);
    assertTrue(result.contains("*Ticker:* \n"));
  }

  @Test
  void testTemplateNotFoundThrowsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> engine.getTemplate("notifications/non_existent.md"));
  }

  @Test
  void testClearCache() {
    engine.getTemplate("notifications/bear_breach.md");
    engine.clearCache();
    // Re-accessing after cache clear succeeds by re-reading
    assertNotNull(engine.getTemplate("notifications/bear_breach.md"));
  }
}
