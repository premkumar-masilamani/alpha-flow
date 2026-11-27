package com.alphaflow.engine.downloaders;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.PipelineGating;
import com.alphaflow.engine.configs.PipelineConfig;
import com.alphaflow.engine.configs.YahooFinanceConfig;
import com.alphaflow.engine.downloaders.yahoofinance.YahooFinanceClient;
import com.alphaflow.engine.pipeline.PipelineJob;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class YahooFinanceDownloader implements PipelineJob {

  private final YahooFinanceClient yahooFinanceClient;
  private final DailyPriceRepository dailyPriceRepository;
  private final TickerRepository tickerRepository;
  private final AtomicLong nextAllowedTimeMs = new AtomicLong(0);

  @Autowired
  public YahooFinanceDownloader(
      YahooFinanceClient yahooFinanceClient,
      DailyPriceRepository dailyPriceRepository,
      TickerRepository tickerRepository) {
    this.yahooFinanceClient = yahooFinanceClient;
    this.dailyPriceRepository = dailyPriceRepository;
    this.tickerRepository = tickerRepository;
  }

  @Override
  public String getJobName() {
    return PipelineConfig.STEP_DOWNLOAD_DAILY_PRICE;
  }

  @Override
  public boolean shouldRun(Timeframe timeframe) {
    Instant now = Instant.now();
    LocalDate usExpected = PipelineGating.getLatestExpectedTradingDate(Country.US, now);
    LocalDate inExpected = PipelineGating.getLatestExpectedTradingDate(Country.IN, now);
    return dailyPriceRepository.hasPendingDailyPrices(usExpected, inExpected);
  }

  @Override
  public void execute(Timeframe timeframe) {
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

    List<DailyPrice> recentStoredBars =
        dailyPriceRepository.findLatestByTickerAndEndDate(
            ticker, actualLatestDate, PageRequest.of(0, YahooFinanceConfig.OVERLAP_BARS));
    LocalDate startDate =
        recentStoredBars.isEmpty()
            ? actualLatestDate.plusDays(1)
            : recentStoredBars.getLast().getPriceDate();

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
      if (!hasPriceDrift(ticker, recentStoredBars, dailyPriceList)) {
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
        ticker.setStale(true);
        tickerRepository.save(ticker);
        log.warn(
            "Marked ticker {} as stale due to detected price drift; skipped saving new daily bars.",
            ticker.getTickerSymbol());
        return 0;
      }
    } else {
      log.info(
          "No price data returned from Yahoo Finance for ticker {}.", ticker.getTickerSymbol());
      return 0;
    }
  }

  private boolean hasPriceDrift(
      Ticker ticker, List<DailyPrice> recentStoredBars, List<DailyPrice> fetchedBars) {
    Map<LocalDate, BigDecimal> storedCloseByDate = new HashMap<>();
    for (DailyPrice storedBar : recentStoredBars) {
      storedCloseByDate.put(storedBar.getPriceDate(), storedBar.getPriceClose());
    }

    for (DailyPrice fetchedBar : fetchedBars) {
      BigDecimal storedClose = storedCloseByDate.get(fetchedBar.getPriceDate());
      if (storedClose != null && storedClose.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal roundedFetchedClose =
            fetchedBar
                .getPriceClose()
                .setScale(YahooFinanceConfig.PRICE_SCALE, RoundingMode.HALF_UP);
        BigDecimal relativeDiff =
            roundedFetchedClose
                .subtract(storedClose)
                .abs()
                .divide(storedClose, YahooFinanceConfig.DRIFT_MATH_CONTEXT);
        if (relativeDiff.compareTo(YahooFinanceConfig.DRIFT_THRESHOLD) > 0) {
          log.warn(
              "Price drift detected for {} on {}: storedClose={}, fetchedClose={}, relativeDiff={}",
              ticker.getTickerSymbol(),
              fetchedBar.getPriceDate(),
              storedClose,
              roundedFetchedClose,
              relativeDiff);
          return true;
        }
      }
    }
    return false;
  }
}
