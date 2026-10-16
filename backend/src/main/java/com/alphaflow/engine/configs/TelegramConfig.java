package com.alphaflow.engine.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "alphaflow.telegram")
@Data
public class TelegramConfig {

  private String apiToken;
  private String chatId;
  private String messageUrl =
      "https://api.telegram.org/bot{TELEGRAM_API_TOKEN}/sendMessage?chat_id={TELEGRAM_CHAT_ID}&text={MESSAGE}&parse_mode=MarkdownV2";
  private int maxMessageLength = 4096;
}
