package com.alphaflow.engine.schedulers;

import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.persistence.enums.Country;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility functions for market schedulers that operate purely on input parameters.
 */
@Slf4j
public final class SchedulerUtils {

  private static final long MILLIS_PER_MINUTE = 60000;
  private static final long MILLIS_PER_SECOND = 1000;

  private SchedulerUtils() {}

  /**
   * Formats execution duration into human-readable minutes, seconds, and milliseconds.
   *
   * @param durationMilliseconds duration in milliseconds
   * @return formatted duration string
   */
  public static String formatDuration(long durationMilliseconds) {
    long minutes = durationMilliseconds / MILLIS_PER_MINUTE;
    long seconds = (durationMilliseconds % MILLIS_PER_MINUTE) / MILLIS_PER_SECOND;
    return String.format("%d m %d s (%d ms)", minutes, seconds, durationMilliseconds);
  }

  /**
   * Determines whether the market session is currently open for trading.
   *
   * @param currentMarketTime current timestamp in the market timezone
   * @param sessionStartTime  start time of the market session
   * @param sessionEndTime    end time of the market session
   * @param country           market country
   * @return true if market is open and within session bounds, false otherwise
   */
  public static boolean isMarketSessionOpen(
      ZonedDateTime currentMarketTime,
      LocalTime sessionStartTime,
      LocalTime sessionEndTime,
      Country country) {
    LocalTime currentTime = currentMarketTime.toLocalTime();
    return MarketTradingCalendar.isTradingDay(currentMarketTime.toLocalDate(), country)
        && !currentTime.isBefore(sessionStartTime)
        && !currentTime.isAfter(sessionEndTime);
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
