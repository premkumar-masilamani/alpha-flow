package com.alphaflow.common.constants;

import java.time.ZoneId;

public final class MarketConstants {

  public static final String EXCHANGE_NSE = "NSE";
  public static final String TIMEZONE_KOLKATA = "Asia/Kolkata";
  public static final ZoneId IST_ZONE = ZoneId.of(TIMEZONE_KOLKATA);
  public static final String TIMEZONE_NEW_YORK = "America/New_York";
  public static final ZoneId EST_ZONE = ZoneId.of(TIMEZONE_NEW_YORK);

  private MarketConstants() {}
}
