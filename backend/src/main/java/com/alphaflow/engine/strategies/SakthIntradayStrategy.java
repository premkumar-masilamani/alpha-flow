package com.alphaflow.engine.strategies;

import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.common.utils.TelegramMarkdownV2Helper;
import com.alphaflow.engine.services.NotificationService;
import com.alphaflow.engine.strategies.model.TickerStrategyState;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SakthIntradayStrategy {

  public static final BigDecimal REBOUND_THRESHOLD_POINTS = new BigDecimal("70");
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final DailyPriceRepository dailyPriceRepository;
  private final NotificationService notificationService;
  private final ConcurrentMap<Long, TickerStrategyState> stateMap = new ConcurrentHashMap<>();

  public SakthIntradayStrategy(
      DailyPriceRepository dailyPriceRepository, NotificationService notificationService) {
    this.dailyPriceRepository = dailyPriceRepository;
    this.notificationService = notificationService;
  }

  public void evaluateAndNotify(Ticker ticker, LiveQuote liveQuote) {
    LocalDate sessionDate =
        liveQuote.getQuoteTime().atZoneSameInstant(MarketConstants.IST_ZONE).toLocalDate();

    TickerStrategyState state =
        stateMap.compute(
            ticker.getTickerId(),
            (tickerId, existingState) -> {
              if (existingState != null && sessionDate.equals(existingState.getSessionDate())) {
                return existingState;
              } else {
                return new TickerStrategyState(sessionDate);
              }
            });

    LocalDate previousTradingDateLimit = sessionDate.minusDays(1);
    List<DailyPrice> recentPrices =
        dailyPriceRepository.findLatestByTickerAndEndDate(
            ticker, previousTradingDateLimit, PageRequest.of(0, 1));

    if (!recentPrices.isEmpty()) {
      DailyPrice previousDayPrice = recentPrices.getFirst();
      BigDecimal previousDayHigh = previousDayPrice.getPriceHigh();
      BigDecimal previousDayLow = previousDayPrice.getPriceLow();
      BigDecimal currentPrice = liveQuote.getLastPrice();

      evaluateBearCase(ticker, liveQuote, state, currentPrice, previousDayLow);
      evaluateBullCase(ticker, liveQuote, state, currentPrice, previousDayHigh);
    } else {
      log.warn(
          "Skipping SakthIntradayStrategy evaluation for {}: no prior daily prices found before {}",
          ticker.getTickerSymbol(),
          previousTradingDateLimit);
    }
  }

  private void evaluateBearCase(
      Ticker ticker,
      LiveQuote liveQuote,
      TickerStrategyState state,
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
        dispatchNotification(
            buildBearReversalMessage(
                ticker, liveQuote, state.getBearLowestPrice(), previousDayLow));
        state.setBearReversalNotified(true);
      }
    }
  }

  private void evaluateBullCase(
      Ticker ticker,
      LiveQuote liveQuote,
      TickerStrategyState state,
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
        dispatchNotification(
            buildBullReversalMessage(
                ticker, liveQuote, state.getBullHighestPrice(), previousDayHigh));
        state.setBullReversalNotified(true);
      }
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
    return "🚨 *"
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
      Ticker ticker, LiveQuote liveQuote, BigDecimal lowestPrice, BigDecimal previousDayLow) {
    return "🟢 *"
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
        + TelegramMarkdownV2Helper.escape(formatTimestamp(liveQuote.getQuoteTime()));
  }

  private String buildBullBreachMessage(
      Ticker ticker, LiveQuote liveQuote, BigDecimal previousDayHigh) {
    return "🚨 *"
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
      Ticker ticker, LiveQuote liveQuote, BigDecimal highestPrice, BigDecimal previousDayHigh) {
    return "🔴 *"
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
        + TelegramMarkdownV2Helper.escape(formatTimestamp(liveQuote.getQuoteTime()));
  }

  private String formatPrice(BigDecimal price) {
    return price.stripTrailingZeros().toPlainString();
  }

  private String formatTimestamp(OffsetDateTime timestamp) {
    return timestamp.atZoneSameInstant(MarketConstants.IST_ZONE).format(DATE_TIME_FORMATTER);
  }
}
