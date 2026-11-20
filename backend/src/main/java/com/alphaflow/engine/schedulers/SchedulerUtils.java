package com.alphaflow.engine.schedulers;

import java.time.Duration;

/**
 * Utility functions for market schedulers that operate purely on input parameters.
 */
public final class SchedulerUtils {

  private SchedulerUtils() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * Formats execution duration into human-readable minutes, seconds, and milliseconds.
   *
   * @param durationMilliseconds duration in milliseconds
   * @return formatted duration string
   */
  public static String formatDuration(long durationMilliseconds) {
    Duration duration = Duration.ofMillis(durationMilliseconds);
    return String.format(
        "%d m %d s (%d ms)", duration.toMinutes(), duration.toSecondsPart(), durationMilliseconds);
  }
}
