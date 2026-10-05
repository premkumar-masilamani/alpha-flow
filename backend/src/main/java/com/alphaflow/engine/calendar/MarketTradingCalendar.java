package com.alphaflow.engine.calendar;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

@Component
public class MarketTradingCalendar {

  private final YahooFinanceConfig yahooFinanceConfig;

  public MarketTradingCalendar(YahooFinanceConfig yahooFinanceConfig) {
    this.yahooFinanceConfig = yahooFinanceConfig;
  }

  public boolean isTradingDay(LocalDate date) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();
    if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
      return !yahooFinanceConfig.getHolidaySet().contains(MonthDay.from(date));
    } else {
      return false;
    }
  }

  public LocalDate getExpectedLatestTradingDate(ZonedDateTime nowInMarket, LocalTime cutoffTime) {
    LocalDate candidate;
    if (!nowInMarket.toLocalTime().isBefore(cutoffTime)) {
      candidate = nowInMarket.toLocalDate();
    } else {
      candidate = nowInMarket.toLocalDate().minusDays(1);
    }

    while (!isTradingDay(candidate)) {
      candidate = candidate.minusDays(1);
    }
    return candidate;
  }
}
