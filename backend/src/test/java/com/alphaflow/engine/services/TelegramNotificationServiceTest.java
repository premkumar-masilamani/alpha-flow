package com.alphaflow.engine.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
            + "/bot{ApiToken}/sendMessage?chat_id={chatId}&text={message}&parse_mode=MarkdownV2");

    service = new TelegramNotificationService(telegramConfig, notificationLogRepository);
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

    assertEquals(message, savedLog.getMessage());
    assertNull(savedLog.getError());
    assertNotNull(savedLog.getCreatedAt());
  }

  @Test
  void testSendNotificationTruncationForTelegramPreservesFullMessageInDatabase() {
    String longMessage = "A".repeat(4100);
    service.sendNotification(longMessage);

    String receivedQuery = lastReceivedQuery.get();
    assertNotNull(receivedQuery);
    assertTrue(receivedQuery.contains("text="));
    assertTrue(receivedQuery.contains("..."));

    ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
    verify(notificationLogRepository).save(captor.capture());
    NotificationLog savedLog = captor.getValue();

    assertEquals(4100, savedLog.getMessage().length());
    assertEquals(longMessage, savedLog.getMessage());
    assertNull(savedLog.getError());
  }

  @Test
  void testSendNotificationHttpErrorThrowsExceptionAndLogsFailure() {
    telegramConfig.setMessageUrl(
        serverBaseUrl
            + "/error/sendMessage?chat_id={chatId}&text={message}&parse_mode=MarkdownV2");

    NotificationException exception =
        assertThrows(
            NotificationException.class, () -> service.sendNotification("Test error payload"));
    assertTrue(exception.getMessage().contains("Failed to send Telegram notification"));

    ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
    verify(notificationLogRepository).save(captor.capture());
    NotificationLog savedLog = captor.getValue();

    assertNotNull(savedLog.getError());
    assertTrue(savedLog.getError().contains("400"));
  }

  @Test
  void testSendNotificationConnectionFailureThrowsException() {
    telegramConfig.setMessageUrl(
        "http://localhost:1/bot{ApiToken}/sendMessage?chat_id={chatId}&text={message}");

    NotificationException exception =
        assertThrows(
            NotificationException.class,
            () -> service.sendNotification("Connection failure message"));
    assertTrue(exception.getMessage().contains("Failed to send Telegram notification"));

    ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
    verify(notificationLogRepository).save(captor.capture());
    NotificationLog savedLog = captor.getValue();

    assertNotNull(savedLog.getError());
  }

  @Test
  void testSendNotificationThrowsWhenMessageIsNull() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> service.sendNotification(null));
    assertEquals("Notification message cannot be null", exception.getMessage());
    verify(notificationLogRepository, never()).save(any());
  }

  @Test
  void testSendNotificationWithCustomTokenAndChatId() {
    telegramConfig.setApiToken("override-token");
    telegramConfig.setChatId("987654");
    service.sendNotification("Custom token message");

    String receivedQuery = lastReceivedQuery.get();
    assertNotNull(receivedQuery);
    assertTrue(receivedQuery.contains("chat_id=987654"));
  }

  @Test
  void testEscapeNullAndEmpty() {
    assertNull(TelegramNotificationService.escape(null));
    assertEquals("", TelegramNotificationService.escape(""));
  }

  @Test
  void testEscapeWithoutSpecialCharacters() {
    assertEquals("Hello World 123", TelegramNotificationService.escape("Hello World 123"));
  }

  @Test
  void testEscapeAllReservedCharacters() {
    String reservedCharacters = "_*[]()~`>#+-=|{}.!";
    String expected = "\\_\\*\\[\\]\\(\\)\\~\\`\\>\\#\\+\\-\\=\\|\\{\\}\\.\\!";
    assertEquals(expected, TelegramNotificationService.escape(reservedCharacters));
  }

  @Test
  void testEscapeMixedText() {
    String rawText = "Breakout on AAPL: Target [150.50] (Score: +5.2)!";
    String expected = "Breakout on AAPL: Target \\[150\\.50\\] \\(Score: \\+5\\.2\\)\\!";
    assertEquals(expected, TelegramNotificationService.escape(rawText));
  }
}
