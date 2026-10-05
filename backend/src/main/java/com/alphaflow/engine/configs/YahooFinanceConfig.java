package com.alphaflow.engine.configs;

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
@ConfigurationProperties(prefix = "alphaflow.yahoo")
@Data
public class YahooFinanceConfig {

  private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

  private String downloadUrl;
  private long delayMilliseconds;
  private LocalTime marketCutoffTime = LocalTime.of(17, 0);
  private List<String> holidays = new ArrayList<>();

  public Set<MonthDay> getHolidaySet() {
    if (holidays != null) {
      return holidays.stream()
          .map(holiday -> MonthDay.parse(holiday.trim(), MONTH_DAY_FORMATTER))
          .collect(Collectors.toSet());
    } else {
      return Set.of();
    }
  }
}
