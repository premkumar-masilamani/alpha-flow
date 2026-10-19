package com.alphaflow.api.controllers;

import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.common.enums.OptionType;
import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.downloaders.AngelOneDownloader;
import com.alphaflow.engine.services.NotificationService;
import com.alphaflow.engine.strategies.SakthiIntradayStrategy;
import com.alphaflow.engine.strategies.options.NiftyOptionSelectorService;
import com.alphaflow.engine.strategies.options.dtos.RecommendedOptionDto;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Temporary test controller to manually verify the full 70-point reversal
 * notification flow with options recommendations and Telegram delivery.
 * NOTE: This is a test harness and can be removed after verification.
 */
@RestController
@RequestMapping("/api/test/sakthi")
@Slf4j
public class TestSakthiNotificationController {

  private final NotificationService notificationService;
  private final SakthiIntradayStrategy sakthiIntradayStrategy;
  private final NiftyOptionSelectorService niftyOptionSelectorService;
  private final AngelOneDownloader angelOneDownloader;

  @Autowired
  public TestSakthiNotificationController(
      NotificationService notificationService,
      SakthiIntradayStrategy sakthiIntradayStrategy,
      NiftyOptionSelectorService niftyOptionSelectorService,
      AngelOneDownloader angelOneDownloader) {
    this.notificationService = notificationService;
    this.sakthiIntradayStrategy = sakthiIntradayStrategy;
    this.niftyOptionSelectorService = niftyOptionSelectorService;
    this.angelOneDownloader = angelOneDownloader;
  }

  @PostMapping("/sync-intraday")
  public ResponseEntity<Map<String, Object>> syncIntradayPost() {
    log.info("Manual trigger received to sync Angel One 15-minute intraday prices...");
    int newlyIngested = angelOneDownloader.downloadIntradayPrices();
    return ResponseEntity.ok(Map.of("status", "SUCCESS", "newlyIngestedCandles", newlyIngested));
  }

  @GetMapping("/sync-intraday")
  public ResponseEntity<Map<String, Object>> syncIntradayGet() {
    return syncIntradayPost();
  }

  @GetMapping("/notification")
  public ResponseEntity<Map<String, Object>> testNotificationGet(
      @RequestParam(name = "scenario", defaultValue = "bear") String scenario,
      @RequestParam(name = "mockOption", defaultValue = "true") boolean mockOption,
      @RequestParam(name = "sendTelegram", defaultValue = "true") boolean sendTelegram,
      @RequestParam(name = "customMessage", required = false) String customMessage) {
    return handleTestNotification(scenario, mockOption, sendTelegram, customMessage);
  }

  @PostMapping("/notification")
  public ResponseEntity<Map<String, Object>> testNotificationPost(
      @RequestParam(name = "scenario", defaultValue = "bear") String scenario,
      @RequestParam(name = "mockOption", defaultValue = "true") boolean mockOption,
      @RequestParam(name = "sendTelegram", defaultValue = "true") boolean sendTelegram,
      @RequestParam(name = "customMessage", required = false) String customMessage) {
    return handleTestNotification(scenario, mockOption, sendTelegram, customMessage);
  }

