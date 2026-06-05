package com.alphaflow.engine.downloaders;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class YahooFinanceDownloader {

  private static final Logger log = LoggerFactory.getLogger(YahooFinanceDownloader.class);
  private static final LocalDate DEFAULT_DATE = LocalDate.of(1900, 1, 1);

  private final YahooFinanceConfig yahooFinanceConfig;
  private final DailyPriceRepository dailyPriceRepository;
  private final YahooResponseParser yahooResponseParser;

  public YahooFinanceDownloader(
      YahooFinanceConfig yahooFinanceConfig,
      DailyPriceRepository dailyPriceRepository,
      YahooResponseParser yahooResponseParser) {
    this.yahooFinanceConfig = yahooFinanceConfig;
    this.dailyPriceRepository = dailyPriceRepository;
    this.yahooResponseParser = yahooResponseParser;
  }

  public void download() {
    log.info("Starting Yahoo Finance data download process...");

    Map<Ticker, LocalDate> latestSavedDates =
        dailyPriceRepository.findLatestPriceDatesForActiveTickers();

    log.info("Found {} active Yahoo Finance tickers to sync.", latestSavedDates.size());

    List<Ticker> tickers = new ArrayList<>(latestSavedDates.keySet());
    for (int i = 0; i < tickers.size(); i++) {
      Ticker ticker = tickers.get(i);
      downloadDataForTicker(ticker, latestSavedDates.get(ticker));

      // Throttle between requests to avoid rate-limiting; stop early if interrupted.
      if (i < tickers.size() - 1 && !pauseBetweenRequests()) {
        break;
      }
    }

    log.info("All Yahoo Finance downloads completed!");
  }

  /**
   * Sleeps for the configured inter-request delay to throttle calls to Yahoo Finance.
   *
   * @return {@code false} if the thread was interrupted (the caller should stop), {@code true}
   *     otherwise.
   */
  private boolean pauseBetweenRequests() {
    long delayMilliseconds = yahooFinanceConfig.getDelayMilliseconds();
    if (delayMilliseconds <= 0) {
      return true;
    }
    try {
      Thread.sleep(delayMilliseconds);
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Download process interrupted during delay.");
      return false;
    }
  }

  private void downloadDataForTicker(Ticker ticker, LocalDate latestSavedDate) {
    LocalDate startDate = latestSavedDate != null ? latestSavedDate.plusDays(1) : DEFAULT_DATE;

    long startTs = startDate.atStartOfDay(ZoneId.of("UTC")).toEpochSecond();
    long endTs = Instant.now().truncatedTo(ChronoUnit.DAYS).getEpochSecond();

    if (startTs >= endTs) {
      log.info(
          "Ticker {} is already up to date (last sync: {}).",
          ticker.getTickerSymbol(),
          startDate.minusDays(1));
      return;
    }

    log.info(
        "Syncing Yahoo Finance data for {} from {} (timestamp: {}) to start of today (timestamp: {})",
        ticker.getTickerSymbol(),
        startDate,
        startTs,
        endTs);

    String encodedSymbol = URLEncoder.encode(ticker.getTickerSymbol(), StandardCharsets.UTF_8);
    String url =
        yahooFinanceConfig
            .getDownloadUrl()
            .replace("{symbol}", encodedSymbol)
            .replace("{start}", String.valueOf(startTs))
            .replace("{end}", String.valueOf(endTs));

    try {
      String json = downloadData(url);
      List<DailyPrice> dailyPriceList = yahooResponseParser.parse(json, ticker);
      if (!dailyPriceList.isEmpty()) {
        List<DailyPrice> newDailyPriceData =
            dailyPriceList.stream()
                .filter(c -> latestSavedDate == null || c.getPriceDate().isAfter(latestSavedDate))
                .collect(Collectors.toList());

        if (!newDailyPriceData.isEmpty()) {
          dailyPriceRepository.saveAll(newDailyPriceData);
          log.info(
              "Successfully synced {} rows for {}",
              newDailyPriceData.size(),
              ticker.getTickerSymbol());
        } else {
          log.info(
              "No new data points to save for ticker {} (all downloaded points already exist).",
              ticker.getTickerSymbol());
        }
      }
    } catch (Exception e) {
      log.error("Failed to download Yahoo Finance data for {}", ticker.getTickerSymbol(), e);
    }
  }

  private String downloadData(String url) throws IOException {
    URLConnection connection = URI.create(url).toURL().openConnection();
    connection.setConnectTimeout(10000);
    connection.setReadTimeout(10000);
    // Add a realistic User-Agent to avoid 401/403 errors
    connection.setRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
    connection.setRequestProperty("Accept", "application/json");

    try (InputStream in = connection.getInputStream()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
