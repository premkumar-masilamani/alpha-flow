package com.alphaflow.engine.schedulers;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility functions for market schedulers that operate purely on input parameters.
 */
@Slf4j
public final class SchedulerUtils {

  /**
   * Formats execution duration into human-readable minutes, seconds, and milliseconds.
   *
   * @param durationMilliseconds duration in milliseconds
   * @return formatted duration string
   */
  public static String formatDuration(long durationMilliseconds) {
    Duration d = Duration.ofMillis(durationMilliseconds);
    return String.format("%d m %d s (%d ms)", d.toMinutes(), d.toSecondsPart(), durationMilliseconds);
  }

  /**
   * Parses an ISO-8601 quote timestamp, returning a fallback timestamp if invalid or unparseable.
   *
   * @param rawTimestamp      raw timestamp string from the provider
   * @param fallbackTimestamp fallback timestamp to use on parse failure
   * @return parsed OffsetDateTime or fallback
   */
  public static OffsetDateTime parseQuoteTimestamp(
      String rawTimestamp, OffsetDateTime fallbackTimestamp) {
    if (rawTimestamp != null && !rawTimestamp.isBlank()) {
      try {
        return OffsetDateTime.parse(rawTimestamp);
      } catch (DateTimeParseException exception) {
        log.debug(
            "Falling back to pipeline timestamp {} for quote timestamp '{}'",
            fallbackTimestamp,
            rawTimestamp);
        return fallbackTimestamp;
      }
    } else {
      return fallbackTimestamp;
    }
  }
}
