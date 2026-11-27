package com.alphaflow.engine.calculators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.enums.Country;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class PipelineGatingTest {

  private static final LocalDate MONDAY = LocalDate.of(2025, 1, 6);
  private static final LocalDate WEDNESDAY = LocalDate.of(2025, 1, 8);
  private static final LocalDate FRIDAY = LocalDate.of(2025, 1, 10);
  private static final LocalDate SATURDAY = LocalDate.of(2025, 1, 11);
  private static final LocalDate SUNDAY = LocalDate.of(2025, 1, 12);
  private static final LocalDate PREVIOUS_MONDAY = LocalDate.of(2024, 12, 30);

  @Test
  void testRequiresDailyUpdateBranches() {
    assertFalse(PipelineGating.requiresDailyUpdate(null, null));
    assertFalse(PipelineGating.requiresDailyUpdate(null, WEDNESDAY));
    assertTrue(PipelineGating.requiresDailyUpdate(WEDNESDAY, null));
    assertTrue(PipelineGating.requiresDailyUpdate(WEDNESDAY, MONDAY));
    assertFalse(PipelineGating.requiresDailyUpdate(WEDNESDAY, WEDNESDAY));
    assertFalse(PipelineGating.requiresDailyUpdate(MONDAY, WEDNESDAY));
  }

  @Test
  void testIsInProgressTradingWeekWeekdayVsWeekend() {
    assertTrue(PipelineGating.isInProgressTradingWeek(MONDAY, MONDAY));
    assertTrue(PipelineGating.isInProgressTradingWeek(MONDAY, WEDNESDAY));
    assertTrue(PipelineGating.isInProgressTradingWeek(MONDAY, FRIDAY));

    assertFalse(PipelineGating.isInProgressTradingWeek(PREVIOUS_MONDAY, WEDNESDAY));
    assertFalse(PipelineGating.isInProgressTradingWeek(MONDAY, SATURDAY));
    assertFalse(PipelineGating.isInProgressTradingWeek(MONDAY, SUNDAY));
  }

  @Test
  void testRequiresWeeklyPriceUpdateBranches() {
    // Null source date is always skipped
    assertFalse(PipelineGating.requiresWeeklyPriceUpdate(null, null, WEDNESDAY));
    assertFalse(PipelineGating.requiresWeeklyPriceUpdate(null, MONDAY, WEDNESDAY));

    // Null calculated date requires initial backfill even on weekend
    assertTrue(PipelineGating.requiresWeeklyPriceUpdate(FRIDAY, null, SATURDAY));

    // Latest calculated weekly Monday is behind latest daily week's Monday
    assertTrue(PipelineGating.requiresWeeklyPriceUpdate(WEDNESDAY, PREVIOUS_MONDAY, SATURDAY));

    // Mid-week weekday (Wed) refreshes Monday-keyed weekly candle even when already computed
    assertTrue(PipelineGating.requiresWeeklyPriceUpdate(WEDNESDAY, MONDAY, WEDNESDAY));

    // Weekend (Sat/Sun) skips when weekly Monday is already up to date
    assertFalse(PipelineGating.requiresWeeklyPriceUpdate(FRIDAY, MONDAY, SATURDAY));
    assertFalse(PipelineGating.requiresWeeklyPriceUpdate(FRIDAY, MONDAY, SUNDAY));

    // Historical week (not current week) skips on weekday when already computed
    assertFalse(
        PipelineGating.requiresWeeklyPriceUpdate(
            PREVIOUS_MONDAY.plusDays(4), PREVIOUS_MONDAY, WEDNESDAY));
  }

  @Test
  void testRequiresWeeklyUpdateBranches() {
    // Null source date is always skipped
    assertFalse(PipelineGating.requiresWeeklyUpdate(null, null, WEDNESDAY));
    assertFalse(PipelineGating.requiresWeeklyUpdate(null, MONDAY, WEDNESDAY));

    // Null calculated date requires initial backfill
    assertTrue(PipelineGating.requiresWeeklyUpdate(MONDAY, null, SATURDAY));

    // Calculated date behind weekly source date
    assertTrue(PipelineGating.requiresWeeklyUpdate(MONDAY, PREVIOUS_MONDAY, SATURDAY));

    // Mid-week weekday refreshes in-progress Monday row
    assertTrue(PipelineGating.requiresWeeklyUpdate(MONDAY, MONDAY, WEDNESDAY));

    // Weekend skips when up to date
    assertFalse(PipelineGating.requiresWeeklyUpdate(MONDAY, MONDAY, SATURDAY));
    assertFalse(PipelineGating.requiresWeeklyUpdate(MONDAY, MONDAY, SUNDAY));

    // Historical week skips on weekday when up to date
    assertFalse(PipelineGating.requiresWeeklyUpdate(PREVIOUS_MONDAY, PREVIOUS_MONDAY, WEDNESDAY));
  }

  @Test
  void testRequiresUpdateDispatchAndUnsupportedTimeframe() {
    assertTrue(PipelineGating.requiresUpdate(Timeframe.DAILY, WEDNESDAY, MONDAY, WEDNESDAY));
    assertFalse(PipelineGating.requiresUpdate(Timeframe.DAILY, WEDNESDAY, WEDNESDAY, WEDNESDAY));

    assertTrue(PipelineGating.requiresUpdate(Timeframe.WEEKLY, MONDAY, MONDAY, WEDNESDAY));
    assertFalse(PipelineGating.requiresUpdate(Timeframe.WEEKLY, MONDAY, MONDAY, SATURDAY));

    assertThrows(
        IllegalArgumentException.class,
        () -> PipelineGating.requiresUpdate(null, WEDNESDAY, MONDAY, WEDNESDAY));
  }

  @Test
  void testGetLatestExpectedTradingDateNullValidation() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PipelineGating.getLatestExpectedTradingDate(null, Instant.now()));
    assertThrows(
        IllegalArgumentException.class,
        () -> PipelineGating.getLatestExpectedTradingDate(Country.US, null));
  }

  @Test
  void testGetLatestExpectedTradingDateUsMarketHoursAndWeekends() {
    ZoneId usZone = Country.US.getZoneId();
    // Wednesday 10:00 AM EST (before close 16:00) -> returns Tuesday
    Instant wedBeforeClose = ZonedDateTime.of(2025, 1, 8, 10, 0, 0, 0, usZone).toInstant();
    assertEquals(
        LocalDate.of(2025, 1, 7),
        PipelineGating.getLatestExpectedTradingDate(Country.US, wedBeforeClose));

    // Wednesday 16:01 PM EST (after close 16:00) -> returns Wednesday
    Instant wedAfterClose = ZonedDateTime.of(2025, 1, 8, 16, 1, 0, 0, usZone).toInstant();
    assertEquals(
        LocalDate.of(2025, 1, 8),
        PipelineGating.getLatestExpectedTradingDate(Country.US, wedAfterClose));

    // Saturday 12:00 PM EST -> returns Friday
    Instant satMidday = ZonedDateTime.of(2025, 1, 11, 12, 0, 0, 0, usZone).toInstant();
    assertEquals(
        LocalDate.of(2025, 1, 10),
        PipelineGating.getLatestExpectedTradingDate(Country.US, satMidday));

    // Sunday 18:00 PM EST -> returns Friday
    Instant sunEvening = ZonedDateTime.of(2025, 1, 12, 18, 0, 0, 0, usZone).toInstant();
    assertEquals(
        LocalDate.of(2025, 1, 10),
        PipelineGating.getLatestExpectedTradingDate(Country.US, sunEvening));

    // Monday 08:00 AM EST (before close 16:00) -> candidate Sunday -> returns Friday
    Instant monBeforeClose = ZonedDateTime.of(2025, 1, 6, 8, 0, 0, 0, usZone).toInstant();
    assertEquals(
        LocalDate.of(2025, 1, 3),
        PipelineGating.getLatestExpectedTradingDate(Country.US, monBeforeClose));

    // Monday 16:00 PM EST (exact close) -> returns Monday
    Instant monExactClose = ZonedDateTime.of(2025, 1, 6, 16, 0, 0, 0, usZone).toInstant();
    assertEquals(
        LocalDate.of(2025, 1, 6),
        PipelineGating.getLatestExpectedTradingDate(Country.US, monExactClose));
  }

  @Test
  void testGetLatestExpectedTradingDateInMarketHours() {
    ZoneId inZone = Country.IN.getZoneId();
    // Wednesday 14:00 PM IST (before close 15:30) -> returns Tuesday
    Instant wedBeforeClose = ZonedDateTime.of(2025, 1, 8, 14, 0, 0, 0, inZone).toInstant();
    assertEquals(
        LocalDate.of(2025, 1, 7),
        PipelineGating.getLatestExpectedTradingDate(Country.IN, wedBeforeClose));

    // Wednesday 15:30 PM IST (exact close 15:30) -> returns Wednesday
    Instant wedExactClose = ZonedDateTime.of(2025, 1, 8, 15, 30, 0, 0, inZone).toInstant();
    assertEquals(
        LocalDate.of(2025, 1, 8),
        PipelineGating.getLatestExpectedTradingDate(Country.IN, wedExactClose));

    // Wednesday 19:04 PM IST (user scenario) -> returns Wednesday
    Instant wedEvening = ZonedDateTime.of(2026, 11, 25, 19, 4, 31, 0, inZone).toInstant();
    assertEquals(
        LocalDate.of(2026, 11, 25),
        PipelineGating.getLatestExpectedTradingDate(Country.IN, wedEvening));

    // Saturday in India -> returns Friday
    Instant satIn = ZonedDateTime.of(2025, 1, 11, 18, 0, 0, 0, inZone).toInstant();
    assertEquals(
        LocalDate.of(2025, 1, 10),
        PipelineGating.getLatestExpectedTradingDate(Country.IN, satIn));
  }
}
