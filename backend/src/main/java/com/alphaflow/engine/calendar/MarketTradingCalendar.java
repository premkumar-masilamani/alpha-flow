package com.alphaflow.engine.calendar;

import com.alphaflow.persistence.enums.Country;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MarketTradingCalendar {

  private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
  private static final Map<Country, Set<MonthDay>> HOLIDAYS_BY_COUNTRY =
      new EnumMap<>(Country.class);

  public MarketTradingCalendar(
      @Value("${alphaflow.market.holidays.us}") List<String> usHolidays,
      @Value("${alphaflow.market.holidays.in}") List<String> inHolidays) {
    setHolidays(Country.US, usHolidays);
    setHolidays(Country.IN, inHolidays);
  }

  public static void setHolidays(Country country, List<String> holidays) {
    if (holidays != null) {
      Set<MonthDay> parsed =
          holidays.stream()
              .map(holiday -> MonthDay.parse(holiday.trim(), MONTH_DAY_FORMATTER))
              .collect(Collectors.toUnmodifiableSet());
      HOLIDAYS_BY_COUNTRY.put(country, parsed);
    } else {
      HOLIDAYS_BY_COUNTRY.put(country, Set.of());
    }
  }

  public static Set<MonthDay> getHolidays(Country country) {
    return HOLIDAYS_BY_COUNTRY.getOrDefault(country, Set.of());
  }

  public static boolean isTradingDay(LocalDate date, Country country) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();
    if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
      return !getHolidays(country).contains(MonthDay.from(date));
    } else {
      return false;
    }
  }

  public static LocalDate getPreviousTradingDay(LocalDate date, Country country) {
    LocalDate candidate = date.minusDays(1);
    while (!isTradingDay(candidate, country)) {
      candidate = candidate.minusDays(1);
    }
    return candidate;
  }

  public static OffsetDateTime getExpectedLatestCompletedCandleTime(
      ZonedDateTime nowInMarket,
      LocalTime sessionStart,
      LocalTime sessionEnd,
      int candleMinutes,
      Country country) {
    LocalTime lastCandleStartTime = sessionEnd.minusMinutes(candleMinutes);
    LocalDate currentDate = nowInMarket.toLocalDate();
    LocalTime currentTime = nowInMarket.toLocalTime();
    LocalTime firstCandleEndTime = sessionStart.plusMinutes(candleMinutes);

    if (isTradingDay(currentDate, country) && !currentTime.isBefore(firstCandleEndTime)) {
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
      LocalDate previousTradingDay = getPreviousTradingDay(currentDate, country);
      return previousTradingDay
          .atTime(lastCandleStartTime)
          .atZone(nowInMarket.getZone())
          .toOffsetDateTime();
    }
  }

  public static LocalDate getExpectedLatestTradingDate(
      ZonedDateTime nowInMarket, LocalTime cutoffTime, Country country) {
    LocalDate candidate;
    if (nowInMarket.toLocalTime().isBefore(cutoffTime)) {
      candidate = nowInMarket.toLocalDate().minusDays(1);
    } else {
      candidate = nowInMarket.toLocalDate();
    }

    while (!isTradingDay(candidate, country)) {
      candidate = candidate.minusDays(1);
    }
    return candidate;
  }
}
