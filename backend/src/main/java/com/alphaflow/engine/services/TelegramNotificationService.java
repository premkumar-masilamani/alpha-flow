package com.alphaflow.engine.services;

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
      String messageToSend;
      if (message.length() <= maxLength) {
        messageToSend = message;
      } else {
        messageToSend = message.substring(0, maxLength - 3) + "...";
      }

      String targetUri = resolveTargetUri(messageToSend);
      String error = null;
      Exception capturedException = null;

      try {
        restClient.get().uri(URI.create(targetUri)).retrieve().toBodilessEntity();
      } catch (Exception exception) {
        error = exception.getMessage();
        capturedException = exception;
      }

      NotificationLog notificationLog =
          NotificationLog.builder()
              .message(message)
              .error(error)
              .createdAt(OffsetDateTime.now())
              .build();
      notificationLogRepository.save(notificationLog);

      if (capturedException != null) {
        log.error("Failed to send Telegram notification: {}", error);
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
