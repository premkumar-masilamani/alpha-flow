package com.alphaflow.engine.downloaders;

import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.AngelOneClient;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneCandle;
import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.DataProvider;
import com.alphaflow.persistence.repositories.IntradayPriceRepository;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AngelOneDownloader {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final AngelOneConfig config;
  private final AngelOneClient client;
  private final IntradayPriceRepository intradayPriceRepository;

  public AngelOneDownloader(
      AngelOneConfig config,
      AngelOneClient client,
      IntradayPriceRepository intradayPriceRepository) {
    this.config = config;
    this.client = client;
    this.intradayPriceRepository = intradayPriceRepository;
  }

  public boolean hasValidCredentials() {
    return config.hasValidCredentials();
  }

  public int downloadIntradayPrices() {
    if (hasValidCredentials()) {
      log.info("Starting Angel One 15-minute intraday data download process...");

      Map<Ticker, OffsetDateTime> latestSavedTimes =
          intradayPriceRepository.findLatestPriceTimesForActiveTickers(
              DataProvider.ANGEL_ONE, Timeframe._15M.getValue());

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
                config.getHolidaySet());

        OffsetDateTime latestSavedTime = latestSavedTimes.get(ticker);
        if (latestSavedTime == null || latestSavedTime.isBefore(expectedCompletedTime)) {
          int ingestedRows = downloadDataForTicker(ticker, latestSavedTime, expectedCompletedTime);
          totalNewlyIngestedRows += ingestedRows;

          try {
            Thread.sleep(config.getDelayMilliseconds());
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Angel One download process interrupted during delay.");
            break;
          }
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
    } else {
      log.debug("Angel One credentials are not configured. Skipping intraday data download.");
      return 0;
    }
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

    OffsetDateTime overallEnd = expectedCompletedTime;
    OffsetDateTime effectiveMinTime =
        (latestSavedTime != null) ? latestSavedTime : OffsetDateTime.MIN;

    String exchange = MarketConstants.EXCHANGE_NSE;
    String token = AngelOneClient.resolveToken(ticker.getTickerSymbol());
    int totalIngestedForTicker = 0;

    OffsetDateTime currentWindowStart = chunkStart;
    while (currentWindowStart.isBefore(overallEnd)) {
      OffsetDateTime candidateWindowEnd = currentWindowStart.plusDays(config.getMaxChunkDays());
      OffsetDateTime currentWindowEnd =
          candidateWindowEnd.isBefore(overallEnd) ? candidateWindowEnd : overallEnd;

      String fromDateStr = currentWindowStart.format(DATE_TIME_FORMATTER);
      String toDateStr = currentWindowEnd.format(DATE_TIME_FORMATTER);

      log.info(
          "Syncing 15m candles for {} (token: {}) from {} to {}",
          ticker.getTickerSymbol(),
          token,
          fromDateStr,
          toDateStr);

      List<AngelOneCandle> candles =
          client.getCandleData(exchange, token, Timeframe._15M, fromDateStr, toDateStr);

      if (!candles.isEmpty()) {
        List<IntradayPrice> toSave = new ArrayList<>();
        for (AngelOneCandle candle : candles) {
          if (candle.timestamp().isAfter(effectiveMinTime)
              && !candle.timestamp().isAfter(overallEnd)) {
            toSave.add(
                IntradayPrice.builder()
                    .ticker(ticker)
                    .timeframe(Timeframe._15M.getValue())
                    .priceTime(candle.timestamp())
                    .priceOpen(candle.open())
                    .priceHigh(candle.high())
                    .priceLow(candle.low())
                    .priceClose(candle.close())
                    .volume(candle.volume())
                    .build());
          }
        }

        if (!toSave.isEmpty()) {
          intradayPriceRepository.saveAll(toSave);
          effectiveMinTime = toSave.getLast().getPriceTime();
          totalIngestedForTicker += toSave.size();
          log.info(
              "Successfully synced {} new 15-minute candles for {}",
              toSave.size(),
              ticker.getTickerSymbol());
        } else {
          log.info(
              "No new 15-minute data points to save for ticker {} in chunk {} to {}.",
              ticker.getTickerSymbol(),
              fromDateStr,
              toDateStr);
        }
      } else {
        log.info(
            "No candle data returned for {} in chunk {} to {}.",
            ticker.getTickerSymbol(),
            fromDateStr,
            toDateStr);
      }

      currentWindowStart = currentWindowEnd;
      if (currentWindowStart.isBefore(overallEnd)) {
        try {
          Thread.sleep(config.getDelayMilliseconds());
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          log.warn("Download process interrupted during chunk delay.");
          break;
        }
      }
    }

    return totalIngestedForTicker;
  }
}
