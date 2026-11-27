package com.alphaflow.engine.configs;

import com.alphaflow.common.enums.Timeframe;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import lombok.extern.slf4j.Slf4j;

/**
 * Central configuration constants governing Dow-ATR swing detection, classical chart pattern
 * geometric tolerances, and persisted snapshot retention windows.
 */
@Slf4j
public final class ChartPatternConfig {

  /** Wilder smoothing period used to compute the rolling Average True Range (ATR) series. */
  public static final int ATR_PERIOD = 14;

  /** Minimum number of price bars required to compute Wilder ATR and detect structural swings. */
  public static final int MIN_BARS = 15;

  /**
   * Rolling ATR multiplier applied to Close-based retracements to confirm a Dow Theory wave
   * reversal.
   */
  public static final BigDecimal SWING_ATR_MULTIPLIER = new BigDecimal("2.0");

  /** Precision context used for intermediate chart pattern ratio and target calculations. */
  public static final MathContext MATH_CONTEXT = new MathContext(18, RoundingMode.HALF_UP);

  /** Decimal scale for persisted chart pattern price levels. */
  public static final int PRICE_SCALE = 4;

  /** Constant divisor of 2 for midpoint and half-depth evaluations. */
  public static final BigDecimal TWO = new BigDecimal("2");

  /** Maximum relative price spread (3.5%) for Double Top/Bottom and Triangle flat boundaries. */
  public static final BigDecimal TOLERANCE_3_PERCENT = new BigDecimal("0.035");

  /** Maximum relative price spread (4.0%) for Triple Top/Bottom and Cup &amp; Handle rims. */
  public static final BigDecimal TOLERANCE_4_PERCENT = new BigDecimal("0.040");

  /** Maximum relative price spread (5.0%) between Head &amp; Shoulders left and right shoulders. */
  public static final BigDecimal TOLERANCE_5_PERCENT = new BigDecimal("0.050");

  /** Minimum pattern depth ratio (1.5%) for Double and Triple formations. */
  public static final BigDecimal MIN_DEPTH_RATIO = new BigDecimal("0.015");

  /** Minimum cup depth ratio (4.0%) for Cup &amp; Handle formations. */
  public static final BigDecimal CUP_MIN_DEPTH_RATIO = new BigDecimal("0.04");

  /** Retention window in calendar days for persisted daily chart pattern snapshots. */
  public static final int DAILY_RETENTION_DAYS = 30;

  /** Retention window in calendar days (12 weeks) for persisted weekly chart pattern snapshots. */
  public static final int WEEKLY_RETENTION_DAYS = 84;

  /**
   * Resolves the snapshot retention window in days for the given timeframe.
   *
   * @param timeframe target market timeframe
   * @return number of calendar days to retain historical chart pattern snapshots
   * @throws IllegalArgumentException if {@code timeframe} is null or unsupported
   */
  public static int getRetentionDaysForTimeframe(Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return DAILY_RETENTION_DAYS;
    } else if (timeframe == Timeframe.WEEKLY) {
      return WEEKLY_RETENTION_DAYS;
    } else {
      log.error("Unsupported timeframe for chart pattern retention lookup: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }
}
