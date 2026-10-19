package com.alphaflow.engine.strategies;

import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.engine.strategies.options.enums.OptionType;
import com.alphaflow.common.utils.TelegramMarkdownV2Helper;
import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.services.NotificationService;
import com.alphaflow.engine.services.NotificationTemplateEngine;
import com.alphaflow.engine.strategies.model.SakthiIntradayStrategyState;
import com.alphaflow.engine.strategies.options.NiftyOptionSelectorService;
import com.alphaflow.engine.strategies.options.dtos.RecommendedOptionDto;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
public class SakthiIntradayStrategy {

  public static final BigDecimal REBOUND_THRESHOLD_POINTS = new BigDecimal("70");
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter OPTION_EXPIRY_FORMATTER =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

  private final DailyPriceRepository dailyPriceRepository;
  private final NotificationService notificationService;
  private final NiftyOptionSelectorService niftyOptionSelectorService;
  private final NotificationTemplateEngine notificationTemplateEngine;
  private final ConcurrentMap<Long, SakthiIntradayStrategyState> stateMap = new ConcurrentHashMap<>();

  public SakthiIntradayStrategy(
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

    SakthiIntradayStrategyState state =
        stateMap.compute(
            ticker.getTickerId(),
            (tickerId, existingState) -> {
              if (existingState != null && sessionDate.equals(existingState.getSessionDate())) {
                return existingState;
              } else {
                return new SakthiIntradayStrategyState(sessionDate);
              }
            });

    fetchPreviousTradingDayData(ticker, sessionDate, state);

    if (state.getPreviousTradingDate() != null) {
      BigDecimal previousDayHigh = state.getPreviousDayHigh();
      BigDecimal previousDayLow = state.getPreviousDayLow();
      BigDecimal currentPrice = liveQuote.getLastPrice();

      evaluateBearCase(ticker, liveQuote, state, currentPrice, previousDayLow);
      evaluateBullCase(ticker, liveQuote, state, currentPrice, previousDayHigh);
    }
  }

  private synchronized void fetchPreviousTradingDayData(
      Ticker ticker, LocalDate sessionDate, SakthiIntradayStrategyState state) {
    if (state.getPreviousTradingDate() == null) {
      LocalDate previousTradingDate =
          MarketTradingCalendar.getPreviousTradingDay(sessionDate, ticker.getCountry());

      List<DailyPrice> recentPrices =
          dailyPriceRepository.findLatestByTickerAndEndDate(
              ticker, previousTradingDate, PageRequest.of(0, 1));

      if (!recentPrices.isEmpty()) {
        DailyPrice previousDayPrice = recentPrices.getFirst();
        state.setPreviousTradingDate(previousTradingDate);
        state.setPreviousDayHigh(previousDayPrice.getPriceHigh());
        state.setPreviousDayLow(previousDayPrice.getPriceLow());
      } else {
        log.warn(
            "Skipping SakthiIntradayStrategy evaluation for {}: no prior daily prices found on or before {}",
            ticker.getTickerSymbol(),
            previousTradingDate);
      }
    }
  }

  private void evaluateBearCase(
      Ticker ticker,
      LiveQuote liveQuote,
      SakthiIntradayStrategyState state,
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
      SakthiIntradayStrategyState state,
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
    Map<String, String> variables = new HashMap<>();
    variables.put("ticker", TelegramMarkdownV2Helper.escape(ticker.getTickerSymbol()));
    variables.put("currentPrice", TelegramMarkdownV2Helper.escape(formatPrice(liveQuote.getLastPrice())));
    variables.put("previousDayLow", TelegramMarkdownV2Helper.escape(formatPrice(previousDayLow)));
    variables.put("timestamp", TelegramMarkdownV2Helper.escape(formatTimestamp(liveQuote.getQuoteTime())));
    return notificationTemplateEngine.render("notifications/bear_breach.md", variables);
  }

