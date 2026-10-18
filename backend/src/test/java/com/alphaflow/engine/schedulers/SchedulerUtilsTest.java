package com.alphaflow.engine.schedulers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.persistence.enums.Country;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchedulerUtilsTest {

  @BeforeEach
  @AfterEach
  void cleanCalendar() {
    MarketTradingCalendar.clearHolidays();
  }

  @Test
  void testFormatDuration() {
    String formatted = SchedulerUtils.formatDuration(65432);
    assertEquals("1 m 5 s (65432 ms)", formatted);

    String zero = SchedulerUtils.formatDuration(0);
    assertEquals("0 m 0 s (0 ms)", zero);
  }

  @Test
  void testIsMarketSessionOpenDuringTradingHours() {
    // Friday at 10:00 IST
    ZonedDateTime openTime =
        ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, MarketConstants.IST_ZONE);
    boolean isOpen =
        SchedulerUtils.isMarketSessionOpen(
            openTime, LocalTime.of(9, 15), LocalTime.of(15, 30), Country.IN);
    assertTrue(isOpen);
  }

  @Test
  void testIsMarketSessionOpenBoundaryTimes() {
    // Exactly at 09:15
    ZonedDateTime startTime =
        ZonedDateTime.of(2026, 10, 16, 9, 15, 0, 0, MarketConstants.IST_ZONE);
    assertTrue(
        SchedulerUtils.isMarketSessionOpen(
            startTime, LocalTime.of(9, 15), LocalTime.of(15, 30), Country.IN));

    // Exactly at 15:30
    ZonedDateTime endTime =
        ZonedDateTime.of(2026, 10, 16, 15, 30, 0, 0, MarketConstants.IST_ZONE);
    assertTrue(
        SchedulerUtils.isMarketSessionOpen(
            endTime, LocalTime.of(9, 15), LocalTime.of(15, 30), Country.IN));
  }

  @Test
  void testIsMarketSessionOpenWhenClosed() {
    // Weekend (Saturday)
    ZonedDateTime weekend =
        ZonedDateTime.of(2026, 10, 17, 10, 0, 0, 0, MarketConstants.IST_ZONE);
    assertFalse(
        SchedulerUtils.isMarketSessionOpen(
            weekend, LocalTime.of(9, 15), LocalTime.of(15, 30), Country.IN));

    // Before session start (09:14)
    ZonedDateTime preMarket =
        ZonedDateTime.of(2026, 10, 16, 9, 14, 0, 0, MarketConstants.IST_ZONE);
    assertFalse(
        SchedulerUtils.isMarketSessionOpen(
            preMarket, LocalTime.of(9, 15), LocalTime.of(15, 30), Country.IN));

    // After session end (15:31)
    ZonedDateTime postMarket =
        ZonedDateTime.of(2026, 10, 16, 15, 31, 0, 0, MarketConstants.IST_ZONE);
    assertFalse(
        SchedulerUtils.isMarketSessionOpen(
            postMarket, LocalTime.of(9, 15), LocalTime.of(15, 30), Country.IN));
  }

  @Test
  void testParseQuoteTimestampValidIso() {
    OffsetDateTime fallback = OffsetDateTime.now();
    OffsetDateTime parsed =
        SchedulerUtils.parseQuoteTimestamp("2026-10-16T09:30:00+05:30", fallback);
    assertNotNull(parsed);
    assertEquals(2026, parsed.getYear());
    assertEquals(10, parsed.getMonthValue());
    assertEquals(16, parsed.getDayOfMonth());
    assertEquals(9, parsed.getHour());
    assertEquals(30, parsed.getMinute());
  }

  @Test
  void testParseQuoteTimestampInvalidOrBlankReturnsFallback() {
    OffsetDateTime fallback =
        OffsetDateTime.of(2026, 10, 16, 12, 0, 0, 0, MarketConstants.IST_ZONE.getRules().getOffset(java.time.Instant.now()));

    assertEquals(fallback, SchedulerUtils.parseQuoteTimestamp("16-Oct-2026 12:00:00", fallback));
    assertEquals(fallback, SchedulerUtils.parseQuoteTimestamp(null, fallback));
    assertEquals(fallback, SchedulerUtils.parseQuoteTimestamp("   ", fallback));
  }
}
