package com.alphaflow.engine.configs;

import java.math.BigDecimal;

/**
 * Central configuration constants governing Dow-ATR swing detection, support/resistance zone
 * sizing, and historical lifecycle validation.
 */
public final class SupportResistanceConfig {

  /** Wilder smoothing period used to compute the rolling Average True Range (ATR) series. */
  public static final int ATR_PERIOD = 14;

  /**
   * Rolling ATR multiplier applied to Close-based retracements to confirm a Dow Theory wave
   * reversal.
   */
  public static final BigDecimal SWING_ATR_MULTIPLIER = new BigDecimal("2.0");

  /** Latest-bar ATR multiplier defining the total vertical thickness of each S&amp;R zone band. */
  public static final BigDecimal ZONE_ATR_MULTIPLIER = new BigDecimal("1.0");

  /**
   * Per-bar ATR multiplier beyond a zone boundary required for a Close to count as a decisive
   * structural breach rather than a shallow mild breach.
   */
  public static final BigDecimal BREACH_ATR_MULTIPLIER = new BigDecimal("1.0");

  /**
   * Maximum number of decisive polarity flips (support to resistance or vice versa) a zone may
   * undergo before being invalidated and reset.
   */
  public static final int MAX_ALLOWED_FLIPS = 1;

  /**
   * Maximum number of shallow false breakouts (mild breach followed by a reclaim) forgiven before a
   * whipsawed zone is invalidated and reset.
   */
  public static final int MAX_FALSE_BREAKOUTS = 1;

  /** Minimum number of surviving confirmed swing points required for a zone to be valid. */
  public static final int MIN_TOUCH_COUNT = 2;
}
