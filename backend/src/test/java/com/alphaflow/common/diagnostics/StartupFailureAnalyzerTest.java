package com.alphaflow.common.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

class StartupFailureAnalyzerTest {

  @Test
  void testAnalyzeWithCause() {
    StartupFailureAnalyzer analyzer = new StartupFailureAnalyzer();
    Exception root = new IllegalArgumentException("alphaflow.angelone.apiKey is missing");
    Exception wrapper = new RuntimeException("Context failed", root);

    FailureAnalysis analysis = analyzer.analyze(wrapper, root);

    assertNotNull(analysis);
    assertEquals("alphaflow.angelone.apiKey is missing", analysis.getDescription());
    assertTrue(analysis.getAction().contains("Angel One"));
    assertEquals(root, analysis.getCause());
  }

  @Test
  void testAnalyzeWithoutCause() {
    StartupFailureAnalyzer analyzer = new StartupFailureAnalyzer();
    Exception root = new IllegalStateException("CORS_ALLOWED_ORIGINS is not configured");

    FailureAnalysis analysis = analyzer.analyze(root, null);

    assertNotNull(analysis);
    assertEquals("CORS_ALLOWED_ORIGINS is not configured", analysis.getDescription());
    assertTrue(analysis.getAction().contains("CORS_ALLOWED_ORIGINS"));
    assertEquals(root, analysis.getCause());
  }

  @Test
  void testExtractRootMessageVariants() {
    assertEquals("Unknown", StartupFailureAnalyzer.extractRootMessage(null));

    Exception exWithoutMessage = new RuntimeException();
    assertEquals("RuntimeException", StartupFailureAnalyzer.extractRootMessage(exWithoutMessage));

    Exception exWithBlankMessage = new RuntimeException("   ");
    assertEquals("RuntimeException", StartupFailureAnalyzer.extractRootMessage(exWithBlankMessage));

    Exception nested = new RuntimeException("outer", new IllegalStateException("inner"));
    assertEquals("inner", StartupFailureAnalyzer.extractRootMessage(nested));

    Throwable mockThrowable = org.mockito.Mockito.mock(Throwable.class);
    org.mockito.Mockito.when(mockThrowable.getCause()).thenReturn(mockThrowable);
    org.mockito.Mockito.when(mockThrowable.getMessage()).thenReturn("mock-loop");
    assertEquals("mock-loop", StartupFailureAnalyzer.extractRootMessage(mockThrowable));
  }

  @Test
  void testDetermineActionBranches() {
    assertEquals(
        "Check application configuration and logs for details.",
        StartupFailureAnalyzer.determineAction(null));

    assertTrue(
        StartupFailureAnalyzer.determineAction("alphaflow.angelone.apiKey is missing")
            .contains("Angel One"));
    assertTrue(
        StartupFailureAnalyzer.determineAction("Missing ANGEL_ONE_CLIENT_CODE")
            .contains("Angel One"));

    assertTrue(
        StartupFailureAnalyzer.determineAction("alphaflow.cors.allowed-origins is not configured")
            .contains("CORS_ALLOWED_ORIGINS"));
    assertTrue(
        StartupFailureAnalyzer.determineAction("CORS_ALLOWED_ORIGINS is empty")
            .contains("CORS_ALLOWED_ORIGINS"));

    assertTrue(
        StartupFailureAnalyzer.determineAction("Could not resolve alphaflow.market.holidays.us")
            .contains("alphaflow.market.holidays"));

    assertTrue(
        StartupFailureAnalyzer.determineAction("Connection refused to database")
            .contains("PostgreSQL"));
    assertTrue(
        StartupFailureAnalyzer.determineAction("Failed for POSTGRES_PASSWORD")
            .contains("PostgreSQL"));
    assertTrue(
        StartupFailureAnalyzer.determineAction("Missing jdbcUrl property").contains("PostgreSQL"));

    assertEquals(
        "Check your configuration in .env and application.properties.",
        StartupFailureAnalyzer.determineAction("Some arbitrary failure"));
  }
}
