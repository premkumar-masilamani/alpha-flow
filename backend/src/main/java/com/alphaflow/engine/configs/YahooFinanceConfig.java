package com.alphaflow.engine.configs;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "alphaflow.yahoo")
@Data
public class YahooFinanceConfig {
  private String downloadUrl;
  private long delayMilliseconds;
  private String marketTimezone = "America/New_York";
  private LocalTime marketCutoffTime = LocalTime.of(17, 0);
  private List<LocalDate> holidays = new ArrayList<>();

  public Set<LocalDate> getHolidaySet() {
    if (holidays != null) {
      return new HashSet<>(holidays);
    } else {
      return Set.of();
    }
  }
}
