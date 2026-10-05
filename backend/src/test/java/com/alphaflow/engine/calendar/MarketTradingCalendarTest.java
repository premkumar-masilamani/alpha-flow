package com.alphaflow.engine.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarketTradingCalendarTest {

  private Set<MonthDay> holidays;
  private final ZoneId zoneId = ZoneId.of("America/New_York");
  private final LocalTime cutoffTime = LocalTime.of(17, 0);

  @BeforeEach
  void setUp() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    config.setHolidays(
        List.of(
            "01-01", // New Year's Day
            "01-19", // Martin Luther King Jr. Day
            "04-03" // Good Friday
            ));
    holidays = config.getHolidaySet();
  }

  @Test
  void testIsTradingDay() {
    // Regular trading day (Wednesday)
    assertTrue(MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 10, 7), holidays));

    // Weekend days
    assertFalse(
        MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 10, 10), holidays)); // Saturday
    assertFalse(MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 10, 11), holidays)); // Sunday

    // Market holidays on weekdays
    assertFalse(
        MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 1, 1), holidays)); // New Year's Day
    assertFalse(MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 1, 19), holidays)); // MLK Day
    assertFalse(
        MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 4, 3), holidays)); // Good Friday

    // Null holidays handled gracefully
    assertTrue(MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 10, 7), null));
  }

  @Test
  void testGetExpectedLatestTradingDateOnWeekdayAfterCutoff() {
    // Wednesday 17:30 EST -> expects today (Wednesday)
    ZonedDateTime now = ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(17, 30), zoneId);
    LocalDate expected =
        MarketTradingCalendar.getExpectedLatestTradingDate(now, cutoffTime, holidays);
    assertEquals(LocalDate.of(2026, 10, 7), expected);
  }

  @Test
  void testGetExpectedLatestTradingDateOnWeekdayAtCutoff() {
    // Wednesday 17:00 EST -> expects today (Wednesday)
    ZonedDateTime now = ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(17, 0), zoneId);
    LocalDate expected =
        MarketTradingCalendar.getExpectedLatestTradingDate(now, cutoffTime, holidays);
    assertEquals(LocalDate.of(2026, 10, 7), expected);
  }

  @Test
  void testGetExpectedLatestTradingDateOnWeekdayBeforeCutoff() {
    // Wednesday 16:59 EST -> expects yesterday (Tuesday)
    ZonedDateTime now = ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(16, 59), zoneId);
    LocalDate expected =
        MarketTradingCalendar.getExpectedLatestTradingDate(now, cutoffTime, holidays);
    assertEquals(LocalDate.of(2026, 10, 6), expected);
  }

  @Test
  void testGetExpectedLatestTradingDateOnMondayBeforeCutoff() {
    // Monday 09:30 EST -> expects previous Friday
    ZonedDateTime now = ZonedDateTime.of(LocalDate.of(2026, 10, 5), LocalTime.of(9, 30), zoneId);
    LocalDate expected =
        MarketTradingCalendar.getExpectedLatestTradingDate(now, cutoffTime, holidays);
    assertEquals(LocalDate.of(2026, 10, 2), expected);
  }

  @Test
  void testGetExpectedLatestTradingDateOnWeekend() {
    // Saturday 12:00 EST -> expects previous Friday
    ZonedDateTime saturday =
        ZonedDateTime.of(LocalDate.of(2026, 10, 10), LocalTime.of(12, 0), zoneId);
    assertEquals(
        LocalDate.of(2026, 10, 9),
        MarketTradingCalendar.getExpectedLatestTradingDate(saturday, cutoffTime, holidays));

    // Sunday 18:00 EST -> expects previous Friday
    ZonedDateTime sunday =
        ZonedDateTime.of(LocalDate.of(2026, 10, 11), LocalTime.of(18, 0), zoneId);
    assertEquals(
        LocalDate.of(2026, 10, 9),
        MarketTradingCalendar.getExpectedLatestTradingDate(sunday, cutoffTime, holidays));
  }

  @Test
  void testGetExpectedLatestTradingDateOnHoliday() {
    // MLK Day (Monday, 2026-01-19) after cutoff -> expects Friday 2026-01-16
    ZonedDateTime mlkDayEvening =
        ZonedDateTime.of(LocalDate.of(2026, 1, 19), LocalTime.of(18, 0), zoneId);
    assertEquals(
        LocalDate.of(2026, 1, 16),
        MarketTradingCalendar.getExpectedLatestTradingDate(mlkDayEvening, cutoffTime, holidays));

    // Tuesday (2026-01-20) before cutoff -> expects Friday 2026-01-16
    ZonedDateTime tuesdayMorning =
        ZonedDateTime.of(LocalDate.of(2026, 1, 20), LocalTime.of(10, 0), zoneId);
    assertEquals(
        LocalDate.of(2026, 1, 16),
        MarketTradingCalendar.getExpectedLatestTradingDate(tuesdayMorning, cutoffTime, holidays));
  }
}