  private ResponseEntity<Map<String, Object>> handleTestNotification(
      String scenario, boolean mockOption, boolean sendTelegram, String customMessage) {
    String formattedMessage;
    Optional<RecommendedOptionDto> recommendedOption = Optional.empty();
    OptionType targetOptionType = null;

    if (customMessage != null && !customMessage.isBlank()) {
      formattedMessage = customMessage;
    } else {
      boolean isBearScenario = !"bull".equalsIgnoreCase(scenario) && !"bull_reversal".equalsIgnoreCase(scenario) && !"bull_breach".equalsIgnoreCase(scenario);
      targetOptionType = isBearScenario ? OptionType.PUT : OptionType.CALL;
      OffsetDateTime now = OffsetDateTime.now(MarketConstants.IST_ZONE);

      BigDecimal currentPrice;
      BigDecimal extremePrice;
      BigDecimal referenceLevel;

      if (isBearScenario) {
        referenceLevel = new BigDecimal("24800.00");
        extremePrice = new BigDecimal("24700.00");
        currentPrice = new BigDecimal("24770.00");

        if (mockOption) {
          LocalDate mockExpiry = now.toLocalDate().plusDays(12);
          recommendedOption =
              Optional.of(
                  new RecommendedOptionDto(
                      "NIFTY" + mockExpiry.format(DateTimeFormatter.ofPattern("ddMMMyy")).toUpperCase(Locale.ENGLISH) + "25200PE",
                      "84833",
                      new BigDecimal("25200.00"),
                      OptionType.PUT,
                      mockExpiry,
                      12L,
                      new BigDecimal("650.00"),
                      new BigDecimal("430.00"),
                      new BigDecimal("220.00"),
                      new BigDecimal("-0.680000"),
                      new BigDecimal("13.850000"),
                      new BigDecimal("-14.200000")));
        } else {
          Ticker ticker = Ticker.builder().tickerSymbol("NIFTY50.NS").build();
          LocalDate today = now.toLocalDate();
          LocalDate sessionDate =
              MarketTradingCalendar.isTradingDay(today, Country.IN)
                  ? today
                  : MarketTradingCalendar.getPreviousTradingDay(today, Country.IN);
          recommendedOption =
              niftyOptionSelectorService.selectBestOption(
                  ticker, currentPrice, targetOptionType, sessionDate);
        }
      } else {
        referenceLevel = new BigDecimal("25200.00");
        extremePrice = new BigDecimal("25300.00");
        currentPrice = new BigDecimal("25230.00");

        if (mockOption) {
          LocalDate mockExpiry = now.toLocalDate().plusDays(12);
          recommendedOption =
              Optional.of(
                  new RecommendedOptionDto(
                      "NIFTY" + mockExpiry.format(DateTimeFormatter.ofPattern("ddMMMyy")).toUpperCase(Locale.ENGLISH) + "24800CE",
                      "84834",
                      new BigDecimal("24800.00"),
                      OptionType.CALL,
                      mockExpiry,
                      12L,
                      new BigDecimal("580.00"),
                      new BigDecimal("430.00"),
                      new BigDecimal("150.00"),
                      new BigDecimal("0.720000"),
                      new BigDecimal("14.100000"),
                      new BigDecimal("-13.500000")));
        } else {
          Ticker ticker = Ticker.builder().tickerSymbol("NIFTY50.NS").build();
          LocalDate today = now.toLocalDate();
          LocalDate sessionDate =
              MarketTradingCalendar.isTradingDay(today, Country.IN)
                  ? today
                  : MarketTradingCalendar.getPreviousTradingDay(today, Country.IN);
          recommendedOption =
              niftyOptionSelectorService.selectBestOption(
                  ticker, currentPrice, targetOptionType, sessionDate);
        }
      }

      Ticker ticker = Ticker.builder().tickerSymbol("NIFTY50.NS").build();
      LiveQuote quote =
          LiveQuote.builder()
              .ticker(ticker)
              .lastPrice(currentPrice)
              .quoteTime(now)
              .build();

      if ("bear_breach".equalsIgnoreCase(scenario)) {
        formattedMessage = sakthiIntradayStrategy.buildBearBreachMessage(ticker, quote, referenceLevel);
      } else if ("bull_breach".equalsIgnoreCase(scenario)) {
        formattedMessage = sakthiIntradayStrategy.buildBullBreachMessage(ticker, quote, referenceLevel);
      } else if (isBearScenario) {
        formattedMessage =
            sakthiIntradayStrategy.buildBearReversalMessage(
                ticker, quote, extremePrice, referenceLevel, recommendedOption);
      } else {
        formattedMessage =
            sakthiIntradayStrategy.buildBullReversalMessage(
                ticker, quote, extremePrice, referenceLevel, recommendedOption);
      }
    }

    boolean telegramSent = false;
    String telegramError = null;

    if (sendTelegram) {
      try {
        notificationService.sendNotification(formattedMessage);
        telegramSent = true;
        log.info("Test Telegram notification sent successfully for scenario: {}", scenario);
      } catch (Exception exception) {
        telegramError =
            (exception.getCause() != null)
                ? exception.getCause().getMessage()
                : exception.getMessage();
        log.error("Failed to send test Telegram notification: {}", telegramError, exception);
      }
    }

    Map<String, Object> response = new HashMap<>();
    response.put("scenario", scenario);
    response.put("targetOptionType", (targetOptionType != null) ? targetOptionType.name() : null);
    response.put("mockOption", mockOption);
    response.put("telegramSent", telegramSent);
    response.put("telegramError", telegramError);
    response.put("recommendedOption", recommendedOption.orElse(null));
    response.put("formattedMessage", formattedMessage);

    return ResponseEntity.ok(response);
  }
}
