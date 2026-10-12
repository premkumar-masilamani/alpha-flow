package com.alphaflow.engine.calendar;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Set;

public final class MarketTradingCalendar {

  private MarketTradingCalendar() {}

  public static boolean isTradingDay(LocalDate date, Set<MonthDay> holidays) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();
    if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
      return holidays == null || !holidays.contains(MonthDay.from(date));
    } else {
      return false;
    }
  }

  public static LocalDate getPreviousTradingDay(LocalDate date, Set<MonthDay> holidays) {
    LocalDate candidate = date.minusDays(1);
    while (!isTradingDay(candidate, holidays)) {
      candidate = candidate.minusDays(1);
    }
    return candidate;
  }

  public static OffsetDateTime getExpectedLatestCompletedCandleTime(
      ZonedDateTime nowInMarket,
      LocalTime sessionStart,
      LocalTime sessionEnd,
      int candleMinutes,
      Set<MonthDay> holidays) {
    LocalTime lastCandleStartTime = sessionEnd.minusMinutes(candleMinutes);
    LocalDate currentDate = nowInMarket.toLocalDate();
    LocalTime currentTime = nowInMarket.toLocalTime();
    LocalTime firstCandleEndTime = sessionStart.plusMinutes(candleMinutes);

    if (isTradingDay(currentDate, holidays) && !currentTime.isBefore(firstCandleEndTime)) {
      if (currentTime.isBefore(sessionEnd)) {
        long elapsedMinutes = Duration.between(sessionStart, currentTime).toMinutes();
        long completedCandleCount = elapsedMinutes / candleMinutes;
        LocalTime candleStartTime =
            sessionStart.plusMinutes((completedCandleCount - 1) * candleMinutes);
        return currentDate.atTime(candleStartTime).atZone(nowInMarket.getZone()).toOffsetDateTime();
      } else {
        return currentDate
            .atTime(lastCandleStartTime)
            .atZone(nowInMarket.getZone())
            .toOffsetDateTime();
      }
    } else {
      LocalDate previousTradingDay = getPreviousTradingDay(currentDate, holidays);
      return previousTradingDay
          .atTime(lastCandleStartTime)
          .atZone(nowInMarket.getZone())
          .toOffsetDateTime();
    }
  }

  public static LocalDate getExpectedLatestTradingDate(
      ZonedDateTime nowInMarket, LocalTime cutoffTime, Set<MonthDay> holidays) {
    LocalDate candidate;
    if (!nowInMarket.toLocalTime().isBefore(cutoffTime)) {
      candidate = nowInMarket.toLocalDate();
    } else {
      candidate = nowInMarket.toLocalDate().minusDays(1);
    }

    while (!isTradingDay(candidate, holidays)) {
      candidate = candidate.minusDays(1);
    }
    return candidate;
  }
}
