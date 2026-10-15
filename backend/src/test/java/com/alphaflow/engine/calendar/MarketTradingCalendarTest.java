package com.alphaflow.engine.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.persistence.entities.MarketHoliday;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.repositories.MarketHolidayRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarketTradingCalendarTest {

  private final ZoneId usZone = ZoneId.of("America/New_York");
  private final ZoneId istZone = ZoneId.of("Asia/Kolkata");
  private final LocalTime cutoffTime = LocalTime.of(17, 0);

  private MarketHolidayRepository marketHolidayRepository;

  @BeforeEach
  void setUp() {
    marketHolidayRepository = mock(MarketHolidayRepository.class);

    List<MarketHoliday> holidays =
        List.of(
            MarketHoliday.builder()
                .country(Country.US)
                .holidayDate(LocalDate.of(2026, 1, 1))
                .name("New Year's Day")
                .build(),
            MarketHoliday.builder()
                .country(Country.US)
                .holidayDate(LocalDate.of(2026, 1, 19))
                .name("Martin Luther King, Jr. Day")
                .build(),
            MarketHoliday.builder()
                .country(Country.US)
                .holidayDate(LocalDate.of(2026, 4, 3))
                .name("Good Friday")
                .build(),
            MarketHoliday.builder()
                .country(Country.US)
                .holidayDate(LocalDate.of(2027, 1, 18))
                .name("Martin Luther King, Jr. Day")
                .build(),
            MarketHoliday.builder()
                .country(Country.US)
                .holidayDate(LocalDate.of(2028, 1, 17))
                .name("Martin Luther King, Jr. Day")
                .build(),
            MarketHoliday.builder()
                .country(Country.IN)
                .holidayDate(LocalDate.of(2026, 1, 26))
                .name("Republic Day")
                .build(),
            MarketHoliday.builder()
                .country(Country.IN)
                .holidayDate(LocalDate.of(2026, 8, 15))
                .name("Independence Day")
                .build(),
            MarketHoliday.builder()
                .country(Country.IN)
                .holidayDate(LocalDate.of(2026, 10, 2))
                .name("Mahatma Gandhi Jayanti")
                .build());

    when(marketHolidayRepository.findAllByOrderByHolidayDateAsc()).thenReturn(holidays);

    MarketTradingCalendar calendar = new MarketTradingCalendar(marketHolidayRepository);
    calendar.initializeHolidays();
  }

  @Test
  void testNullMarketConfig() {
    MarketTradingCalendar calendar = new MarketTradingCalendar(null);
    calendar.initializeHolidays();
    assertTrue(MarketTradingCalendar.getHolidays(Country.US).isEmpty());
    assertTrue(MarketTradingCalendar.getHolidays(Country.IN).isEmpty());
    setUp();
  }

  @Test
  void testHolidaysPerCountryConfiguration() {
    assertEquals(
        Set.of(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 19),
            LocalDate.of(2026, 4, 3),
            LocalDate.of(2027, 1, 18),
            LocalDate.of(2028, 1, 17)),
        MarketTradingCalendar.getHolidays(Country.US));
    assertEquals(
        Set.of(LocalDate.of(2026, 1, 26), LocalDate.of(2026, 8, 15), LocalDate.of(2026, 10, 2)),
        MarketTradingCalendar.getHolidays(Country.IN));

    assertEquals(
        Optional.of("Martin Luther King, Jr. Day"),
        MarketTradingCalendar.getHolidayName(LocalDate.of(2026, 1, 19), Country.US));
    assertEquals(
        Optional.empty(),
        MarketTradingCalendar.getHolidayName(LocalDate.of(2026, 1, 20), Country.US));

    // Null holidays map resets country to empty set
    MarketTradingCalendar.setHolidays(Country.US, null);
    assertTrue(MarketTradingCalendar.getHolidays(Country.US).isEmpty());

    // Explicit setHolidays restores mapping
    MarketTradingCalendar.setHolidays(
        Country.US,
        Map.of(
            LocalDate.of(2026, 1, 19), "Martin Luther King, Jr. Day",
            LocalDate.of(2026, 4, 3), "Good Friday"));
    assertEquals(2, MarketTradingCalendar.getHolidays(Country.US).size());
    assertEquals(
        Optional.of("Good Friday"),
        MarketTradingCalendar.getHolidayName(LocalDate.of(2026, 4, 3), Country.US));
  }

  @Test
  void testMultiYearHolidayDetection() {
    // 2026 MLK Day
    assertFalse(MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 1, 19), Country.US));
    // 2027 MLK Day
    assertFalse(MarketTradingCalendar.isTradingDay(LocalDate.of(2027, 1, 18), Country.US));
    // 2028 MLK Day
    assertFalse(MarketTradingCalendar.isTradingDay(LocalDate.of(2028, 1, 17), Country.US));
    // Day after 2026 MLK Day is a trading day
    assertTrue(MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 1, 20), Country.US));
  }

  @Test
  void testIsTradingDayDifferentForUsAndIndia() {
    // Regular trading day (Wednesday) for both US and India
    assertTrue(MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 10, 7), Country.US));
    assertTrue(MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 10, 7), Country.IN));

    // Weekend days
    assertFalse(
        MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 10, 10), Country.US)); // Saturday
    assertFalse(
        MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 10, 11), Country.IN)); // Sunday

    // US holiday (MLK Day 2026-01-19) is NOT a trading day in US, but IS a trading day in India
    assertFalse(MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 1, 19), Country.US));
    assertTrue(MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 1, 19), Country.IN));

    // India holiday (Republic Day 2026-01-26) is NOT a trading day in India, but IS a trading day
    // in US
    assertFalse(MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 1, 26), Country.IN));
    assertTrue(MarketTradingCalendar.isTradingDay(LocalDate.of(2026, 1, 26), Country.US));
  }

  @Test
  void testGetExpectedLatestTradingDateOnWeekdayAfterCutoff() {
    // Wednesday 17:30 EST -> expects today (Wednesday)
    ZonedDateTime now = ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(17, 30), usZone);
    LocalDate expected =
        MarketTradingCalendar.getExpectedLatestTradingDate(now, cutoffTime, Country.US);
    assertEquals(LocalDate.of(2026, 10, 7), expected);
  }

  @Test
  void testGetExpectedLatestTradingDateOnWeekdayAtCutoff() {
    // Wednesday 17:00 EST -> expects today (Wednesday)
    ZonedDateTime now = ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(17, 0), usZone);
    LocalDate expected =
        MarketTradingCalendar.getExpectedLatestTradingDate(now, cutoffTime, Country.US);
    assertEquals(LocalDate.of(2026, 10, 7), expected);
  }

  @Test
  void testGetExpectedLatestTradingDateOnWeekdayBeforeCutoff() {
    // Wednesday 16:59 EST -> expects yesterday (Tuesday)
    ZonedDateTime now = ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(16, 59), usZone);
    LocalDate expected =
        MarketTradingCalendar.getExpectedLatestTradingDate(now, cutoffTime, Country.US);
    assertEquals(LocalDate.of(2026, 10, 6), expected);
  }

  @Test
  void testGetExpectedLatestTradingDateOnMondayBeforeCutoff() {
    // Monday 09:30 EST -> expects previous Friday
    ZonedDateTime now = ZonedDateTime.of(LocalDate.of(2026, 10, 5), LocalTime.of(9, 30), usZone);
    LocalDate expected =
        MarketTradingCalendar.getExpectedLatestTradingDate(now, cutoffTime, Country.US);
    assertEquals(LocalDate.of(2026, 10, 2), expected);
  }

  @Test
  void testGetExpectedLatestTradingDateOnWeekend() {
    // Saturday 12:00 EST -> expects previous Friday
    ZonedDateTime saturday =
        ZonedDateTime.of(LocalDate.of(2026, 10, 10), LocalTime.of(12, 0), usZone);
    assertEquals(
        LocalDate.of(2026, 10, 9),
        MarketTradingCalendar.getExpectedLatestTradingDate(saturday, cutoffTime, Country.US));

    // Sunday 18:00 EST -> expects previous Friday
    ZonedDateTime sunday =
        ZonedDateTime.of(LocalDate.of(2026, 10, 11), LocalTime.of(18, 0), usZone);
    assertEquals(
        LocalDate.of(2026, 10, 9),
        MarketTradingCalendar.getExpectedLatestTradingDate(sunday, cutoffTime, Country.US));
  }

  @Test
  void testGetExpectedLatestTradingDateOnHoliday() {
    // MLK Day (Monday, 2026-01-19) after cutoff -> expects Friday 2026-01-16 for US
    ZonedDateTime mlkDayEvening =
        ZonedDateTime.of(LocalDate.of(2026, 1, 19), LocalTime.of(18, 0), usZone);
    assertEquals(
        LocalDate.of(2026, 1, 16),
        MarketTradingCalendar.getExpectedLatestTradingDate(mlkDayEvening, cutoffTime, Country.US));

    // Tuesday (2026-01-20) before cutoff -> expects Friday 2026-01-16 for US
    ZonedDateTime tuesdayMorning =
        ZonedDateTime.of(LocalDate.of(2026, 1, 20), LocalTime.of(10, 0), usZone);
    assertEquals(
        LocalDate.of(2026, 1, 16),
        MarketTradingCalendar.getExpectedLatestTradingDate(tuesdayMorning, cutoffTime, Country.US));

    // Gandhi Jayanti (Friday, 2026-10-02) after cutoff -> expects Thursday 2026-10-01 for India
    ZonedDateTime gandhiJayantiEvening =
        ZonedDateTime.of(LocalDate.of(2026, 10, 2), LocalTime.of(18, 0), istZone);
    assertEquals(
        LocalDate.of(2026, 10, 1),
        MarketTradingCalendar.getExpectedLatestTradingDate(
            gandhiJayantiEvening, cutoffTime, Country.IN));
  }

  @Test
  void testGetPreviousTradingDay() {
    // Wednesday -> Tuesday
    assertEquals(
        LocalDate.of(2026, 10, 6),
        MarketTradingCalendar.getPreviousTradingDay(LocalDate.of(2026, 10, 7), Country.US));

    // Monday -> Friday
    assertEquals(
        LocalDate.of(2026, 10, 2),
        MarketTradingCalendar.getPreviousTradingDay(LocalDate.of(2026, 10, 5), Country.US));

    // Sunday -> Friday
    assertEquals(
        LocalDate.of(2026, 10, 9),
        MarketTradingCalendar.getPreviousTradingDay(LocalDate.of(2026, 10, 11), Country.US));

    // Saturday -> Friday
    assertEquals(
        LocalDate.of(2026, 10, 9),
        MarketTradingCalendar.getPreviousTradingDay(LocalDate.of(2026, 10, 10), Country.US));

    // Day after US holiday (Tuesday 2026-01-20 after MLK Day Monday 2026-01-19) -> Friday
    // 2026-01-16
    assertEquals(
        LocalDate.of(2026, 1, 16),
        MarketTradingCalendar.getPreviousTradingDay(LocalDate.of(2026, 1, 20), Country.US));

    // Day after India holiday (Tuesday 2026-01-27 after Republic Day Monday 2026-01-26) -> Friday
    // 2026-01-23
    assertEquals(
        LocalDate.of(2026, 1, 23),
        MarketTradingCalendar.getPreviousTradingDay(LocalDate.of(2026, 1, 27), Country.IN));
  }

  @Test
  void testGetExpectedLatestCompletedCandleTimeDuringTradingHours() {
    LocalTime sessionStart = LocalTime.of(9, 15);
    LocalTime sessionEnd = LocalTime.of(15, 30);
    int candleMinutes = 15;

    // Wednesday 09:30:00 -> first candle (09:15) completed
    ZonedDateTime nowAt0930 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(9, 30), istZone);
    assertEquals(
        nowAt0930.toLocalDate().atTime(9, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt0930, sessionStart, sessionEnd, candleMinutes, Country.IN));

    // Wednesday 09:44:59 -> first candle (09:15) completed
    ZonedDateTime nowAt0944 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(9, 44, 59), istZone);
    assertEquals(
        nowAt0944.toLocalDate().atTime(9, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt0944, sessionStart, sessionEnd, candleMinutes, Country.IN));

    // Wednesday 09:45:00 -> second candle (09:30) completed
    ZonedDateTime nowAt0945 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(9, 45), istZone);
    assertEquals(
        nowAt0945.toLocalDate().atTime(9, 30).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt0945, sessionStart, sessionEnd, candleMinutes, Country.IN));

    // Wednesday 10:20:00 -> 4 completed candles (09:15, 09:30, 09:45, 10:00) -> 10:00
    ZonedDateTime nowAt1020 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(10, 20), istZone);
    assertEquals(
        nowAt1020.toLocalDate().atTime(10, 0).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt1020, sessionStart, sessionEnd, candleMinutes, Country.IN));

    // Wednesday 15:29:59 -> 24 completed candles -> 15:00
    ZonedDateTime nowAt1529 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(15, 29, 59), istZone);
    assertEquals(
        nowAt1529.toLocalDate().atTime(15, 0).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt1529, sessionStart, sessionEnd, candleMinutes, Country.IN));
  }

  @Test
  void testGetExpectedLatestCompletedCandleTimeAtAndAfterMarketClose() {
    LocalTime sessionStart = LocalTime.of(9, 15);
    LocalTime sessionEnd = LocalTime.of(15, 30);
    int candleMinutes = 15;

    // Wednesday 15:30:00 (exact session end) -> full day completed -> 15:15 candle
    ZonedDateTime nowAt1530 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(15, 30), istZone);
    assertEquals(
        nowAt1530.toLocalDate().atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt1530, sessionStart, sessionEnd, candleMinutes, Country.IN));

    // Wednesday 18:00:00 (evening) -> full day completed -> 15:15 candle of today
    ZonedDateTime nowAt1800 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(18, 0), istZone);
    assertEquals(
        nowAt1800.toLocalDate().atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt1800, sessionStart, sessionEnd, candleMinutes, Country.IN));
  }

  @Test
  void testGetExpectedLatestCompletedCandleTimeBeforeFirstCandleAndOffMarket() {
    LocalTime sessionStart = LocalTime.of(9, 15);
    LocalTime sessionEnd = LocalTime.of(15, 30);
    int candleMinutes = 15;

    // Wednesday 09:29:59 (before first candle closes at 09:30) -> expects Tuesday 15:15
    ZonedDateTime nowAt0929 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(9, 29, 59), istZone);
    assertEquals(
        LocalDate.of(2026, 10, 6).atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt0929, sessionStart, sessionEnd, candleMinutes, Country.IN));

    // Wednesday 08:00:00 (morning before open) -> expects Tuesday 15:15
    ZonedDateTime nowAt0800 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(8, 0), istZone);
    assertEquals(
        LocalDate.of(2026, 10, 6).atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            nowAt0800, sessionStart, sessionEnd, candleMinutes, Country.IN));

    // Monday 08:30:00 -> expects Friday 15:15 (since Friday 2026-10-02 is Gandhi Jayanti holiday in
    // India, expects Thursday 2026-10-01!)
    ZonedDateTime mondayMorning =
        ZonedDateTime.of(LocalDate.of(2026, 10, 5), LocalTime.of(8, 30), istZone);
    assertEquals(
        LocalDate.of(2026, 10, 1).atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            mondayMorning, sessionStart, sessionEnd, candleMinutes, Country.IN));

    // Saturday 12:00:00 -> expects Friday 15:15
    ZonedDateTime saturdayNoon =
        ZonedDateTime.of(LocalDate.of(2026, 10, 10), LocalTime.of(12, 0), istZone);
    assertEquals(
        LocalDate.of(2026, 10, 9).atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            saturdayNoon, sessionStart, sessionEnd, candleMinutes, Country.IN));

    // Sunday 18:00:00 -> expects Friday 15:15
    ZonedDateTime sundayEvening =
        ZonedDateTime.of(LocalDate.of(2026, 10, 11), LocalTime.of(18, 0), istZone);
    assertEquals(
        LocalDate.of(2026, 10, 9).atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            sundayEvening, sessionStart, sessionEnd, candleMinutes, Country.IN));

    // India market holiday (Republic Day 2026-01-26) at 11:00 -> expects Friday 2026-01-23 15:15
    ZonedDateTime holidayAt11 =
        ZonedDateTime.of(LocalDate.of(2026, 1, 26), LocalTime.of(11, 0), istZone);
    assertEquals(
        LocalDate.of(2026, 1, 23).atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
            holidayAt11, sessionStart, sessionEnd, candleMinutes, Country.IN));
  }
}
