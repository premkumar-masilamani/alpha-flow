package com.alphaflow.engine.calendar;

import com.alphaflow.persistence.entities.MarketHoliday;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.repositories.MarketHolidayRepository;
import jakarta.annotation.PostConstruct;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MarketTradingCalendar {

  private static final Map<Country, Set<LocalDate>> HOLIDAYS_BY_COUNTRY =
      new EnumMap<>(Country.class);
  private static final Map<Country, Map<LocalDate, String>> HOLIDAY_NAMES_BY_COUNTRY =
      new EnumMap<>(Country.class);

  private final MarketHolidayRepository marketHolidayRepository;

  public MarketTradingCalendar(MarketHolidayRepository marketHolidayRepository) {
    this.marketHolidayRepository = marketHolidayRepository;
    if (marketHolidayRepository != null) {
      loadHolidays();
    } else {
      clearHolidays();
    }
  }

  @PostConstruct
  public void initializeHolidays() {
    if (marketHolidayRepository != null) {
      loadHolidays();
    } else {
      clearHolidays();
    }
  }

  public final void loadHolidays() {
    if (marketHolidayRepository != null) {
      List<MarketHoliday> holidayList = marketHolidayRepository.findAllByOrderByHolidayDateAsc();
      Map<Country, Set<LocalDate>> datesMap = new EnumMap<>(Country.class);
      Map<Country, Map<LocalDate, String>> namesMap = new EnumMap<>(Country.class);

      for (Country country : Country.values()) {
        datesMap.put(country, new HashSet<>());
        namesMap.put(country, new HashMap<>());
      }

      for (MarketHoliday holiday : holidayList) {
        datesMap.get(holiday.getCountry()).add(holiday.getHolidayDate());
        namesMap.get(holiday.getCountry()).put(holiday.getHolidayDate(), holiday.getName());
      }

      for (Country country : Country.values()) {
        HOLIDAYS_BY_COUNTRY.put(country, Set.copyOf(datesMap.get(country)));
        HOLIDAY_NAMES_BY_COUNTRY.put(country, Map.copyOf(namesMap.get(country)));
      }
    } else {
      clearHolidays();
    }
  }

  public static void clearHolidays() {
    for (Country country : Country.values()) {
      HOLIDAYS_BY_COUNTRY.put(country, Set.of());
      HOLIDAY_NAMES_BY_COUNTRY.put(country, Map.of());
    }
  }

  public static void setHolidays(Country country, Map<LocalDate, String> holidays) {
    if (holidays != null) {
      HOLIDAYS_BY_COUNTRY.put(country, Set.copyOf(holidays.keySet()));
      HOLIDAY_NAMES_BY_COUNTRY.put(country, Map.copyOf(holidays));
    } else {
      HOLIDAYS_BY_COUNTRY.put(country, Set.of());
      HOLIDAY_NAMES_BY_COUNTRY.put(country, Map.of());
    }
  }

  public static Set<LocalDate> getHolidays(Country country) {
    return HOLIDAYS_BY_COUNTRY.getOrDefault(country, Set.of());
  }

  public static Optional<String> getHolidayName(LocalDate date, Country country) {
    return Optional.ofNullable(HOLIDAY_NAMES_BY_COUNTRY.getOrDefault(country, Map.of()).get(date));
  }

  public static boolean isTradingDay(LocalDate date, Country country) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();
    if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
      return !getHolidays(country).contains(date);
    } else {
      return false;
    }
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
    return isTradingDay(currentMarketTime.toLocalDate(), country)
        && !currentTime.isBefore(sessionStartTime)
        && !currentTime.isAfter(sessionEndTime);
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