  public String buildBearReversalMessage(
      Ticker ticker,
      LiveQuote liveQuote,
      BigDecimal lowestPrice,
      BigDecimal previousDayLow,
      Optional<RecommendedOptionDto> recommendedOption) {
    Map<String, String> variables = new HashMap<>();
    variables.put("ticker", TelegramMarkdownV2Helper.escape(ticker.getTickerSymbol()));
    variables.put("currentPrice", TelegramMarkdownV2Helper.escape(formatPrice(liveQuote.getLastPrice())));
    variables.put("dayLow", TelegramMarkdownV2Helper.escape(formatPrice(lowestPrice)));
    variables.put("reboundPoints", TelegramMarkdownV2Helper.escape(formatPrice(REBOUND_THRESHOLD_POINTS)));
    variables.put("previousDayLow", TelegramMarkdownV2Helper.escape(formatPrice(previousDayLow)));
    variables.put("timestamp", TelegramMarkdownV2Helper.escape(formatTimestamp(liveQuote.getQuoteTime())));
    variables.put("optionDetails", buildOptionDetailsMessage(recommendedOption, OptionType.PUT));
    return notificationTemplateEngine.render("notifications/bear_reversal.md", variables);
  }

  public String buildBullBreachMessage(
      Ticker ticker, LiveQuote liveQuote, BigDecimal previousDayHigh) {
    Map<String, String> variables = new HashMap<>();
    variables.put("ticker", TelegramMarkdownV2Helper.escape(ticker.getTickerSymbol()));
    variables.put("currentPrice", TelegramMarkdownV2Helper.escape(formatPrice(liveQuote.getLastPrice())));
    variables.put("previousDayHigh", TelegramMarkdownV2Helper.escape(formatPrice(previousDayHigh)));
    variables.put("timestamp", TelegramMarkdownV2Helper.escape(formatTimestamp(liveQuote.getQuoteTime())));
    return notificationTemplateEngine.render("notifications/bull_breach.md", variables);
  }

  public String buildBullReversalMessage(
      Ticker ticker,
      LiveQuote liveQuote,
      BigDecimal highestPrice,
      BigDecimal previousDayHigh,
      Optional<RecommendedOptionDto> recommendedOption) {
    Map<String, String> variables = new HashMap<>();
    variables.put("ticker", TelegramMarkdownV2Helper.escape(ticker.getTickerSymbol()));
    variables.put("currentPrice", TelegramMarkdownV2Helper.escape(formatPrice(liveQuote.getLastPrice())));
    variables.put("dayHigh", TelegramMarkdownV2Helper.escape(formatPrice(highestPrice)));
    variables.put("pullbackPoints", TelegramMarkdownV2Helper.escape(formatPrice(REBOUND_THRESHOLD_POINTS)));
    variables.put("previousDayHigh", TelegramMarkdownV2Helper.escape(formatPrice(previousDayHigh)));
    variables.put("timestamp", TelegramMarkdownV2Helper.escape(formatTimestamp(liveQuote.getQuoteTime())));
    variables.put("optionDetails", buildOptionDetailsMessage(recommendedOption, OptionType.CALL));
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

      Map<String, String> variables = new HashMap<>();
      variables.put("action", TelegramMarkdownV2Helper.escape(action));
      variables.put("contract", TelegramMarkdownV2Helper.escape(option.tradingSymbol()));
      variables.put("strikePrice", TelegramMarkdownV2Helper.escape(formatPrice(option.strikePrice())));
      variables.put("expiry", TelegramMarkdownV2Helper.escape(expiryStr));
      variables.put("lastPrice", TelegramMarkdownV2Helper.escape(formatPrice(option.lastPrice())));
      variables.put("intrinsicValue", TelegramMarkdownV2Helper.escape(formatPrice(option.intrinsicValue())));
      variables.put("timeValue", TelegramMarkdownV2Helper.escape(formatPrice(option.timeValue())));

      if (option.delta() != null || option.impliedVolatility() != null || option.theta() != null) {
        String deltaStr = option.delta() != null ? formatPrice(option.delta()) : "N/A";
        String ivStr =
            option.impliedVolatility() != null
                ? formatPrice(option.impliedVolatility())
                : "N/A";
        String thetaStr = option.theta() != null ? formatPrice(option.theta()) : "N/A";

        variables.put("delta", TelegramMarkdownV2Helper.escape(deltaStr));
        variables.put("iv", TelegramMarkdownV2Helper.escape(ivStr));
        variables.put("theta", TelegramMarkdownV2Helper.escape(thetaStr));
      }

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
