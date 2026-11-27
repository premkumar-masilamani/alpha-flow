package com.alphaflow.engine.configs;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;

public final class YahooFinanceConfig {

  public static final String DOWNLOAD_URL =
      "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?period1={start}&period2={end}&interval=1d";
  public static final long DELAY_MILLISECONDS = 1000L;
  public static final LocalDate BACKFILL_START_DATE = LocalDate.of(1900, 1, 1);
  public static final int OVERLAP_BARS = 7;
  public static final int PRICE_SCALE = 4;
  public static final BigDecimal DRIFT_THRESHOLD = new BigDecimal("0.005");
  public static final MathContext DRIFT_MATH_CONTEXT = new MathContext(12, RoundingMode.HALF_UP);
}
