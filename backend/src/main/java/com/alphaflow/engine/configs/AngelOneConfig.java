package com.alphaflow.engine.configs;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "alphaflow.angelone")
@Data
public class AngelOneConfig {

  private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

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
  private List<String> holidays = new ArrayList<>();

  public Duration getTokenTtl() {
    return Duration.ofSeconds(tokenTtlSeconds);
  }

  public Set<MonthDay> getHolidaySet() {
    if (holidays != null) {
      return holidays.stream()
          .map(holiday -> MonthDay.parse(holiday.trim(), MONTH_DAY_FORMATTER))
          .collect(Collectors.toSet());
    } else {
      return Set.of();
    }
  }

  public boolean hasValidCredentials() {
    return apiKey != null
        && !apiKey.isBlank()
        && clientCode != null
        && !clientCode.isBlank()
        && password != null
        && !password.isBlank()
        && totpKey != null
        && !totpKey.isBlank();
  }
}
