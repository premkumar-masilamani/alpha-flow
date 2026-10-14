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
  private String loginUrl;
  private String candleUrl;
  private String quoteUrl;
  private long delayMilliseconds;
  private int initialLookbackDays;
  private long tokenTtlSeconds;
  private LocalTime sessionStartTime = LocalTime.of(9, 15);
  private LocalTime sessionEndTime = LocalTime.of(15, 30);
  private LocalDate backfillStartDate = LocalDate.of(2015, 1, 1);
  private int maxChunkDays = 200;

  public Duration getTokenTtl() {
    return Duration.ofSeconds(tokenTtlSeconds);
  }
}
