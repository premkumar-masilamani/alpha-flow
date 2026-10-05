package com.alphaflow.engine.downloaders;

import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.YahooFinanceConfig;
import com.alphaflow.engine.downloaders.yahoofinance.YahooResponseParser;
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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class YahooFinanceDownloader {

  private final YahooFinanceConfig yahooFinanceConfig;
  private final DailyPriceRepository dailyPriceRepository;

  public YahooFinanceDownloader(
      YahooFinanceConfig yahooFinanceConfig, DailyPriceRepository dailyPriceRepository) {
    this.yahooFinanceConfig = yahooFinanceConfig;
    this.dailyPriceRepository = dailyPriceRepository;
  }

  public int downloadDailyPrices() {
    log.info("Starting Yahoo Finance data download process...");

    Map<Ticker, LocalDate> latestSavedDates =
        dailyPriceRepository.findLatestPriceDatesForActiveTickers();

    log.info("Found {} active Yahoo Finance tickers to sync.", latestSavedDates.size());

    int totalNewlyIngestedRows = 0;
    List<Ticker> tickers = new ArrayList<>(latestSavedDates.keySet());
    for (Ticker ticker : tickers) {
      ZoneId marketZone = ticker.getCountry().getZoneId();

      LocalDate expectedTradingDate =
          MarketTradingCalendar.getExpectedLatestTradingDate(
              ZonedDateTime.now(marketZone),
              yahooFinanceConfig.getMarketCutoffTime(),
              yahooFinanceConfig.getHolidaySet());

      LocalDate latestSavedDate = latestSavedDates.get(ticker);
      if (latestSavedDate == null || latestSavedDate.isBefore(expectedTradingDate)) {
        int ingestedRows =
            downloadDataForTicker(ticker, latestSavedDate, expectedTradingDate, marketZone);
        totalNewlyIngestedRows += ingestedRows;

        try {
          Thread.sleep(yahooFinanceConfig.getDelayMilliseconds());
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          log.warn("Download process interrupted during delay.");
          break;
        }
      } else {
        log.debug(
            "{}: up to date (latest saved: {}, expected: {}). Skipping.",
            ticker.getTickerSymbol(),
            latestSavedDate,
            expectedTradingDate);
      }
    }
    log.info(
        "All Yahoo Finance downloads completed! Total newly ingested rows: {}",
        totalNewlyIngestedRows);
    return totalNewlyIngestedRows;
  }

  private int downloadDataForTicker(
      Ticker ticker, LocalDate latestSavedDate, LocalDate expectedTradingDate, ZoneId marketZone) {
    LocalDate actualLatestDate =
        (latestSavedDate != null) ? latestSavedDate : LocalDate.of(1900, 1, 1);
    LocalDate startDate = actualLatestDate.plusDays(1);

    long startTs = startDate.atStartOfDay(ZoneId.of("UTC")).toEpochSecond();
    long endTs =
        Math.max(
            Instant.now().getEpochSecond(),
            expectedTradingDate.plusDays(1).atStartOfDay(marketZone).toEpochSecond());

    log.info(
        "Syncing {} from {} (timestamp: {}) to timestamp: {}",
        ticker.getTickerSymbol(),
        startDate,
        Instant.ofEpochSecond(startTs),
        Instant.ofEpochSecond(endTs));

    String encodedSymbol = URLEncoder.encode(ticker.getTickerSymbol(), StandardCharsets.UTF_8);
    String url =
        yahooFinanceConfig
            .getDownloadUrl()
            .replace("{symbol}", encodedSymbol)
            .replace("{start}", String.valueOf(startTs))
            .replace("{end}", String.valueOf(endTs));

    try {
      String json = downloadData(url);
      List<DailyPrice> dailyPriceList = YahooResponseParser.parse(json, ticker);
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
    } catch (Exception exception) {
      log.error(
          "Failed to download Yahoo Finance data for {}: {}",
          ticker.getTickerSymbol(),
          exception.getMessage(),
          exception);
      return 0;
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
