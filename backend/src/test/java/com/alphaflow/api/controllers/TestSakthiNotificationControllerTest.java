package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.engine.strategies.options.enums.OptionType;
import com.alphaflow.engine.services.NotificationService;
import com.alphaflow.engine.services.NotificationTemplateEngine;
import com.alphaflow.engine.strategies.SakthiIntradayStrategy;
import com.alphaflow.engine.strategies.options.NiftyOptionSelectorService;
import com.alphaflow.engine.strategies.options.dtos.RecommendedOptionDto;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class TestSakthiNotificationControllerTest {

  private NotificationService notificationService;
  private NiftyOptionSelectorService niftyOptionSelectorService;
  private SakthiIntradayStrategy sakthiIntradayStrategy;
  private TestSakthiNotificationController controller;

  @BeforeEach
  void setUp() {
    notificationService = mock(NotificationService.class);
    niftyOptionSelectorService = mock(NiftyOptionSelectorService.class);
    DailyPriceRepository dailyPriceRepository = mock(DailyPriceRepository.class);
    NotificationTemplateEngine notificationTemplateEngine = new NotificationTemplateEngine();
    sakthiIntradayStrategy =
        new SakthiIntradayStrategy(
            dailyPriceRepository,
            notificationService,
            niftyOptionSelectorService,
            notificationTemplateEngine);
    controller =
        new TestSakthiNotificationController(
            notificationService, sakthiIntradayStrategy, niftyOptionSelectorService);
  }

  @Test
  void testBearScenarioMockOptionGet() {
    ResponseEntity<Map<String, Object>> response =
        controller.testNotificationGet("bear", true, true, null);

    assertEquals(200, response.getStatusCode().value());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertEquals("bear", body.get("scenario"));
    assertEquals("PUT", body.get("targetOptionType"));
    assertTrue((Boolean) body.get("telegramSent"));

    String message = (String) body.get("formattedMessage");
    assertTrue(message.contains("BULLISH REVERSAL"));
    assertTrue(message.contains("BUY PUT"));
    verify(notificationService).sendNotification(anyString());
  }

  @Test
  void testBullScenarioMockOptionPost() {
    ResponseEntity<Map<String, Object>> response =
        controller.testNotificationPost("bull", true, true, null);

    assertEquals(200, response.getStatusCode().value());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertEquals("bull", body.get("scenario"));
    assertEquals("CALL", body.get("targetOptionType"));
    assertTrue((Boolean) body.get("telegramSent"));

    String message = (String) body.get("formattedMessage");
    assertTrue(message.contains("BEARISH REVERSAL"));
    assertTrue(message.contains("BUY CALL"));
    verify(notificationService).sendNotification(anyString());
  }

  @Test
  void testBreachScenarios() {
    ResponseEntity<Map<String, Object>> bearBreachResponse =
        controller.testNotificationGet("bear_breach", true, false, null);
    assertEquals(200, bearBreachResponse.getStatusCode().value());
    Map<String, Object> bearBody = bearBreachResponse.getBody();
    assertNotNull(bearBody);
    String bearMessage = (String) bearBody.get("formattedMessage");
    assertTrue(bearMessage.contains("BEARISH ALERT"));
    assertTrue(bearMessage.contains("Previous Day Low Breached"));

    ResponseEntity<Map<String, Object>> bullBreachResponse =
        controller.testNotificationGet("bull_breach", true, false, null);
    assertEquals(200, bullBreachResponse.getStatusCode().value());
    Map<String, Object> bullBody = bullBreachResponse.getBody();
    assertNotNull(bullBody);
    String bullMessage = (String) bullBody.get("formattedMessage");
    assertTrue(bullMessage.contains("BULLISH ALERT"));
    assertTrue(bullMessage.contains("Previous Day High Breached"));
  }

  @Test
  void testCustomMessage() {
    String custom = "Hello Telegram from Antigravity!";
    ResponseEntity<Map<String, Object>> response =
        controller.testNotificationPost("custom", false, true, custom);

    assertEquals(200, response.getStatusCode().value());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertEquals(custom, body.get("formattedMessage"));
    assertTrue((Boolean) body.get("telegramSent"));
    verify(notificationService).sendNotification(custom);
  }

  @Test
  void testBearScenarioLiveOptionSelectorFallback() {
    when(niftyOptionSelectorService.selectBestOption(any(), any(), any(), any()))
        .thenReturn(Optional.empty());

    ResponseEntity<Map<String, Object>> response =
        controller.testNotificationPost("bear", false, false, null);

    assertEquals(200, response.getStatusCode().value());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertFalse((Boolean) body.get("telegramSent"));

    String message = (String) body.get("formattedMessage");
    assertTrue(message.contains("No option contract found"));
  }

  @Test
  void testBullScenarioLiveOptionSelectorPresent() {
    RecommendedOptionDto mockDto =
        new RecommendedOptionDto(
            "NIFTY29OCT2624800CE",
            "84834",
            new BigDecimal("24800.00"),
            OptionType.CALL,
            LocalDate.of(2026, 10, 29),
            12L,
            new BigDecimal("580.00"),
            new BigDecimal("430.00"),
            new BigDecimal("150.00"),
            null,
            null,
            null);
    when(niftyOptionSelectorService.selectBestOption(any(), any(), any(), any()))
        .thenReturn(Optional.of(mockDto));

    ResponseEntity<Map<String, Object>> response =
        controller.testNotificationGet("bull", false, true, null);

    assertEquals(200, response.getStatusCode().value());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertTrue((Boolean) body.get("telegramSent"));
  }

  @Test
  void testTelegramSendFailureHandledGracefully() {
    doThrow(new RuntimeException("Telegram API network timeout"))
        .when(notificationService)
        .sendNotification(anyString());

    ResponseEntity<Map<String, Object>> response =
        controller.testNotificationPost("bear", true, true, null);

    assertEquals(200, response.getStatusCode().value());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertFalse((Boolean) body.get("telegramSent"));
    assertEquals("Telegram API network timeout", body.get("telegramError"));
  }
}
