package com.alphaflow.engine.schedulers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.alphaflow.engine.configs.MarketConfig;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class SchedulerUtilsTest {

  @Test
  void testFormatDuration() {
    String formatted = SchedulerUtils.formatDuration(65432);
    assertEquals("1 m 5 s (65432 ms)", formatted);

    String zero = SchedulerUtils.formatDuration(0);
    assertEquals("0 m 0 s (0 ms)", zero);
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
        OffsetDateTime.of(
            2026, 10, 16, 12, 0, 0, 0, MarketConfig.IST_ZONE.getRules().getOffset(Instant.now()));

    assertEquals(fallback, SchedulerUtils.parseQuoteTimestamp("16-Oct-2026 12:00:00", fallback));
    assertEquals(fallback, SchedulerUtils.parseQuoteTimestamp(null, fallback));
    assertEquals(fallback, SchedulerUtils.parseQuoteTimestamp("   ", fallback));
  }
}
