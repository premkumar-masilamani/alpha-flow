package com.alphaflow.engine.schedulers;

import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.AngelOneClient;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneQuote;
import com.alphaflow.engine.services.NotificationService;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.enums.DataProvider;
import com.alphaflow.persistence.repositories.LiveQuoteRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LiveQuoteScheduler {

  private final AngelOneConfig angelOneConfig;
  private final AngelOneClient angelOneClient;
  private final TickerRepository tickerRepository;
  private final LiveQuoteRepository liveQuoteRepository;
  private final NotificationService notificationService;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public LiveQuoteScheduler(
      AngelOneConfig angelOneConfig,
      AngelOneClient angelOneClient,
      TickerRepository tickerRepository,
      LiveQuoteRepository liveQuoteRepository,
      NotificationService notificationService) {
    this.angelOneConfig = angelOneConfig;
    this.angelOneClient = angelOneClient;
    this.tickerRepository = tickerRepository;
    this.liveQuoteRepository = liveQuoteRepository;
    this.notificationService = notificationService;
  }

  @Scheduled(cron = "0 * 9-15 * * MON-FRI", zone = MarketConstants.TIMEZONE_KOLKATA)
  public void runScheduledLiveQuoteUpdate() {
    log.info("Starting scheduled Angel One 1-minute live quote pipeline...");
    run(ZonedDateTime.now(MarketConstants.IST_ZONE));
  }

  public void run(ZonedDateTime currentMarketTime) {
    if (isMarketSessionOpen(currentMarketTime)) {
      if (running.compareAndSet(false, true)) {
        try {
          executePipeline(currentMarketTime.toOffsetDateTime());
        } catch (Exception exception) {
          log.error("Error occurred during Angel One live quote pipeline execution", exception);
        } finally {
          running.set(false);
        }
      } else {
        log.warn("Live quote pipeline skipped: a previous run is still in progress.");
      }
    } else {
      log.debug(
          "Skipping live quote pipeline: Indian market session is closed at {}.",
          currentMarketTime);
    }
  }

  private boolean isMarketSessionOpen(ZonedDateTime currentMarketTime) {
    LocalTime currentTime = currentMarketTime.toLocalTime();
    return MarketTradingCalendar.isTradingDay(currentMarketTime.toLocalDate(), Country.IN)
        && !currentTime.isBefore(angelOneConfig.getSessionStartTime())
        && !currentTime.isAfter(angelOneConfig.getSessionEndTime());
  }

  private void executePipeline(OffsetDateTime currentTimestamp) {
    List<Ticker> activeTickers =
        tickerRepository.findByIsActiveTrueAndCountryAndDataProvider(
            Country.IN, DataProvider.ANGEL_ONE);
    for (Ticker ticker : activeTickers) {
      Optional<LiveQuote> persistedQuoteOptional = fetchAndPersistQuote(ticker, currentTimestamp);
      if (persistedQuoteOptional.isPresent()) {
        LiveQuote persistedQuote = persistedQuoteOptional.get();
        evaluateAlertConditions(ticker, persistedQuote);
        dispatchAlertNotifications(ticker, persistedQuote);
      } else {
        log.warn(
            "Skipping alert and notification tasks: no live quote fetched for {}",
            ticker.getTickerSymbol());
      }
    }
  }

  private Optional<LiveQuote> fetchAndPersistQuote(Ticker ticker, OffsetDateTime currentTimestamp) {
    String symbolToken = AngelOneClient.resolveToken(ticker.getTickerSymbol());
    Optional<AngelOneQuote> marketQuoteOptional =
        angelOneClient.getMarketQuote(MarketConstants.EXCHANGE_NSE, symbolToken);
    if (marketQuoteOptional.isPresent()) {
      AngelOneQuote marketQuote = marketQuoteOptional.get();
      OffsetDateTime quoteTime = parseQuoteTimestamp(marketQuote.timestamp(), currentTimestamp);
      Optional<LiveQuote> existingQuoteOptional = liveQuoteRepository.findByTicker(ticker);
      LiveQuote liveQuote;
      if (existingQuoteOptional.isPresent()) {
        liveQuote = existingQuoteOptional.get();
        liveQuote.setLastPrice(marketQuote.lastPrice());
        liveQuote.setQuoteTime(quoteTime);
        liveQuote.setUpdatedAt(currentTimestamp);
      } else {
        liveQuote =
            LiveQuote.builder()
                .ticker(ticker)
                .lastPrice(marketQuote.lastPrice())
                .volume(BigDecimal.ZERO)
                .quoteTime(quoteTime)
                .createdAt(currentTimestamp)
                .updatedAt(currentTimestamp)
                .build();
      }
      return Optional.of(liveQuoteRepository.save(liveQuote));
    } else {
      return Optional.empty();
    }
  }

  private OffsetDateTime parseQuoteTimestamp(
      String rawTimestamp, OffsetDateTime fallbackTimestamp) {
    try {
      return OffsetDateTime.parse(rawTimestamp);
    } catch (DateTimeParseException exception) {
      log.debug(
          "Falling back to pipeline timestamp {} for quote timestamp '{}'",
          fallbackTimestamp,
          rawTimestamp);
      return fallbackTimestamp;
    }
  }

  private void evaluateAlertConditions(Ticker ticker, LiveQuote liveQuote) {
    log.debug(
        "Evaluating alert conditions for {}: price={} at {}",
        ticker.getTickerSymbol(),
        liveQuote.getLastPrice(),
        liveQuote.getQuoteTime());
  }

  private void dispatchAlertNotifications(Ticker ticker, LiveQuote liveQuote) {
    log.debug(
        "Placeholder notification check for {}: price={} via {}",
        ticker.getTickerSymbol(),
        liveQuote.getLastPrice(),
        notificationService.getClass().getSimpleName());
  }
}
