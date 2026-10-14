package com.alphaflow.engine.configs;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "alphaflow.market.holidays")
@Data
public class MarketConfig {

  private List<String> us;
  private List<String> in;
}
