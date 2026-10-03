package com.alphaflow.engine.configs;

import java.time.Duration;
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
  private String loginUrl;
  private String candleUrl;
  private String quoteUrl;
  private long delayMilliseconds;
  private int initialLookbackDays;
  private long tokenTtlSeconds;

  public Duration getTokenTtl() {
    return Duration.ofSeconds(tokenTtlSeconds);
  }
}
