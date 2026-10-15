package com.alphaflow.engine.services;

import com.alphaflow.common.enums.NotificationStatus;
import com.alphaflow.common.exceptions.NotificationException;
import com.alphaflow.engine.configs.TelegramConfig;
import com.alphaflow.persistence.entities.NotificationLog;
import com.alphaflow.persistence.repositories.NotificationLogRepository;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class TelegramNotificationService implements NotificationService {

  private static final int MAX_MESSAGE_LENGTH = 4096;
  private static final String CHANNEL_TELEGRAM = "TELEGRAM";

  private final TelegramConfig telegramConfig;
  private final NotificationLogRepository notificationLogRepository;
  private final RestClient restClient;

  @Autowired
  public TelegramNotificationService(
      TelegramConfig telegramConfig,
      NotificationLogRepository notificationLogRepository,
      @Autowired(required = false) RestClient.Builder restClientBuilder) {
    this.telegramConfig = telegramConfig;
    this.notificationLogRepository = notificationLogRepository;
    if (restClientBuilder != null) {
      this.restClient = restClientBuilder.build();
    } else {
      this.restClient = RestClient.builder().build();
    }
  }

  @Override
  public void sendNotification(String markdownMessage) {
    if (markdownMessage != null) {
      String finalMessage;
      if (markdownMessage.length() <= MAX_MESSAGE_LENGTH) {
        finalMessage = markdownMessage;
      } else {
        finalMessage = markdownMessage.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
      }

      String targetUri = resolveTargetUri();

      try {
        restClient.get().uri(targetUri, finalMessage).retrieve().toBodilessEntity();
        NotificationLog notificationLog =
            NotificationLog.builder()
                .channel(CHANNEL_TELEGRAM)
                .message(finalMessage)
                .status(NotificationStatus.SENT)
                .createdAt(OffsetDateTime.now())
                .build();
        notificationLogRepository.save(notificationLog);
      } catch (Exception exception) {
        String errorDescription = sanitizeErrorDescription(exception.getMessage());
        log.error("Failed to send Telegram notification: {}", errorDescription);
        NotificationLog failureLog =
            NotificationLog.builder()
                .channel(CHANNEL_TELEGRAM)
                .message(finalMessage)
                .status(NotificationStatus.FAILED)
                .errorMessage(errorDescription)
                .createdAt(OffsetDateTime.now())
                .build();
        notificationLogRepository.save(failureLog);
        throw new NotificationException("Failed to send Telegram notification", exception);
      }
    } else {
      log.error("Notification message cannot be null");
      throw new IllegalArgumentException("Notification message cannot be null");
    }
  }

  private String resolveTargetUri() {
    String rawUrl = telegramConfig.getMessageUrl();
    if (rawUrl != null && !rawUrl.isBlank()) {
      String apiToken = telegramConfig.getApiToken() != null ? telegramConfig.getApiToken() : "";
      String chatId = telegramConfig.getChatId() != null ? telegramConfig.getChatId() : "";

      String resolved =
          rawUrl
              .replace("{TELEGRAM_API_TOKEN}", apiToken)
              .replace("{apiToken}", apiToken)
              .replace("{TELEGRAM_CHAT_ID}", chatId)
              .replace("{chatId}", chatId)
              .replace("chat_id= ", "chat_id=")
              .replace("{MESSAGE}", "{message}");

      if (resolved.contains("?")) {
        if (!resolved.contains("parse_mode")) {
          resolved = resolved + "&parse_mode=MarkdownV2";
        }
        if (!resolved.contains("{message}")) {
          resolved = resolved + "&text={message}";
        }
      } else {
        resolved = resolved + "?parse_mode=MarkdownV2&text={message}";
      }

      return resolved;
    } else {
      log.error("Telegram message URL is not configured");
      throw new NotificationException("Telegram message URL is not configured");
    }
  }

  private String sanitizeErrorDescription(String message) {
    if (message != null) {
      String sanitized = message;
      String token = telegramConfig.getApiToken();
      if (token != null && !token.isBlank()) {
        sanitized = sanitized.replace(token, "******");
      }
      if (sanitized.length() <= MAX_MESSAGE_LENGTH) {
        return sanitized;
      } else {
        return sanitized.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
      }
    } else {
      return "Unknown error occurred";
    }
  }
}
