package com.alphaflow.engine.strategies;

import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.common.enums.OptionType;
import com.alphaflow.common.utils.TelegramMarkdownV2Helper;
import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.services.NotificationService;
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
import java.util.List;
import java.util.Locale;
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
  private final ConcurrentMap<Long, SakthiIntradayStrategyState> stateMap = new ConcurrentHashMap<>();

  public SakthiIntradayStrategy(
      DailyPriceRepository dailyPriceRepository,
      NotificationService notificationService,
      NiftyOptionSelectorService niftyOptionSelectorService) {
    this.dailyPriceRepository = dailyPriceRepository;
    this.notificationService = notificationService;
    this.niftyOptionSelectorService = niftyOptionSelectorService;
  }

  public void execute(Ticker ticker, LiveQuote liveQuote) {
    LocalDate sessionDate =
        liveQuote.getQuoteTime().atZoneSameInstant(MarketConstants.IST_ZONE).toLocalDate();

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
            fetchOptionRecommendation(ticker, currentPrice, OptionType.PE, sessionDate);
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
            fetchOptionRecommendation(ticker, currentPrice, OptionType.CE, sessionDate);
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

  private String buildBearBreachMessage(
      Ticker ticker, LiveQuote liveQuote, BigDecimal previousDayLow) {
    return "*"
        + TelegramMarkdownV2Helper.escape("[BEARISH ALERT] Previous Day Low Breached")
        + "*\n"
        + "*Ticker:* "
        + TelegramMarkdownV2Helper.escape(ticker.getTickerSymbol())
        + "\n"
        + "*Current Price:* "
        + TelegramMarkdownV2Helper.escape(formatPrice(liveQuote.getLastPrice()))
        + "\n"
        + "*Previous Day Low "
        + TelegramMarkdownV2Helper.escape("(PDL)")
        + ":* "
        + TelegramMarkdownV2Helper.escape(formatPrice(previousDayLow))
        + "\n"
        + "*Time:* "
        + TelegramMarkdownV2Helper.escape(formatTimestamp(liveQuote.getQuoteTime()))
        + "\n"
        + "*Status:* "
        + TelegramMarkdownV2Helper.escape("Monitoring for 70-point reversal bounce.");
  }

  private String buildBearReversalMessage(
      Ticker ticker,
      LiveQuote liveQuote,
      BigDecimal lowestPrice,
      BigDecimal previousDayLow,
      Optional<RecommendedOptionDto> recommendedOption) {
    return "*"
        + TelegramMarkdownV2Helper.escape("[BULLISH REVERSAL] 70-Point Bounce from Low")
        + "*\n"
        + "*Ticker:* "
        + TelegramMarkdownV2Helper.escape(ticker.getTickerSymbol())
        + "\n"
        + "*Current Price:* "
        + TelegramMarkdownV2Helper.escape(formatPrice(liveQuote.getLastPrice()))
        + "\n"
        + "*Day's Low:* "
        + TelegramMarkdownV2Helper.escape(formatPrice(lowestPrice))
        + "\n"
        + "*Rebound:* "
        + TelegramMarkdownV2Helper.escape(
            "+" + formatPrice(REBOUND_THRESHOLD_POINTS) + " points from low")
        + "\n"
        + "*PDL Reference:* "
        + TelegramMarkdownV2Helper.escape(formatPrice(previousDayLow))
        + "\n"
        + "*Time:* "
        + TelegramMarkdownV2Helper.escape(formatTimestamp(liveQuote.getQuoteTime()))
        + buildOptionDetailsMessage(recommendedOption, OptionType.PE);
  }

  private String buildBullBreachMessage(
      Ticker ticker, LiveQuote liveQuote, BigDecimal previousDayHigh) {
    return "*"
        + TelegramMarkdownV2Helper.escape("[BULLISH ALERT] Previous Day High Breached")
        + "*\n"
        + "*Ticker:* "
        + TelegramMarkdownV2Helper.escape(ticker.getTickerSymbol())
        + "\n"
        + "*Current Price:* "
        + TelegramMarkdownV2Helper.escape(formatPrice(liveQuote.getLastPrice()))
        + "\n"
        + "*Previous Day High "
        + TelegramMarkdownV2Helper.escape("(PDH)")
        + ":* "
        + TelegramMarkdownV2Helper.escape(formatPrice(previousDayHigh))
        + "\n"
        + "*Time:* "
        + TelegramMarkdownV2Helper.escape(formatTimestamp(liveQuote.getQuoteTime()))
        + "\n"
        + "*Status:* "
        + TelegramMarkdownV2Helper.escape("Monitoring for 70-point pullback.");
  }

  private String buildBullReversalMessage(
      Ticker ticker,
      LiveQuote liveQuote,
      BigDecimal highestPrice,
      BigDecimal previousDayHigh,
      Optional<RecommendedOptionDto> recommendedOption) {
    return "*"
        + TelegramMarkdownV2Helper.escape("[BEARISH REVERSAL] 70-Point Pullback from High")
        + "*\n"
        + "*Ticker:* "
        + TelegramMarkdownV2Helper.escape(ticker.getTickerSymbol())
        + "\n"
        + "*Current Price:* "
        + TelegramMarkdownV2Helper.escape(formatPrice(liveQuote.getLastPrice()))
        + "\n"
        + "*Day's High:* "
        + TelegramMarkdownV2Helper.escape(formatPrice(highestPrice))
        + "\n"
        + "*Pullback:* "
        + TelegramMarkdownV2Helper.escape(
            "-" + formatPrice(REBOUND_THRESHOLD_POINTS) + " points from high")
        + "\n"
        + "*PDH Reference:* "
        + TelegramMarkdownV2Helper.escape(formatPrice(previousDayHigh))
        + "\n"
        + "*Time:* "
        + TelegramMarkdownV2Helper.escape(formatTimestamp(liveQuote.getQuoteTime()))
        + buildOptionDetailsMessage(recommendedOption, OptionType.CE);
  }

  private String buildOptionDetailsMessage(
      Optional<RecommendedOptionDto> recommendedOptionOptional, OptionType optionType) {
    if (recommendedOptionOptional.isPresent()) {
      RecommendedOptionDto option = recommendedOptionOptional.get();
      String action = optionType == OptionType.PE ? "BUY PUT" : "BUY CALL";
      String expiryStr =
          option.expiryDate().format(OPTION_EXPIRY_FORMATTER).toUpperCase(Locale.ENGLISH)
              + " ("
              + option.daysToExpiry()
              + " DTE)";

      StringBuilder messageBuilder = new StringBuilder();
      messageBuilder
          .append("\n\n*")
          .append(TelegramMarkdownV2Helper.escape("--- RECOMMENDED OPTION (Lowest TV) ---"))
          .append("*\n*Action:* ")
          .append(TelegramMarkdownV2Helper.escape(action))
          .append("\n*Contract:* ")
          .append(TelegramMarkdownV2Helper.escape(option.tradingSymbol()))
          .append("\n*Strike Price:* ")
          .append(TelegramMarkdownV2Helper.escape(formatPrice(option.strikePrice())))
          .append("\n*Expiry:* ")
          .append(TelegramMarkdownV2Helper.escape(expiryStr))
          .append("\n*Option Premium (LTP):* ")
          .append(TelegramMarkdownV2Helper.escape("₹ " + formatPrice(option.lastPrice())))
          .append("\n*Intrinsic Value:* ")
          .append(TelegramMarkdownV2Helper.escape("₹ " + formatPrice(option.intrinsicValue())))
          .append("\n*Time Value (TV):* ")
          .append(TelegramMarkdownV2Helper.escape("₹ " + formatPrice(option.timeValue())));

      if (option.delta() != null || option.impliedVolatility() != null || option.theta() != null) {
        String deltaStr = option.delta() != null ? formatPrice(option.delta()) : "N/A";
        String ivStr =
            option.impliedVolatility() != null
                ? formatPrice(option.impliedVolatility()) + "%"
                : "N/A";
        String thetaStr = option.theta() != null ? formatPrice(option.theta()) : "N/A";
        String greeksLine = "Delta: " + deltaStr + " | IV: " + ivStr + " | Theta: " + thetaStr;
        messageBuilder
            .append("\n*Greeks:* ")
            .append(TelegramMarkdownV2Helper.escape(greeksLine));
      }

      return messageBuilder.toString();
    } else {
      return "\n\n*Note:* "
          + TelegramMarkdownV2Helper.escape(
              "No option contract found meeting the ₹500–₹900 premium & 10–16 DTE criteria.");
    }
  }

  private String formatPrice(BigDecimal price) {
    return price.stripTrailingZeros().toPlainString();
  }

  private String formatTimestamp(OffsetDateTime timestamp) {
    return timestamp.atZoneSameInstant(MarketConstants.IST_ZONE).format(DATE_TIME_FORMATTER);
  }
}
