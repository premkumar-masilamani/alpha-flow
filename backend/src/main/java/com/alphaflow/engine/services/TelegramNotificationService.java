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
    sendNotification(message, telegramConfig.getApiToken(), telegramConfig.getChatId());
  }

  @Override
  public void sendNotification(String message, String apiToken, String chatId) {
    if (message != null) {
      String effectiveToken =
          (apiToken != null && !apiToken.isBlank()) ? apiToken : telegramConfig.getApiToken();
      String effectiveChatId =
          (chatId != null && !chatId.isBlank()) ? chatId : telegramConfig.getChatId();

      if (isConfigured(effectiveToken, effectiveChatId)) {
        int maxLength = telegramConfig.getMaxMessageLength();
        String messageToSend;
        if (message.length() <= maxLength) {
          messageToSend = message;
        } else {
          messageToSend = message.substring(0, maxLength - 3) + "...";
        }

        String targetUri = resolveTargetUri(messageToSend, effectiveToken, effectiveChatId);
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
        String configError =
            "Telegram API token or chat ID is not configured. Please define TELEGRAM_API_TOKEN and TELEGRAM_CHAT_ID in your environment or .env file.";
        log.error(configError);
        NotificationLog notificationLog =
            NotificationLog.builder()
                .message(message)
                .error(configError)
                .createdAt(OffsetDateTime.now())
                .build();
        notificationLogRepository.save(notificationLog);
        throw new NotificationException(configError);
      }
    } else {
      log.error("Notification message cannot be null");
      throw new IllegalArgumentException("Notification message cannot be null");
    }
  }

  private boolean isConfigured(String token, String chatId) {
    return token != null
        && !token.isBlank()
        && !token.startsWith("${")
        && chatId != null
        && !chatId.isBlank()
        && !chatId.startsWith("${");
  }

  private String resolveTargetUri(String message, String apiToken, String chatId) {
    String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
    return telegramConfig
        .getMessageUrl()
        .replace("{TELEGRAM_API_TOKEN}", apiToken)
        .replace("{TELEGRAM_CHAT_ID}", chatId)
        .replace("{MESSAGE}", encodedMessage);
  }
}
