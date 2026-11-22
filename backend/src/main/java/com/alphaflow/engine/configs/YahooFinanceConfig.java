package com.alphaflow.engine.configs;

import java.time.LocalDate;
import lombok.Data;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class YahooFinanceConfig {

  public static final String DOWNLOAD_URL =
      "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?period1={start}&period2={end}&interval=1d";
  public static final long DELAY_MILLISECONDS = 1000L;
  public static final LocalDate BACKFILL_START_DATE = LocalDate.of(1900, 1, 1);

  private String downloadUrl = DOWNLOAD_URL;
  private long delayMilliseconds = DELAY_MILLISECONDS;
  private LocalDate backfillStartDate = BACKFILL_START_DATE;
}
