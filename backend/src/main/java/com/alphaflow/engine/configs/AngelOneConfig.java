package com.alphaflow.engine.configs;

import jakarta.annotation.PostConstruct;
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

  @PostConstruct
  public void validate() {
    validateRequiredProperty("apiKey", apiKey, "ANGEL_ONE_API_KEY");
    validateRequiredProperty("clientCode", clientCode, "ANGEL_ONE_CLIENT_CODE");
    validateRequiredProperty("password", password, "ANGEL_ONE_PASSWORD");
    validateRequiredProperty("totpKey", totpKey, "ANGEL_ONE_TOTP_KEY");
  }

  private void validateRequiredProperty(String propertyName, String value, String envVarName) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          "Required Angel One configuration property 'alphaflow.angelone."
              + propertyName
              + "' is missing or empty. Please set the "
              + envVarName
              + " environment variable in your .env file.");
    }
  }

  public Duration getTokenTtl() {
    return Duration.ofSeconds(tokenTtlSeconds);
  }
}
