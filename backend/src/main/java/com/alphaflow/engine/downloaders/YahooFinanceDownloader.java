package com.alphaflow.engine.downloaders;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.configs.PipelineConfig;
import com.alphaflow.engine.configs.YahooFinanceConfig;
import com.alphaflow.engine.downloaders.yahoofinance.YahooFinanceClient;
import com.alphaflow.engine.pipeline.PipelineJob;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    for (Map.Entry<Ticker, LocalDate> entry : latestSavedDates.entrySet()) {
      Ticker ticker = entry.getKey();
      LocalDate today = LocalDate.now(ticker.getCountry().getZoneId());
      LocalDate latestSavedDate = entry.getValue();
      if (latestSavedDate == null || latestSavedDate.isBefore(today)) {
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

    List<Ticker> staleTickers = new ArrayList<>();
    for (Map.Entry<Ticker, LocalDate> entry : latestSavedDates.entrySet()) {
      Ticker ticker = entry.getKey();
      LocalDate today = LocalDate.now(ticker.getCountry().getZoneId());
      LocalDate latestSavedDate = entry.getValue();

      if (latestSavedDate == null || latestSavedDate.isBefore(today)) {
        staleTickers.add(ticker);
      } else {
        log.debug(
            "{}: up to date (source={}, calculated={}). Skipping.",
            ticker.getTickerSymbol(),
            today,
            latestSavedDate);
      }
    }

    int totalActiveTickers = latestSavedDates.size();
    int staleCount = staleTickers.size();
    if (staleCount > 0) {
      log.info(
          "Yahoo Finance daily download: {} of {} active tickers require processing (skipping {} up-to-date tickers).",
          staleCount,
          totalActiveTickers,
          totalActiveTickers - staleCount);

      int totalNewlyIngestedRows = 0;
      for (Ticker ticker : staleTickers) {
        ZoneId marketZone = ticker.getCountry().getZoneId();
        LocalDate today = LocalDate.now(marketZone);
        LocalDate latestSavedDate = latestSavedDates.get(ticker);

        throttle();
        int ingestedRows = downloadAndPersistForTicker(ticker, latestSavedDate, today, marketZone);
        totalNewlyIngestedRows += ingestedRows;
      }
      log.info(
          "All Yahoo Finance downloads completed! Total newly ingested rows: {}",
          totalNewlyIngestedRows);
      return totalNewlyIngestedRows;
    } else {
      log.info(
          "Yahoo Finance daily download: 0 of {} active tickers require processing; skipping step.",
          totalActiveTickers);
      return 0;
    }
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
      Ticker ticker, LocalDate latestSavedDate, LocalDate today, ZoneId marketZone) {
    LocalDate actualLatestDate =
        (latestSavedDate != null) ? latestSavedDate : YahooFinanceConfig.BACKFILL_START_DATE;
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
