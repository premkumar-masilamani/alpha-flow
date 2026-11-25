package com.alphaflow.engine.calculators;

import com.alphaflow.common.enums.Timeframe;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Pure deterministic gating predicates determining whether an active ticker requires calculation on
 * daily or weekly timeframes without loading full price history.
 */
@Slf4j
@UtilityClass
public class PipelineGating {

  /**
   * Determines whether a daily calculation is required for a ticker.
   *
   * @param latestSourceDate latest persisted daily price date (null if no daily prices exist)
   * @param latestCalculatedDate latest persisted calculated date (null if never computed)
   * @return {@code true} iff source data exists and is newer than the latest calculated date
   */
  public static boolean requiresDailyUpdate(
      LocalDate latestSourceDate, LocalDate latestCalculatedDate) {
    return latestSourceDate != null
        && (latestCalculatedDate == null || latestCalculatedDate.isBefore(latestSourceDate));
  }

  /**
   * Determines whether weekly OHLCV bar aggregation is required for a ticker.
   *
   * <p>Weekly candles are keyed by Monday's date. During a trading weekday (Monday through Friday),
   * an already-persisted row for the current week is refreshed idempotently as Tue–Fri daily closes
   * arrive; on weekends (Saturday/Sunday) and for historical weeks, once the latest weekly bar
   * matches the latest daily bar's week Monday, the ticker is skipped.
   *
   * @param latestDailyDate latest persisted daily price date (null if no daily prices exist)
   * @param latestWeeklyDate latest persisted weekly price Monday date (null if never computed)
   * @param today current date in UTC
   * @return {@code true} iff weekly aggregation must run for this ticker
   */
  public static boolean requiresWeeklyPriceUpdate(
      LocalDate latestDailyDate, LocalDate latestWeeklyDate, LocalDate today) {
    if (latestDailyDate != null) {
      LocalDate latestDailyWeekMonday =
          latestDailyDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      return latestWeeklyDate == null
          || latestWeeklyDate.isBefore(latestDailyWeekMonday)
          || isInProgressTradingWeek(latestDailyWeekMonday, today);
    } else {
      return false;
    }
  }

  /**
   * Determines whether a downstream weekly calculation (S&amp;R, candlestick patterns, indicators,
   * chart patterns) is required for a ticker.
   *
   * @param latestWeeklyPriceDate latest persisted weekly price Monday date (null if none exist)
   * @param latestCalculatedDate latest persisted calculated date (null if never computed)
   * @param today current date in UTC
   * @return {@code true} iff the weekly downstream calculation must run for this ticker
   */
  public static boolean requiresWeeklyUpdate(
      LocalDate latestWeeklyPriceDate, LocalDate latestCalculatedDate, LocalDate today) {
    return latestWeeklyPriceDate != null
        && (latestCalculatedDate == null
            || latestCalculatedDate.isBefore(latestWeeklyPriceDate)
            || isInProgressTradingWeek(latestWeeklyPriceDate, today));
  }

  /**
   * Returns {@code true} when {@code today} is a trading weekday (Monday–Friday) and {@code
   * weekMonday} is the Monday of {@code today}'s trading week.
   *
   * @param weekMonday Monday anchor date of a weekly candle
   * @param today current date in UTC
   * @return {@code true} iff {@code weekMonday} represents the currently active weekday trading
   *     week
   */
  public static boolean isInProgressTradingWeek(LocalDate weekMonday, LocalDate today) {
    DayOfWeek dayOfWeek = today.getDayOfWeek();
    boolean isTradingWeekday = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    LocalDate currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    return isTradingWeekday && weekMonday.equals(currentWeekMonday);
  }

  /**
   * Dispatches gating evaluation by {@link Timeframe}.
   *
   * @param timeframe target market timeframe
   * @param latestSourceDate latest persisted source price date
   * @param latestCalculatedDate latest persisted calculated date
   * @param today current date in UTC
   * @return {@code true} iff the ticker requires processing for the requested timeframe
   * @throws IllegalArgumentException if {@code timeframe} is null or unsupported
   */
  public static boolean requiresUpdate(
      Timeframe timeframe,
      LocalDate latestSourceDate,
      LocalDate latestCalculatedDate,
      LocalDate today) {
    if (timeframe == Timeframe.DAILY) {
      return requiresDailyUpdate(latestSourceDate, latestCalculatedDate);
    } else if (timeframe == Timeframe.WEEKLY) {
      return requiresWeeklyUpdate(latestSourceDate, latestCalculatedDate, today);
    } else {
      log.error("Unsupported timeframe for pipeline gating check: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }
}
