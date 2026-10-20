package com.alphaflow.engine.configs;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class YahooFinanceConfig {

  private String downloadUrl =
      "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?period1={start}&period2={end}&interval=1d";
  private long delayMilliseconds = 1000;
  private LocalTime marketCutoffTime = LocalTime.of(17, 0);
  private LocalDate backfillStartDate = LocalDate.of(1900, 1, 1);
}
