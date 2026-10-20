package com.alphaflow.engine.configs;

import java.time.ZoneId;

public final class MarketConfig {

  public static final String EXCHANGE_NSE = "NSE";
  /**
   * National Stock Exchange Futures and Options (NFO) segment code.
   * In Indian broker APIs (Angel One SmartAPI, Zerodha, etc.), "NFO" is the standard
   * exchange segment identifier for NSE derivatives.
   */
  public static final String EXCHANGE_SEGMENT_NFO = "NFO";
  public static final String TIMEZONE_KOLKATA = "Asia/Kolkata";
  public static final ZoneId IST_ZONE = ZoneId.of(TIMEZONE_KOLKATA);
  public static final String TIMEZONE_NEW_YORK = "America/New_York";
  public static final ZoneId EST_ZONE = ZoneId.of(TIMEZONE_NEW_YORK);
}
