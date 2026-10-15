package com.alphaflow.engine.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.alphaflow.common.enums.NotificationStatus;
import com.alphaflow.common.exceptions.NotificationException;
import com.alphaflow.engine.configs.TelegramConfig;
import com.alphaflow.persistence.entities.NotificationLog;
import com.alphaflow.persistence.repositories.NotificationLogRepository;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClient;

class TelegramNotificationServiceTest {

  private static HttpServer mockServer;
  private static String serverBaseUrl;
  private static final AtomicReference<String> lastReceivedQuery = new AtomicReference<>();

  private NotificationLogRepository notificationLogRepository;
  private TelegramConfig telegramConfig;
  private TelegramNotificationService service;

  @BeforeAll
  static void startMockServer() throws Exception {
    mockServer = HttpServer.create(new InetSocketAddress(0), 0);
    mockServer.createContext(
        "/",
        exchange -> {
          String path = exchange.getRequestURI().getPath();
          if (path.contains("/error")) {
            byte[] responseBytes =
                "{\"ok\":false,\"description\":\"Bad Request\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, responseBytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
              outputStream.write(responseBytes);
            }
          } else {
            lastReceivedQuery.set(exchange.getRequestURI().getRawQuery());
            byte[] responseBytes = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
              outputStream.write(responseBytes);
            }
          }
        });
    mockServer.start();
    serverBaseUrl = "http://localhost:" + mockServer.getAddress().getPort();
  }

  @AfterAll
  static void stopMockServer() {
    if (mockServer != null) {
      mockServer.stop(0);
    }
  }

  @BeforeEach
  void setUp() {
    lastReceivedQuery.set(null);
    notificationLogRepository = mock(NotificationLogRepository.class);
    telegramConfig = new TelegramConfig();
    telegramConfig.setApiToken("test-token");
    telegramConfig.setChatId("123456");
    telegramConfig.setMessageUrl(
        serverBaseUrl
            + "/bot{apiToken}/sendMessage?chat_id={chatId}&text={message}&parse_mode=MarkdownV2");

    service =
        new TelegramNotificationService(
            telegramConfig, notificationLogRepository, RestClient.builder());
  }

  @Test
  void testSendNotificationSuccess() {
    String message = "Market alert: Breakout detected on AAPL";
    service.sendNotification(message);

    String receivedQuery = lastReceivedQuery.get();
    assertNotNull(receivedQuery);
    assertTrue(receivedQuery.contains("chat_id=123456"));
    assertTrue(receivedQuery.contains("parse_mode=MarkdownV2"));

    ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
    verify(notificationLogRepository).save(captor.capture());
    NotificationLog savedLog = captor.getValue();

    assertEquals("TELEGRAM", savedLog.getChannel());
    assertEquals(message, savedLog.getMessage());
    assertEquals(NotificationStatus.SENT, savedLog.getStatus());
    assertNull(savedLog.getErrorMessage());
    assertNotNull(savedLog.getCreatedAt());
  }

  @Test
  void testSendNotificationTruncationWhenExceedingMaxLimit() {
    String longMessage = "A".repeat(4100);
    service.sendNotification(longMessage);

    ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
    verify(notificationLogRepository).save(captor.capture());
    NotificationLog savedLog = captor.getValue();

    assertEquals(4096, savedLog.getMessage().length());
    assertTrue(savedLog.getMessage().endsWith("..."));
    assertEquals(NotificationStatus.SENT, savedLog.getStatus());
  }

  @Test
  void testSendNotificationHttpErrorThrowsExceptionAndLogsFailure() {
    telegramConfig.setMessageUrl(
        serverBaseUrl + "/error/sendMessage?chat_id={chatId}&text={message}&parse_mode=MarkdownV2");

    NotificationException exception =
        assertThrows(
            NotificationException.class, () -> service.sendNotification("Test error payload"));
    assertTrue(exception.getMessage().contains("Failed to send Telegram notification"));

    ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
    verify(notificationLogRepository).save(captor.capture());
    NotificationLog savedLog = captor.getValue();

    assertEquals("TELEGRAM", savedLog.getChannel());
    assertEquals(NotificationStatus.FAILED, savedLog.getStatus());
    assertNotNull(savedLog.getErrorMessage());
    assertTrue(savedLog.getErrorMessage().contains("400"));
  }

  @Test
  void testSendNotificationConnectionFailureThrowsException() {
    telegramConfig.setMessageUrl(
        "http://localhost:1/bot{apiToken}/sendMessage?chat_id={chatId}&text={message}");

    NotificationException exception =
        assertThrows(
            NotificationException.class,
            () -> service.sendNotification("Connection failure message"));
    assertTrue(exception.getMessage().contains("Failed to send Telegram notification"));

    ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
    verify(notificationLogRepository).save(captor.capture());
    NotificationLog savedLog = captor.getValue();

    assertEquals(NotificationStatus.FAILED, savedLog.getStatus());
  }

  @Test
  void testSendNotificationThrowsWhenMessageIsNull() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> service.sendNotification(null));
    assertEquals("Notification message cannot be null", exception.getMessage());
    verify(notificationLogRepository, never()).save(any());
  }

  @Test
  void testSendNotificationThrowsWhenMessageUrlIsMissing() {
    telegramConfig.setMessageUrl(null);
    NotificationException exception =
        assertThrows(NotificationException.class, () -> service.sendNotification("Some message"));
    assertEquals("Telegram message URL is not configured", exception.getMessage());

    telegramConfig.setMessageUrl("   ");
    NotificationException blankException =
        assertThrows(NotificationException.class, () -> service.sendNotification("Some message"));
    assertEquals("Telegram message URL is not configured", blankException.getMessage());
    verify(notificationLogRepository, never()).save(any());
  }

  @Test
  void testResolveTargetUriTemplateVariations() {
    telegramConfig.setApiToken("custom-token");
    telegramConfig.setChatId("987654");
    telegramConfig.setMessageUrl(
        serverBaseUrl
            + "/bot{TELEGRAM_API_TOKEN}/sendMessage?chat_id= {TELEGRAM_CHAT_ID}&text={MESSAGE}");

    service.sendNotification("Testing uppercase template");

    String receivedQuery = lastReceivedQuery.get();
    assertNotNull(receivedQuery);
    assertTrue(receivedQuery.contains("chat_id=987654"));
    assertTrue(receivedQuery.contains("parse_mode=MarkdownV2"));
  }

  @Test
  void testResolveTargetUriWithoutQueryParams() {
    telegramConfig.setMessageUrl(serverBaseUrl + "/bot-test-token/sendMessage");

    service.sendNotification("Testing base url without query params");

    String receivedQuery = lastReceivedQuery.get();
    assertNotNull(receivedQuery);
    assertTrue(receivedQuery.contains("parse_mode=MarkdownV2"));
    assertTrue(receivedQuery.contains("text="));
  }

  @Test
  void testResolveTargetUriWithQueryParamWithoutParseMode() {
    telegramConfig.setMessageUrl(
        serverBaseUrl + "/bot-test-token/sendMessage?dummy=true&text={message}");

    service.sendNotification("Testing url with existing query param without parse_mode");

    String receivedQuery = lastReceivedQuery.get();
    assertNotNull(receivedQuery);
    assertTrue(receivedQuery.contains("parse_mode=MarkdownV2"));
    assertTrue(receivedQuery.contains("dummy=true"));
  }

  @Test
  void testResolveTargetUriWithQueryParamWithoutMessage() {
    telegramConfig.setMessageUrl(
        serverBaseUrl + "/bot-test-token/sendMessage?parse_mode=MarkdownV2");

    service.sendNotification("Testing url with parse_mode without message param");

    String receivedQuery = lastReceivedQuery.get();
    assertNotNull(receivedQuery);
    assertTrue(receivedQuery.contains("parse_mode=MarkdownV2"));
    assertTrue(receivedQuery.contains("text="));
  }

  @Test
  void testResolveTargetUriWithNullApiTokenAndChatId() {
    telegramConfig.setApiToken(null);
    telegramConfig.setChatId(null);
    telegramConfig.setMessageUrl(serverBaseUrl + "/bot{apiToken}/sendMessage?chat_id={chatId}");

    service.sendNotification("Testing null tokens");

    String receivedQuery = lastReceivedQuery.get();
    assertNotNull(receivedQuery);
    assertTrue(receivedQuery.contains("chat_id="));
  }

  @Test
  void testDefaultRestClientBuilderFallback() {
    TelegramNotificationService defaultService =
        new TelegramNotificationService(telegramConfig, notificationLogRepository, null);
    assertNotNull(defaultService);
  }

  @Test
  void testSanitizeErrorDescriptionMasksToken() {
    telegramConfig.setApiToken("secret-token-12345");
    telegramConfig.setMessageUrl(serverBaseUrl + "/error/sendMessage");

    assertThrows(NotificationException.class, () -> service.sendNotification("Error payload"));

    ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
    verify(notificationLogRepository).save(captor.capture());
    NotificationLog savedLog = captor.getValue();

    assertNotNull(savedLog.getErrorMessage());
    assertFalse(savedLog.getErrorMessage().contains("secret-token-12345"));
  }

  @Test
  void testSanitizeErrorDescriptionWithNullAndBlankToken() {
    telegramConfig.setApiToken(null);
    telegramConfig.setMessageUrl(serverBaseUrl + "/error/sendMessage");
    assertThrows(
        NotificationException.class, () -> service.sendNotification("Error with null token"));

    telegramConfig.setApiToken("   ");
    assertThrows(
        NotificationException.class, () -> service.sendNotification("Error with blank token"));
  }

  @Test
  void testSanitizeErrorDescriptionWithNullMessage() {
    RestClient.Builder customBuilder =
        RestClient.builder()
            .requestInterceptor(
                (request, body, execution) -> {
                  throw new RuntimeException((String) null);
                });

    TelegramNotificationService nullMessageService =
        new TelegramNotificationService(telegramConfig, notificationLogRepository, customBuilder);

    assertThrows(
        NotificationException.class,
        () -> nullMessageService.sendNotification("Null message error"));

    ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
    verify(notificationLogRepository).save(captor.capture());
    NotificationLog savedLog = captor.getValue();

    assertEquals("Unknown error occurred", savedLog.getErrorMessage());
  }

  @Test
  void testSanitizeErrorDescriptionTruncationWhenExceedingMaxLimit() {
    String longError = "E".repeat(5000);
    RestClient.Builder customBuilder =
        RestClient.builder()
            .requestInterceptor(
                (request, body, execution) -> {
                  throw new RuntimeException(longError);
                });

    TelegramNotificationService longErrorService =
        new TelegramNotificationService(telegramConfig, notificationLogRepository, customBuilder);

    assertThrows(
        NotificationException.class, () -> longErrorService.sendNotification("Long error payload"));

    ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
    verify(notificationLogRepository).save(captor.capture());
    NotificationLog savedLog = captor.getValue();

    assertEquals(4096, savedLog.getErrorMessage().length());
    assertTrue(savedLog.getErrorMessage().endsWith("..."));
  }
}
