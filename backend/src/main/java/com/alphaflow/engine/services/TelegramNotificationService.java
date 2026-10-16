package com.alphaflow.engine.services;

import com.alphaflow.common.enums.NotificationStatus;
import com.alphaflow.common.exceptions.NotificationException;
import com.alphaflow.engine.configs.TelegramConfig;
import com.alphaflow.persistence.entities.NotificationLog;
import com.alphaflow.persistence.repositories.NotificationLogRepository;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class TelegramNotificationService implements NotificationService {

  private final TelegramConfig telegramConfig;
  private final NotificationLogRepository notificationLogRepository;
  private final RestClient restClient;

  @Autowired
  public TelegramNotificationService(
      TelegramConfig telegramConfig, NotificationLogRepository notificationLogRepository) {
    this.telegramConfig = telegramConfig;
    this.notificationLogRepository = notificationLogRepository;
    this.restClient = RestClient.builder().build();
  }

  @Override
  public void sendNotification(String message) {
    if (message != null) {
      int maxLength = telegramConfig.getMaxMessageLength();
      String finalMessage;
      if (message.length() <= maxLength) {
        finalMessage = message;
      } else {
        finalMessage = message.substring(0, maxLength - 3) + "...";
      }

      String targetUri = resolveTargetUri(finalMessage);
      NotificationStatus status = NotificationStatus.SENT;
      String errorMessage = null;
      Exception capturedException = null;

      try {
        restClient.get().uri(URI.create(targetUri)).retrieve().toBodilessEntity();
      } catch (Exception exception) {
        status = NotificationStatus.FAILED;
        errorMessage = exception.getMessage();
        capturedException = exception;
      }

      NotificationLog notificationLog =
          NotificationLog.builder()
              .message(finalMessage)
              .status(status)
              .errorMessage(errorMessage)
              .createdAt(OffsetDateTime.now())
              .build();
      notificationLogRepository.save(notificationLog);

      if (capturedException != null) {
        log.error("Failed to send Telegram notification: {}", errorMessage);
        throw new NotificationException("Failed to send Telegram notification", capturedException);
      }
    } else {
      log.error("Notification message cannot be null");
      throw new IllegalArgumentException("Notification message cannot be null");
    }
  }

  private String resolveTargetUri(String message) {
    String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
    return telegramConfig
        .getMessageUrl()
        .replace("{TELEGRAM_API_TOKEN}", telegramConfig.getApiToken())
        .replace("{TELEGRAM_CHAT_ID}", telegramConfig.getChatId())
        .replace("{MESSAGE}", encodedMessage);
  }
}
