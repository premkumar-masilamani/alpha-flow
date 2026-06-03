package com.alphaflow.engine.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "alphaflow.yahoo")
@Data
public class YahooFinanceConfig {

  private String downloadUrl;

  private long delayMilliseconds;
}
