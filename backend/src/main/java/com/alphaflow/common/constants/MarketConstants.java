package com.alphaflow.common.constants;

import java.time.ZoneId;

public final class MarketConstants {

  public static final String EXCHANGE_NSE = "NSE";
  public static final String TIMEZONE_KOLKATA = "Asia/Kolkata";
  public static final ZoneId IST_ZONE = ZoneId.of(TIMEZONE_KOLKATA);

  private MarketConstants() {}
}
