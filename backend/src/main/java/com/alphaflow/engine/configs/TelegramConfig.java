package com.alphaflow.engine.configs;

import jakarta.annotation.PostConstruct;
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

  @PostConstruct
  public void validate() {
    if (apiToken == null || apiToken.isBlank() || apiToken.startsWith("${")) {
      throw new IllegalStateException(
          "Missing required configuration: alphaflow.telegram.api-token (TELEGRAM_API_TOKEN environment variable must be set)");
    }
    if (chatId == null || chatId.isBlank() || chatId.startsWith("${")) {
      throw new IllegalStateException(
          "Missing required configuration: alphaflow.telegram.chat-id (TELEGRAM_CHAT_ID environment variable must be set)");
    }
  }
}
