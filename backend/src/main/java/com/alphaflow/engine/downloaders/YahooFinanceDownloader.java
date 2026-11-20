package com.alphaflow.engine.downloaders;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import com.alphaflow.engine.downloaders.yahoofinance.YahooFinanceClient;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class YahooFinanceDownloader {

  private final YahooFinanceConfig yahooFinanceConfig;
  private final YahooFinanceClient yahooFinanceClient;
  private final DailyPriceRepository dailyPriceRepository;

  @Autowired
  public YahooFinanceDownloader(
      YahooFinanceConfig yahooFinanceConfig,
      YahooFinanceClient yahooFinanceClient,
      DailyPriceRepository dailyPriceRepository) {
    this.yahooFinanceConfig = yahooFinanceConfig;
    this.yahooFinanceClient = yahooFinanceClient;
    this.dailyPriceRepository = dailyPriceRepository;
  }

  public int downloadAndPersistDailyPrices() {
    log.info("Starting Yahoo Finance data download process...");

    Map<Ticker, LocalDate> latestSavedDates =
        dailyPriceRepository.findLatestPriceDatesForActiveTickers();

    log.info("Found {} active Yahoo Finance tickers to sync.", latestSavedDates.size());

    int totalNewlyIngestedRows = 0;
    List<Ticker> tickers = new ArrayList<>(latestSavedDates.keySet());
    for (Ticker ticker : tickers) {
      ZoneId marketZone = ticker.getCountry().getZoneId();
      LocalDate today = LocalDate.now(marketZone);
      LocalDate latestSavedDate = latestSavedDates.get(ticker);

      if (latestSavedDate == null || latestSavedDate.isBefore(today)) {
        int ingestedRows =
            downloadAndPersistForTicker(ticker, latestSavedDate, today, marketZone);
        totalNewlyIngestedRows += ingestedRows;
      } else {
        log.debug(
            "{}: up to date (latest saved: {}, today: {}). Skipping.",
            ticker.getTickerSymbol(),
            latestSavedDate,
            today);
      }
    }
    log.info(
        "All Yahoo Finance downloads completed! Total newly ingested rows: {}",
        totalNewlyIngestedRows);
    return totalNewlyIngestedRows;
  }

  private int downloadAndPersistForTicker(
      Ticker ticker, LocalDate latestSavedDate, LocalDate today, ZoneId marketZone) {
    LocalDate actualLatestDate =
        (latestSavedDate != null) ? latestSavedDate : yahooFinanceConfig.getBackfillStartDate();
    LocalDate startDate = actualLatestDate.plusDays(1);

    long startTs = startDate.atStartOfDay(ZoneId.of("UTC")).toEpochSecond();
    long endTs =
        Math.max(
            Instant.now().getEpochSecond(),
            today.plusDays(1).atStartOfDay(marketZone).toEpochSecond());

    log.info(
        "Syncing {} from {} (timestamp: {}) to timestamp: {}",
        ticker.getTickerSymbol(),
        startDate,
        Instant.ofEpochSecond(startTs),
        Instant.ofEpochSecond(endTs));

    List<DailyPrice> dailyPriceList = yahooFinanceClient.fetchDailyPrices(ticker, startTs, endTs);
    if (!dailyPriceList.isEmpty()) {
      List<DailyPrice> newDailyPriceData =
          dailyPriceList.stream()
              .filter(candle -> candle.getPriceDate().isAfter(actualLatestDate))
              .collect(Collectors.toList());

      if (!newDailyPriceData.isEmpty()) {
        dailyPriceRepository.saveAll(newDailyPriceData);
        log.info(
            "Successfully synced {} rows for {}",
            newDailyPriceData.size(),
            ticker.getTickerSymbol());
        return newDailyPriceData.size();
      } else {
        log.info(
            "No new data points to save for ticker {} (all downloaded points already exist).",
            ticker.getTickerSymbol());
        return 0;
      }
    } else {
      log.info(
          "No price data returned from Yahoo Finance for ticker {}.", ticker.getTickerSymbol());
      return 0;
    }
  }
}
