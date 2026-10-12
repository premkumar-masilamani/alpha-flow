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

  @Test
  void testGetPreviousTradingDay() {
    // Wednesday -> Tuesday
    assertEquals(
        LocalDate.of(2026, 10, 6),
        MarketTradingCalendar.getPreviousTradingDay(LocalDate.of(2026, 10, 7), holidays));

    // Monday -> Friday
    assertEquals(
        LocalDate.of(2026, 10, 2),
        MarketTradingCalendar.getPreviousTradingDay(LocalDate.of(2026, 10, 5), holidays));

    // Sunday -> Friday
    assertEquals(
        LocalDate.of(2026, 10, 9),
        MarketTradingCalendar.getPreviousTradingDay(LocalDate.of(2026, 10, 11), holidays));

    // Saturday -> Friday
    assertEquals(
        LocalDate.of(2026, 10, 9),
        MarketTradingCalendar.getPreviousTradingDay(LocalDate.of(2026, 10, 10), holidays));

    // Day after holiday (Tuesday 2026-01-20 after MLK Day Monday 2026-01-19) -> Friday 2026-01-16
    assertEquals(
        LocalDate.of(2026, 1, 16),
        MarketTradingCalendar.getPreviousTradingDay(LocalDate.of(2026, 1, 20), holidays));
  }

  @Test
  void testGetExpectedLatestCompletedCandleTimeDuringTradingHours() {
    LocalTime sessionStart = LocalTime.of(9, 15);
    LocalTime sessionEnd = LocalTime.of(15, 30);
    int candleMinutes = 15;
    ZoneId istZone = ZoneId.of("Asia/Kolkata");

    // Wednesday 09:30:00 -> first candle (09:15) completed
    ZonedDateTime nowAt0930 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(9, 30), istZone);
    assertEquals(
        nowAt0930.toLocalDate().atTime(9, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt0930, sessionStart, sessionEnd, candleMinutes, holidays));

    // Wednesday 09:44:59 -> first candle (09:15) completed
    ZonedDateTime nowAt0944 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(9, 44, 59), istZone);
    assertEquals(
        nowAt0944.toLocalDate().atTime(9, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt0944, sessionStart, sessionEnd, candleMinutes, holidays));

    // Wednesday 09:45:00 -> second candle (09:30) completed
    ZonedDateTime nowAt0945 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(9, 45), istZone);
    assertEquals(
        nowAt0945.toLocalDate().atTime(9, 30).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt0945, sessionStart, sessionEnd, candleMinutes, holidays));

    // Wednesday 10:20:00 -> 4 completed candles (09:15, 09:30, 09:45, 10:00) -> 10:00
    ZonedDateTime nowAt1020 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(10, 20), istZone);
    assertEquals(
        nowAt1020.toLocalDate().atTime(10, 0).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt1020, sessionStart, sessionEnd, candleMinutes, holidays));

    // Wednesday 15:29:59 -> 24 completed candles -> 15:00
    ZonedDateTime nowAt1529 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(15, 29, 59), istZone);
    assertEquals(
        nowAt1529.toLocalDate().atTime(15, 0).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt1529, sessionStart, sessionEnd, candleMinutes, holidays));
  }

  @Test
  void testGetExpectedLatestCompletedCandleTimeAtAndAfterMarketClose() {
    LocalTime sessionStart = LocalTime.of(9, 15);
    LocalTime sessionEnd = LocalTime.of(15, 30);
    int candleMinutes = 15;
    ZoneId istZone = ZoneId.of("Asia/Kolkata");

    // Wednesday 15:30:00 (exact session end) -> full day completed -> 15:15 candle
    ZonedDateTime nowAt1530 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(15, 30), istZone);
    assertEquals(
        nowAt1530.toLocalDate().atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt1530, sessionStart, sessionEnd, candleMinutes, holidays));

    // Wednesday 18:00:00 (evening) -> full day completed -> 15:15 candle of today
    ZonedDateTime nowAt1800 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(18, 0), istZone);
    assertEquals(
        nowAt1800.toLocalDate().atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt1800, sessionStart, sessionEnd, candleMinutes, holidays));
  }

  @Test
  void testGetExpectedLatestCompletedCandleTimeBeforeFirstCandleAndOffMarket() {
    LocalTime sessionStart = LocalTime.of(9, 15);
    LocalTime sessionEnd = LocalTime.of(15, 30);
    int candleMinutes = 15;
    ZoneId istZone = ZoneId.of("Asia/Kolkata");

    // Wednesday 09:29:59 (before first candle closes at 09:30) -> expects Tuesday 15:15
    ZonedDateTime nowAt0929 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(9, 29, 59), istZone);
    assertEquals(
        LocalDate.of(2026, 10, 6).atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt0929, sessionStart, sessionEnd, candleMinutes, holidays));

    // Wednesday 08:00:00 (morning before open) -> expects Tuesday 15:15
    ZonedDateTime nowAt0800 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(8, 0), istZone);
    assertEquals(
        LocalDate.of(2026, 10, 6).atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt0800, sessionStart, sessionEnd, candleMinutes, holidays));

    // Monday 08:30:00 -> expects Friday 15:15
    ZonedDateTime mondayMorning =
        ZonedDateTime.of(LocalDate.of(2026, 10, 5), LocalTime.of(8, 30), istZone);
    assertEquals(
        LocalDate.of(2026, 10, 2).atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            mondayMorning, sessionStart, sessionEnd, candleMinutes, holidays));

    // Saturday 12:00:00 -> expects Friday 15:15
    ZonedDateTime saturdayNoon =
        ZonedDateTime.of(LocalDate.of(2026, 10, 10), LocalTime.of(12, 0), istZone);
    assertEquals(
        LocalDate.of(2026, 10, 9).atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            saturdayNoon, sessionStart, sessionEnd, candleMinutes, holidays));

    // Sunday 18:00:00 -> expects Friday 15:15
    ZonedDateTime sundayEvening =
        ZonedDateTime.of(LocalDate.of(2026, 10, 11), LocalTime.of(18, 0), istZone);
    assertEquals(
        LocalDate.of(2026, 10, 9).atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            sundayEvening, sessionStart, sessionEnd, candleMinutes, holidays));

    // Market holiday (e.g. MLK Day 2026-01-19) at 11:00 -> expects Friday 2026-01-16 15:15
    ZonedDateTime holidayAt11 =
        ZonedDateTime.of(LocalDate.of(2026, 1, 19), LocalTime.of(11, 0), istZone);
    assertEquals(
        LocalDate.of(2026, 1, 16).atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            holidayAt11, sessionStart, sessionEnd, candleMinutes, holidays));
  }
}
