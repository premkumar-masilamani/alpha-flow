package com.alphaflow.engine.calculators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.common.enums.Timeframe;
import java.time.LocalDate;
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
}
