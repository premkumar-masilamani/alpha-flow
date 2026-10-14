package com.alphaflow.common.diagnostics;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class StartupFailureAnalyzer extends AbstractFailureAnalyzer<Throwable> {

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, Throwable cause) {
    Throwable effectiveCause;
    if (cause != null) {
      effectiveCause = cause;
    } else {
      effectiveCause = rootFailure;
    }
    String rootMessage = extractRootMessage(effectiveCause);
    String action = determineAction(rootMessage);

    return new FailureAnalysis(rootMessage, action, effectiveCause);
  }

  public static String extractRootMessage(Throwable throwable) {
    if (throwable != null) {
      Throwable root = throwable;
      while (root.getCause() != null && !root.equals(root.getCause())) {
        root = root.getCause();
      }
      String message = root.getMessage();
      if (message != null && !message.isBlank()) {
        return message;
      } else {
        return root.getClass().getSimpleName();
      }
    } else {
      return "Unknown";
    }
  }

  public static String determineAction(String message) {
    if (message != null) {
      if (message.contains("alphaflow.angelone") || message.contains("ANGEL_ONE")) {
        return "Configure the required Angel One environment variables "
            + "(ANGEL_ONE_API_KEY, ANGEL_ONE_CLIENT_CODE, ANGEL_ONE_PASSWORD, ANGEL_ONE_TOTP_KEY) in your .env file.";
      } else if (message.contains("alphaflow.cors") || message.contains("CORS_ALLOWED_ORIGINS")) {
        return "Configure CORS_ALLOWED_ORIGINS in your .env file (e.g. http://localhost:5173).";
      } else if (message.contains("alphaflow.market.holidays")) {
        return "Configure market holiday lists in application.properties "
            + "(alphaflow.market.holidays.us / alphaflow.market.holidays.in).";
      } else if (message.contains("Connection refused")
          || message.contains("POSTGRES")
          || message.contains("jdbcUrl")) {
        return "Ensure PostgreSQL is running (run 'make run_database') and verify POSTGRES_* credentials in .env.";
      } else {
        return "Check your configuration in .env and application.properties.";
      }
    } else {
      return "Check application configuration and logs for details.";
    }
  }
}
