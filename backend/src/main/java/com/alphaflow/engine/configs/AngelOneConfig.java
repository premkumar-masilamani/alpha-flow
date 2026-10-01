package com.alphaflow.engine.configs;

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
  private String baseUrl = "https://apiconnect.angelone.in";
  private long delayMilliseconds = 1000;
  private int initialLookbackDays = 5;
  private String clientLocalIp = "";
  private String clientPublicIp = "";
  private String macAddress = "";
}
