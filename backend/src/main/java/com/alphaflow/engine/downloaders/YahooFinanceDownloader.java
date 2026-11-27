package com.alphaflow.engine.downloaders;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.PipelineGating;
import com.alphaflow.engine.configs.PipelineConfig;
import com.alphaflow.engine.configs.YahooFinanceConfig;
import com.alphaflow.engine.downloaders.yahoofinance.YahooFinanceClient;
import com.alphaflow.engine.pipeline.PipelineJob;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class YahooFinanceDownloader implements PipelineJob {

  private final YahooFinanceClient yahooFinanceClient;
  private final DailyPriceRepository dailyPriceRepository;
  private final AtomicLong nextAllowedTimeMs = new AtomicLong(0);

  @Autowired
  public YahooFinanceDownloader(
      YahooFinanceClient yahooFinanceClient, DailyPriceRepository dailyPriceRepository) {
    this.yahooFinanceClient = yahooFinanceClient;
    this.dailyPriceRepository = dailyPriceRepository;
  }

  @Override
  public String getJobName() {
    return PipelineConfig.STEP_DOWNLOAD_DAILY_PRICE;
  }

  @Override
  public boolean shouldRun(Timeframe timeframe) {
    Map<Ticker, LocalDate> latestSavedDates =
        dailyPriceRepository.findLatestPriceDatesForActiveTickers();
    Instant now = Instant.now();
    for (Map.Entry<Ticker, LocalDate> entry : latestSavedDates.entrySet()) {
      Ticker ticker = entry.getKey();
      LocalDate latestSavedDate = entry.getValue();
      LocalDate expectedTradingDate =
          PipelineGating.getLatestExpectedTradingDate(ticker.getCountry(), now);
      if (latestSavedDate == null || latestSavedDate.isBefore(expectedTradingDate)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void execute(Timeframe timeframe) {
    downloadAndPersistDailyPrices();
  }

  public int downloadAndPersistDailyPrices() {
    log.info("Starting Yahoo Finance data download process...");

    Map<Ticker, LocalDate> latestSavedDates =
        dailyPriceRepository.findLatestPriceDatesForActiveTickers();
    log.info("Found {} active Yahoo Finance tickers to sync.", latestSavedDates.size());

    Instant now = Instant.now();
    int totalNewlyIngestedRows = 0;
    for (Map.Entry<Ticker, LocalDate> entry : latestSavedDates.entrySet()) {
      Ticker ticker = entry.getKey();
      LocalDate latestSavedDate = entry.getValue();
      LocalDate expectedTradingDate =
          PipelineGating.getLatestExpectedTradingDate(ticker.getCountry(), now);

      if (latestSavedDate == null || latestSavedDate.isBefore(expectedTradingDate)) {
        throttle();
        int ingestedRows =
            downloadAndPersistForTicker(ticker, latestSavedDate, expectedTradingDate);
        totalNewlyIngestedRows += ingestedRows;
      }
    }
    log.info(
        "All Yahoo Finance downloads completed! Total newly ingested rows: {}",
        totalNewlyIngestedRows);
    return totalNewlyIngestedRows;
  }

  private void throttle() {
    long minDelayMs = YahooFinanceConfig.DELAY_MILLISECONDS;
    long now = System.currentTimeMillis();
    long scheduledTime =
        nextAllowedTimeMs.updateAndGet(current -> Math.max(current, now) + minDelayMs);
    long waitMs = scheduledTime - minDelayMs - now;
    if (waitMs <= 0) {
      log.debug("No rate limit throttle required");
    } else {
      log.debug("Rate limit throttle active: sleeping {} ms before next request", waitMs);
      try {
        Thread.sleep(waitMs);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        log.warn("Rate limiter sleep interrupted: {}", exception.getMessage());
      }
    }
  }

  private int downloadAndPersistForTicker(
      Ticker ticker, LocalDate latestSavedDate, LocalDate expectedTradingDate) {
    LocalDate actualLatestDate =
        (latestSavedDate != null) ? latestSavedDate : YahooFinanceConfig.BACKFILL_START_DATE;
    LocalDate startDate = actualLatestDate.plusDays(1);

    long startTs = startDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
    long endTs =
        Math.max(
            Instant.now().getEpochSecond(),
            expectedTradingDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond());

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
