package com.alphaflow.engine.configs;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "alphaflow.angelone")
@Data
public class AngelOneConfig {

  private String apiKey;
  private String clientCode;
  private String password;
  private String totpKey;

  private String loginUrl =
      "https://apiconnect.angelone.in/rest/auth/angelbroking/user/v1/loginByPassword";
  private String candleUrl =
      "https://apiconnect.angelone.in/rest/secure/angelbroking/historical/v1/getCandleData";
  private String quoteUrl =
      "https://apiconnect.angelone.in/rest/secure/angelbroking/market/v1/quote";
  /**
   * SmartAPI OpenAPIScripMaster JSON endpoint published daily by Angel One.
   * Angel One publishes a single master instrument dump across all segments;
   * symbol-specific endpoints (e.g. NIFTY-only) are not provided by SmartAPI.
   * {@code AngelOneScripMasterManager} downloads this file once per trading day and
   * filters/caches NIFTY index options into an in-memory index for fast lookup.
   */
  private String scripMasterUrl =
      "https://margincalculator.angelbroking.com/OpenAPI_File/files/OpenAPIScripMaster.json";

  private long delayMilliseconds = 1000;
  private Duration tokenTtl = Duration.ofSeconds(72000);
  private LocalTime sessionStartTime = LocalTime.of(9, 15);
  private LocalTime sessionEndTime = LocalTime.of(15, 30);
  private LocalDate backfillStartDate = LocalDate.of(2015, 1, 1);
}
