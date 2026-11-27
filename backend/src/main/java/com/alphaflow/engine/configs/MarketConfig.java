package com.alphaflow.engine.configs;

import java.time.LocalTime;
import java.time.ZoneId;

public final class MarketConfig {

  public static final String TIMEZONE_KOLKATA = "Asia/Kolkata";
  public static final ZoneId IST_ZONE = ZoneId.of(TIMEZONE_KOLKATA);
  public static final String TIMEZONE_NEW_YORK = "America/New_York";
  public static final ZoneId EST_ZONE = ZoneId.of(TIMEZONE_NEW_YORK);

  public static final LocalTime IN_MARKET_CLOSE_TIME = LocalTime.of(15, 30);
  public static final LocalTime US_MARKET_CLOSE_TIME = LocalTime.of(16, 0);

  private MarketConfig() {}
}
