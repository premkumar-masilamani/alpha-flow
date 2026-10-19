package com.alphaflow.engine.downloaders;

import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.common.enums.IntradayTimeframe;
import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.AngelOneClient;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneCandle;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneQuote;
import com.alphaflow.engine.schedulers.SchedulerUtils;
import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.enums.DataProvider;
import com.alphaflow.persistence.repositories.IntradayPriceRepository;
import com.alphaflow.persistence.repositories.LiveQuoteRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AngelOneDownloader {

  private final AngelOneConfig config;
  private final AngelOneClient client;
  private final IntradayPriceRepository intradayPriceRepository;
  private final TickerRepository tickerRepository;
  private final LiveQuoteRepository liveQuoteRepository;

  public AngelOneDownloader(
      AngelOneConfig config,
      AngelOneClient client,
      IntradayPriceRepository intradayPriceRepository,
      TickerRepository tickerRepository,
      LiveQuoteRepository liveQuoteRepository) {
    this.config = config;
    this.client = client;
    this.intradayPriceRepository = intradayPriceRepository;
    this.tickerRepository = tickerRepository;
    this.liveQuoteRepository = liveQuoteRepository;
  }

  public int downloadIntradayPrices() {
    log.info("Starting Angel One 15-minute intraday data download process...");

    Map<Ticker, OffsetDateTime> latestSavedTimes =
        intradayPriceRepository.findLatestPriceTimesForActiveTickers(
            DataProvider.ANGEL_ONE, IntradayTimeframe._15M.getValue());

    log.info("Found {} active Angel One tickers to sync.", latestSavedTimes.size());

    int totalNewlyIngestedRows = 0;
    List<Ticker> tickers = new ArrayList<>(latestSavedTimes.keySet());
    for (Ticker ticker : tickers) {
      ZoneId marketZone = ticker.getCountry().getZoneId();
      OffsetDateTime expectedCompletedTime =
          MarketTradingCalendar.getExpectedLatestCompletedCandleTime(
              ZonedDateTime.now(marketZone),
              config.getSessionStartTime(),
              config.getSessionEndTime(),
              15,
              ticker.getCountry());

      OffsetDateTime latestSavedTime = latestSavedTimes.get(ticker);
      if (latestSavedTime == null || latestSavedTime.isBefore(expectedCompletedTime)) {
        int ingestedRows = downloadDataForTicker(ticker, latestSavedTime, expectedCompletedTime);
        totalNewlyIngestedRows += ingestedRows;
      } else {
        log.info(
            "{}: up to date (latest saved: {}, expected: {}). Skipping.",
            ticker.getTickerSymbol(),
            latestSavedTime,
            expectedCompletedTime);
      }
    }

    log.info(
        "All Angel One 15-minute downloads completed! Total newly ingested rows: {}",
        totalNewlyIngestedRows);
    return totalNewlyIngestedRows;
  }

  private int downloadDataForTicker(
      Ticker ticker, OffsetDateTime latestSavedTime, OffsetDateTime expectedCompletedTime) {
    OffsetDateTime chunkStart;
    if (latestSavedTime != null) {
      chunkStart = latestSavedTime;
    } else {
      chunkStart =
          config
              .getBackfillStartDate()
              .atTime(config.getSessionStartTime())
              .atZone(ticker.getCountry().getZoneId())
              .toOffsetDateTime();
    }

    OffsetDateTime effectiveMinTime =
        (latestSavedTime != null) ? latestSavedTime : OffsetDateTime.MIN;

    String exchange = MarketConstants.EXCHANGE_NSE;
    String token = AngelOneClient.resolveToken(ticker.getTickerSymbol());
    int totalIngestedForTicker = 0;

    List<AngelOneCandle> candles =
        client.fetch15mCandles(exchange, token, chunkStart, expectedCompletedTime);

    if (!candles.isEmpty()) {
      Map<OffsetDateTime, IntradayPrice> toSaveMap = new LinkedHashMap<>();
      for (AngelOneCandle candle : candles) {
        if (candle.timestamp().isAfter(effectiveMinTime)
            && !candle.timestamp().isAfter(expectedCompletedTime)) {
          toSaveMap.putIfAbsent(
              candle.timestamp(),
              IntradayPrice.builder()
                  .ticker(ticker)
                  .timeframe(IntradayTimeframe._15M.getValue())
                  .priceTime(candle.timestamp())
                  .priceOpen(candle.open())
                  .priceHigh(candle.high())
                  .priceLow(candle.low())
                  .priceClose(candle.close())
                  .volume(candle.volume())
                  .build());
        }
      }

      List<IntradayPrice> toSave = new ArrayList<>(toSaveMap.values());
      if (!toSave.isEmpty()) {
        intradayPriceRepository.saveAll(toSave);
        totalIngestedForTicker = toSave.size();
        log.info(
            "Successfully synced {} new 15-minute candles for {}",
            toSave.size(),
            ticker.getTickerSymbol());
      } else {
        log.info(
            "No new 15-minute data points to save for ticker {} from {} to {}.",
            ticker.getTickerSymbol(),
            chunkStart,
            expectedCompletedTime);
      }
    } else {
      log.info(
          "No candle data returned for {} from {} to {}.",
          ticker.getTickerSymbol(),
          chunkStart,
          expectedCompletedTime);
    }

    return totalIngestedForTicker;
  }

  /**
   * Fetches and persists live quotes for all active Angel One Indian tickers.
   *
   * @param currentTimestamp current pipeline timestamp
   * @return list of newly saved or updated live quotes
   */
  public List<LiveQuote> downloadLiveQuotes(OffsetDateTime currentTimestamp) {
    log.info("Starting Angel One live quote download process...");
    List<Ticker> activeTickers =
        tickerRepository.findByIsActiveTrueAndCountryAndDataProvider(
            Country.IN, DataProvider.ANGEL_ONE);
    List<LiveQuote> savedQuotes = new ArrayList<>();
    for (Ticker ticker : activeTickers) {
      Optional<LiveQuote> liveQuoteOptional = downloadLiveQuoteForTicker(ticker, currentTimestamp);
      if (liveQuoteOptional.isPresent()) {
        savedQuotes.add(liveQuoteOptional.get());
      } else {
        log.warn("Skipping live quote update: no quote returned for {}", ticker.getTickerSymbol());
      }
    }
    log.info(
        "Angel One live quote download completed. Persisted {} quotes.", savedQuotes.size());
    return savedQuotes;
  }

  /**
   * Fetches and persists a single live quote for the specified ticker.
   *
   * @param ticker           ticker to fetch quote for
   * @param currentTimestamp fallback timestamp to associate with quote
   * @return persisted LiveQuote if fetched, empty otherwise
   */
  public Optional<LiveQuote> downloadLiveQuoteForTicker(
      Ticker ticker, OffsetDateTime currentTimestamp) {
    String symbolToken = AngelOneClient.resolveToken(ticker.getTickerSymbol());
    Optional<AngelOneQuote> marketQuoteOptional =
        client.getMarketQuote(MarketConstants.EXCHANGE_NSE, symbolToken);
    if (marketQuoteOptional.isPresent()) {
      AngelOneQuote marketQuote = marketQuoteOptional.get();
      OffsetDateTime quoteTime =
          SchedulerUtils.parseQuoteTimestamp(marketQuote.timestamp(), currentTimestamp);
      Optional<LiveQuote> existingQuoteOptional = liveQuoteRepository.findByTicker(ticker);
      LiveQuote liveQuote;
      if (existingQuoteOptional.isPresent()) {
        liveQuote = existingQuoteOptional.get();
        liveQuote.setTicker(ticker);
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
                .updatedAt(currentTimestamp)
                .build();
      }
      LiveQuote savedQuote = liveQuoteRepository.save(liveQuote);
      savedQuote.setTicker(ticker);
      return Optional.of(savedQuote);
    } else {
      return Optional.empty();
    }
  }
}
