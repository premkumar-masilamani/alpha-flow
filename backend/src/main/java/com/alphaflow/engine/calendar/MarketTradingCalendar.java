package com.alphaflow.engine.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
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
