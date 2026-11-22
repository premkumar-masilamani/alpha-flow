package com.alphaflow.engine.configs;

import java.time.LocalDate;

public final class YahooFinanceConfig {

  public static final String DOWNLOAD_URL =
      "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?period1={start}&period2={end}&interval=1d";
  public static final long DELAY_MILLISECONDS = 1000L;
  public static final LocalDate BACKFILL_START_DATE = LocalDate.of(1900, 1, 1);
}
