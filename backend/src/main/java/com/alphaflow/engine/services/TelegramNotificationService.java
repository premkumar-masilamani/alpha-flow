package com.alphaflow.engine.services;

import com.alphaflow.common.exceptions.NotificationException;
import com.alphaflow.engine.configs.TelegramConfig;
import com.alphaflow.persistence.entities.NotificationLog;
import com.alphaflow.persistence.repositories.NotificationLogRepository;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class TelegramNotificationService implements NotificationService {

  private static final Set<Character> RESERVED_CHARACTERS =
      Set.of(
          '_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!');

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

  public static String escape(String text) {
    if (StringUtils.isNotEmpty(text)) {
      StringBuilder builder = new StringBuilder(text.length() * 2);
      for (int index = 0; index < text.length(); index++) {
        char character = text.charAt(index);
        if (RESERVED_CHARACTERS.contains(character)) {
          builder.append('\\');
        }
        builder.append(character);
      }
      return builder.toString();
    } else {
      return text;
    }
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

      if (capturedException == null) {
        log.info("Telegram notification sent successfully.");
      } else {
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
        .replace("{ApiToken}", telegramConfig.getApiToken())
        .replace("{chatId}", telegramConfig.getChatId())
        .replace("{message}", encodedMessage);
  }
}
