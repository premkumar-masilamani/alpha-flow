package com.alphaflow.engine.configs;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "alphaflow.angelone")
@Data
public class AngelOneConfig {
  private boolean enabled = false;
  private String apiKey = "";
  private String clientCode = "";
  private String password = "";
  private String totpKey = "";
  private String loginUrl =
      "https://apiconnect.angelone.in/rest/auth/angelbroking/user/v1/loginByPassword";
  private String candleUrl =
      "https://apiconnect.angelone.in/rest/secure/angelbroking/historical/v1/getCandleData";
  private String quoteUrl =
      "https://apiconnect.angelone.in/rest/secure/angelbroking/market/v1/quote";
  private long delayMilliseconds = 1000;
  private int initialLookbackDays = 5;
  private long tokenTtlSeconds = 72000;

  public Duration getTokenTtl() {
    return Duration.ofSeconds(tokenTtlSeconds);
  }
}
