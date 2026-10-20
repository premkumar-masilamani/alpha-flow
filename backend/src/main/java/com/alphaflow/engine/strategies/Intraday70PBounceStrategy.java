package com.alphaflow.engine.strategies;

import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.services.NotificationService;
import com.alphaflow.engine.services.NotificationTemplateEngine;
import com.alphaflow.engine.services.TelegramNotificationService;
import com.alphaflow.engine.strategies.model.Intraday70PBounceStrategyState;
import com.alphaflow.engine.strategies.options.NiftyOptionSelectorService;
import com.alphaflow.engine.strategies.options.dtos.RecommendedOptionDto;
import com.alphaflow.engine.strategies.options.enums.OptionType;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Intraday70PBounceStrategy {

  public static final BigDecimal REBOUND_THRESHOLD_POINTS = new BigDecimal("70");
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter OPTION_EXPIRY_FORMATTER =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

  private final DailyPriceRepository dailyPriceRepository;
  private final NotificationService notificationService;
  private final NiftyOptionSelectorService niftyOptionSelectorService;
  private final NotificationTemplateEngine notificationTemplateEngine;
  private final ConcurrentMap<Long, Intraday70PBounceStrategyState> stateMap =
      new ConcurrentHashMap<>();

  public Intraday70PBounceStrategy(
      DailyPriceRepository dailyPriceRepository,
      NotificationService notificationService,
      NiftyOptionSelectorService niftyOptionSelectorService,
      NotificationTemplateEngine notificationTemplateEngine) {
    this.dailyPriceRepository = dailyPriceRepository;
    this.notificationService = notificationService;
    this.niftyOptionSelectorService = niftyOptionSelectorService;
    this.notificationTemplateEngine = notificationTemplateEngine;
  }

  public void execute(Ticker ticker, LiveQuote liveQuote) {
    LocalDate sessionDate =
        liveQuote.getQuoteTime().atZoneSameInstant(MarketConfig.IST_ZONE).toLocalDate();

    Intraday70PBounceStrategyState state =
        stateMap.compute(
            ticker.getTickerId(),
            (tickerId, existingState) -> {
              if (existingState != null && sessionDate.equals(existingState.getSessionDate())) {
                return existingState;
              } else {
                Optional<PriceBar> previousDayData =
                    fetchPreviousTradingDayData(ticker, sessionDate);
                if (previousDayData.isPresent()) {
                  PriceBar priceBar = previousDayData.get();
                  return new Intraday70PBounceStrategyState(
                      sessionDate, priceBar.date(), priceBar.high(), priceBar.low());
                } else {
                  return new Intraday70PBounceStrategyState(sessionDate);
                }
              }
            });

    if (state.getPreviousTradingDate() != null) {
      BigDecimal previousDayHigh = state.getPreviousDayHigh();
      BigDecimal previousDayLow = state.getPreviousDayLow();
      BigDecimal currentPrice = liveQuote.getLastPrice();

      evaluateBearCase(ticker, liveQuote, state, currentPrice, previousDayLow);
      evaluateBullCase(ticker, liveQuote, state, currentPrice, previousDayHigh);
    }
  }

  private Optional<PriceBar> fetchPreviousTradingDayData(Ticker ticker, LocalDate sessionDate) {
    LocalDate previousTradingDate =
        MarketTradingCalendar.getPreviousTradingDay(sessionDate, ticker.getCountry());

    List<DailyPrice> recentPrices =
        dailyPriceRepository.findLatestByTickerAndEndDate(
            ticker, previousTradingDate, PageRequest.of(0, 1));

    if (!recentPrices.isEmpty()) {
      DailyPrice previousDayPrice = recentPrices.getFirst();
      return Optional.of(
          new PriceBar(
              previousTradingDate,
              previousDayPrice.getPriceOpen(),
              previousDayPrice.getPriceHigh(),
              previousDayPrice.getPriceLow(),
              previousDayPrice.getPriceClose(),
              previousDayPrice.getVolume()));
    } else {
      log.warn(
          "Skipping Intraday70PBounceStrategy evaluation for {}: no prior daily prices found on or before {}",
          ticker.getTickerSymbol(),
          previousTradingDate);
      return Optional.empty();
    }
  }

  private void evaluateBearCase(
      Ticker ticker,
      LiveQuote liveQuote,
      Intraday70PBounceStrategyState state,
      BigDecimal currentPrice,
      BigDecimal previousDayLow) {
    if (!state.isBearBreachNotified()) {
      if (currentPrice.compareTo(previousDayLow) < 0) {
        dispatchNotification(buildBearBreachMessage(ticker, liveQuote, previousDayLow));
        state.setBearBreachNotified(true);
        state.setBearLowestPrice(currentPrice);
      }
    } else if (!state.isBearReversalNotified()) {
      if (currentPrice.compareTo(state.getBearLowestPrice()) < 0) {
        state.setBearLowestPrice(currentPrice);
      } else if (currentPrice.compareTo(state.getBearLowestPrice().add(REBOUND_THRESHOLD_POINTS))
          >= 0) {
        LocalDate sessionDate = state.getSessionDate();
        Optional<RecommendedOptionDto> recommendedOption =
            fetchOptionRecommendation(ticker, currentPrice, OptionType.PUT, sessionDate);
        dispatchNotification(
            buildBearReversalMessage(
                ticker,
                liveQuote,
                state.getBearLowestPrice(),
                previousDayLow,
                recommendedOption));
        state.setBearReversalNotified(true);
      }
    }
  }

  private void evaluateBullCase(
      Ticker ticker,
      LiveQuote liveQuote,
      Intraday70PBounceStrategyState state,
      BigDecimal currentPrice,
      BigDecimal previousDayHigh) {
    if (!state.isBullBreachNotified()) {
      if (currentPrice.compareTo(previousDayHigh) > 0) {
        dispatchNotification(buildBullBreachMessage(ticker, liveQuote, previousDayHigh));
        state.setBullBreachNotified(true);
        state.setBullHighestPrice(currentPrice);
      }
    } else if (!state.isBullReversalNotified()) {
      if (currentPrice.compareTo(state.getBullHighestPrice()) > 0) {
        state.setBullHighestPrice(currentPrice);
      } else if (currentPrice.compareTo(
              state.getBullHighestPrice().subtract(REBOUND_THRESHOLD_POINTS))
          <= 0) {
        LocalDate sessionDate = state.getSessionDate();
        Optional<RecommendedOptionDto> recommendedOption =
            fetchOptionRecommendation(ticker, currentPrice, OptionType.CALL, sessionDate);
        dispatchNotification(
            buildBullReversalMessage(
                ticker,
                liveQuote,
                state.getBullHighestPrice(),
                previousDayHigh,
                recommendedOption));
        state.setBullReversalNotified(true);
      }
    }
  }

  private Optional<RecommendedOptionDto> fetchOptionRecommendation(
      Ticker ticker, BigDecimal spotPrice, OptionType optionType, LocalDate sessionDate) {
    try {
      return niftyOptionSelectorService.selectBestOption(
          ticker, spotPrice, optionType, sessionDate);
    } catch (Exception exception) {
      log.error(
          "Error selecting option recommendation for {}: {}",
          ticker.getTickerSymbol(),
          exception.getMessage(),
          exception);
      return Optional.empty();
    }
  }

  private void dispatchNotification(String message) {
    try {
      notificationService.sendNotification(message);
    } catch (Exception exception) {
      log.error("Failed to send Telegram alert: {}", exception.getMessage(), exception);
    }
  }

  public String buildBearBreachMessage(
      Ticker ticker, LiveQuote liveQuote, BigDecimal previousDayLow) {
    Map<String, String> variables =
        Map.ofEntries(
            Map.entry("ticker", TelegramNotificationService.escape(ticker.getTickerSymbol())),
            Map.entry(
                "spotPrice",
                TelegramNotificationService.escape(formatPrice(liveQuote.getLastPrice()))),
            Map.entry(
                "previousDayLow", TelegramNotificationService.escape(formatPrice(previousDayLow))),
            Map.entry(
                "timestamp",
                TelegramNotificationService.escape(formatTimestamp(liveQuote.getQuoteTime()))));
    return notificationTemplateEngine.render("notifications/bear_breach.md", variables);
  }

  public String buildBearReversalMessage(
      Ticker ticker,
      LiveQuote liveQuote,
      BigDecimal lowestPrice,
      BigDecimal previousDayLow,
      Optional<RecommendedOptionDto> recommendedOption) {
    BigDecimal reboundPoints = liveQuote.getLastPrice().subtract(lowestPrice);
    Map<String, String> variables =
        Map.ofEntries(
            Map.entry("ticker", TelegramNotificationService.escape(ticker.getTickerSymbol())),
            Map.entry(
                "spotPrice",
                TelegramNotificationService.escape(formatPrice(liveQuote.getLastPrice()))),
            Map.entry("dayLow", TelegramNotificationService.escape(formatPrice(lowestPrice))),
            Map.entry(
                "reboundPoints", TelegramNotificationService.escape(formatPrice(reboundPoints))),
            Map.entry(
                "previousDayLow", TelegramNotificationService.escape(formatPrice(previousDayLow))),
            Map.entry(
                "timestamp",
                TelegramNotificationService.escape(formatTimestamp(liveQuote.getQuoteTime()))),
            Map.entry(
                "optionDetails", buildOptionDetailsMessage(recommendedOption, OptionType.PUT)));
    return notificationTemplateEngine.render("notifications/bear_reversal.md", variables);
  }

  public String buildBullBreachMessage(
      Ticker ticker, LiveQuote liveQuote, BigDecimal previousDayHigh) {
    Map<String, String> variables =
        Map.ofEntries(
            Map.entry("ticker", TelegramNotificationService.escape(ticker.getTickerSymbol())),
            Map.entry(
                "spotPrice",
                TelegramNotificationService.escape(formatPrice(liveQuote.getLastPrice()))),
            Map.entry(
                "previousDayHigh",
                TelegramNotificationService.escape(formatPrice(previousDayHigh))),
            Map.entry(
                "timestamp",
                TelegramNotificationService.escape(formatTimestamp(liveQuote.getQuoteTime()))));
    return notificationTemplateEngine.render("notifications/bull_breach.md", variables);
  }

  public String buildBullReversalMessage(
      Ticker ticker,
      LiveQuote liveQuote,
      BigDecimal highestPrice,
      BigDecimal previousDayHigh,
      Optional<RecommendedOptionDto> recommendedOption) {
    BigDecimal pullbackPoints = highestPrice.subtract(liveQuote.getLastPrice());
    Map<String, String> variables =
        Map.ofEntries(
            Map.entry("ticker", TelegramNotificationService.escape(ticker.getTickerSymbol())),
            Map.entry(
                "spotPrice",
                TelegramNotificationService.escape(formatPrice(liveQuote.getLastPrice()))),
            Map.entry("dayHigh", TelegramNotificationService.escape(formatPrice(highestPrice))),
            Map.entry(
                "pullbackPoints", TelegramNotificationService.escape(formatPrice(pullbackPoints))),
            Map.entry(
                "previousDayHigh",
                TelegramNotificationService.escape(formatPrice(previousDayHigh))),
            Map.entry(
                "timestamp",
                TelegramNotificationService.escape(formatTimestamp(liveQuote.getQuoteTime()))),
            Map.entry(
                "optionDetails", buildOptionDetailsMessage(recommendedOption, OptionType.CALL)));
    return notificationTemplateEngine.render("notifications/bull_reversal.md", variables);
  }

  private String buildOptionDetailsMessage(
      Optional<RecommendedOptionDto> recommendedOptionOptional, OptionType optionType) {
    if (recommendedOptionOptional.isPresent()) {
      RecommendedOptionDto option = recommendedOptionOptional.get();
      String action = optionType == OptionType.PUT ? "BUY PUT" : "BUY CALL";

      final String expiryStr =
          option.expiryDate().format(OPTION_EXPIRY_FORMATTER).toUpperCase(Locale.ENGLISH)
              + " ("
              + option.daysToExpiry()
              + " DTE)";

      Map<String, String> variables =
          Map.ofEntries(
              Map.entry("action", TelegramNotificationService.escape(action)),
              Map.entry("contract", TelegramNotificationService.escape(option.tradingSymbol())),
              Map.entry(
                  "strikePrice",
                  TelegramNotificationService.escape(formatPrice(option.strikePrice()))),
              Map.entry("expiry", TelegramNotificationService.escape(expiryStr)),
              Map.entry(
                  "lastPrice", TelegramNotificationService.escape(formatPrice(option.lastPrice()))),
              Map.entry(
                  "intrinsicValue",
                  TelegramNotificationService.escape(formatPrice(option.intrinsicValue()))),
              Map.entry(
                  "timeValue", TelegramNotificationService.escape(formatPrice(option.timeValue()))));

      return notificationTemplateEngine.render("notifications/option_details.md", variables);
    } else {
      return notificationTemplateEngine.render("notifications/option_fallback.md", Map.of());
    }
  }

  private String formatPrice(BigDecimal price) {
    return price.stripTrailingZeros().toPlainString();
  }

  private String formatTimestamp(OffsetDateTime timestamp) {
    return timestamp.atZoneSameInstant(MarketConfig.IST_ZONE).format(DATE_TIME_FORMATTER);
  }
}
