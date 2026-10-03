package com.alphaflow.engine.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarketTradingCalendarTest {

  private YahooFinanceConfig config;
  private MarketTradingCalendar calendar;
  private final ZoneId zoneId = ZoneId.of("America/New_York");
  private final LocalTime cutoffTime = LocalTime.of(17, 0);

  @BeforeEach
  void setUp() {
    config = new YahooFinanceConfig();
    config.setHolidays(
        List.of(
            LocalDate.of(2026, 1, 1), // New Year's Day (Thursday)
            LocalDate.of(2026, 1, 19), // Martin Luther King Jr. Day (Monday)
            LocalDate.of(2026, 4, 3) // Good Friday (Friday)
            ));
    calendar = new MarketTradingCalendar(config);
  }

  @Test
  void testIsTradingDay() {
    // Regular trading day (Wednesday)
    assertTrue(calendar.isTradingDay(LocalDate.of(2026, 10, 7)));

    // Weekend days
    assertFalse(calendar.isTradingDay(LocalDate.of(2026, 10, 10))); // Saturday
    assertFalse(calendar.isTradingDay(LocalDate.of(2026, 10, 11))); // Sunday

    // Market holidays on weekdays
    assertFalse(calendar.isTradingDay(LocalDate.of(2026, 1, 1))); // New Year's Day
    assertFalse(calendar.isTradingDay(LocalDate.of(2026, 1, 19))); // MLK Day
    assertFalse(calendar.isTradingDay(LocalDate.of(2026, 4, 3))); // Good Friday
  }

  @Test
  void testGetExpectedLatestTradingDateOnWeekdayAfterCutoff() {
    // Wednesday 17:30 EST -> expects today (Wednesday)
    ZonedDateTime now = ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(17, 30), zoneId);
    LocalDate expected = calendar.getExpectedLatestTradingDate(now, cutoffTime);
    assertEquals(LocalDate.of(2026, 10, 7), expected);
  }

  @Test
  void testGetExpectedLatestTradingDateOnWeekdayAtCutoff() {
    // Wednesday 17:00 EST -> expects today (Wednesday)
    ZonedDateTime now = ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(17, 0), zoneId);
    LocalDate expected = calendar.getExpectedLatestTradingDate(now, cutoffTime);
    assertEquals(LocalDate.of(2026, 10, 7), expected);
  }

  @Test
  void testGetExpectedLatestTradingDateOnWeekdayBeforeCutoff() {
    // Wednesday 16:59 EST -> expects yesterday (Tuesday)
    ZonedDateTime now = ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(16, 59), zoneId);
    LocalDate expected = calendar.getExpectedLatestTradingDate(now, cutoffTime);
    assertEquals(LocalDate.of(2026, 10, 6), expected);
  }

  @Test
  void testGetExpectedLatestTradingDateOnMondayBeforeCutoff() {
    // Monday 09:30 EST -> expects previous Friday
    ZonedDateTime now = ZonedDateTime.of(LocalDate.of(2026, 10, 5), LocalTime.of(9, 30), zoneId);
    LocalDate expected = calendar.getExpectedLatestTradingDate(now, cutoffTime);
    assertEquals(LocalDate.of(2026, 10, 2), expected);
  }

  @Test
  void testGetExpectedLatestTradingDateOnWeekend() {
    // Saturday 12:00 EST -> expects previous Friday
    ZonedDateTime saturday =
        ZonedDateTime.of(LocalDate.of(2026, 10, 10), LocalTime.of(12, 0), zoneId);
    assertEquals(
        LocalDate.of(2026, 10, 9), calendar.getExpectedLatestTradingDate(saturday, cutoffTime));

    // Sunday 18:00 EST -> expects previous Friday
    ZonedDateTime sunday =
        ZonedDateTime.of(LocalDate.of(2026, 10, 11), LocalTime.of(18, 0), zoneId);
    assertEquals(
        LocalDate.of(2026, 10, 9), calendar.getExpectedLatestTradingDate(sunday, cutoffTime));
  }

  @Test
  void testGetExpectedLatestTradingDateOnHoliday() {
    // MLK Day (Monday, 2026-01-19) after cutoff -> expects Friday 2026-01-16
    ZonedDateTime mlkDayEvening =
        ZonedDateTime.of(LocalDate.of(2026, 1, 19), LocalTime.of(18, 0), zoneId);
    assertEquals(
        LocalDate.of(2026, 1, 16),
        calendar.getExpectedLatestTradingDate(mlkDayEvening, cutoffTime));

    // Tuesday (2026-01-20) before cutoff -> expects Friday 2026-01-16
    ZonedDateTime tuesdayMorning =
        ZonedDateTime.of(LocalDate.of(2026, 1, 20), LocalTime.of(10, 0), zoneId);
    assertEquals(
        LocalDate.of(2026, 1, 16),
        calendar.getExpectedLatestTradingDate(tuesdayMorning, cutoffTime));
  }
}
