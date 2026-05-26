package com.alphaflow.engine.downloaders;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import com.alphaflow.infrastructure.entities.DailyCandleData;
import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.repositories.DailyCandleDataRepository;
import com.alphaflow.infrastructure.repositories.TickerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLConnection;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class YahooFinanceDownloader {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceDownloader.class);
    private static final LocalDate DEFAULT_LATEST_CANDLE_DATE = LocalDate.of(1900, 1, 1);

    private final YahooFinanceConfig yahooFinanceConfig;
    private final TickerRepository tickerRepository;
    private final DailyCandleDataRepository dailyCandleDataRepository;
    private final ObjectMapper objectMapper;

    public YahooFinanceDownloader(
            YahooFinanceConfig yahooFinanceConfig,
            TickerRepository tickerRepository,
            DailyCandleDataRepository dailyCandleDataRepository,
            ObjectMapper objectMapper
    ) {
        this.yahooFinanceConfig = yahooFinanceConfig;
        this.tickerRepository = tickerRepository;
        this.dailyCandleDataRepository = dailyCandleDataRepository;
        this.objectMapper = objectMapper;
    }

    public void download() {
        log.info("Starting Yahoo Finance data download process...");

        var tickers = tickerRepository.findAll();
        log.info("Found {} Yahoo Finance tickers to sync.", tickers.size());

        ZoneId utcZone = ZoneId.of("UTC");
        Map<Long, LocalDate> latestSavedDatesByTickerId = dailyCandleDataRepository.findLatestCandleDatesForAllTickers(
                        DEFAULT_LATEST_CANDLE_DATE
                ).stream()
                .collect(Collectors.toMap(
                        DailyCandleDataRepository.TickerLatestCandleDateView::getTickerId,
                        DailyCandleDataRepository.TickerLatestCandleDateView::getLatestCandleDate,
                        (left, right) -> left
                ));

        for (int i = 0; i < tickers.size(); i++) {
            Ticker ticker = tickers.get(i);
            downloadDataForTicker(ticker, latestSavedDatesByTickerId.get(ticker.getTickerId()), utcZone);

            if (i < tickers.size() - 1 && yahooFinanceConfig.getDelayMs() > 0) {
                try {
                    Thread.sleep(yahooFinanceConfig.getDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Download process interrupted during delay.");
                    break;
                }
            }
        }

        log.info("All Yahoo Finance downloads completed!");
    }

    private void downloadDataForTicker(Ticker ticker, LocalDate latestSavedDate, ZoneId utcZone) {
        LocalDate startDate = latestSavedDate != null
                ? latestSavedDate.plusDays(1)
                : ticker.getTickerDate();

        long startTs = startDate.atStartOfDay(utcZone).toEpochSecond();
        long endTs = Instant.now().truncatedTo(ChronoUnit.DAYS).getEpochSecond();

        if (startTs >= endTs) {
            log.info("Ticker {} is already up to date (last sync: {}).", ticker.getTickerSymbol(), startDate.minusDays(1));
            return;
        }

        log.info("Syncing Yahoo Finance data for {} from {} (timestamp: {}) to start of today (timestamp: {})",
                ticker.getTickerSymbol(), startDate, startTs, endTs);

        String url = yahooFinanceConfig.getDownloadUrl()
                .replace("{symbol}", ticker.getTickerSymbol())
                .replace("{start}", String.valueOf(startTs))
                .replace("{end}", String.valueOf(endTs));

        try {
            List<DailyCandleData> candleDataList = fetchAndParseJson(url, ticker);
            if (!candleDataList.isEmpty()) {
                LocalDate checkStartDate = latestSavedDate != null
                        ? latestSavedDate.minusDays(7)
                        : ticker.getTickerDate();
                List<LocalDate> existingDates = dailyCandleDataRepository.findDatesByTickerAndDateGreaterThanEqual(ticker, checkStartDate);
                Set<LocalDate> existingDatesSet = new HashSet<>(existingDates);

                List<DailyCandleData> newCandleData = candleDataList.stream()
                        .filter(c -> !existingDatesSet.contains(c.getCandleDataDate()))
                        .collect(Collectors.toList());

                if (!newCandleData.isEmpty()) {
                    dailyCandleDataRepository.saveAll(newCandleData);
                    log.info("Successfully synced {} rows for {}", newCandleData.size(), ticker.getTickerSymbol());
                } else {
                    log.info("No new data points to save for ticker {} (all downloaded points already exist).", ticker.getTickerSymbol());
                }
            }
        } catch (Exception e) {
            log.error("Failed to download Yahoo Finance data for {}: {}", ticker.getTickerSymbol(), e.getMessage());
        }
    }

    private List<DailyCandleData> fetchAndParseJson(String url, Ticker ticker) throws IOException {
        URLConnection connection = URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        // Add a realistic User-Agent to avoid 401/403 errors
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        connection.setRequestProperty("Accept", "application/json");

        try (InputStream in = connection.getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            JsonNode result = root.path("chart").path("result").get(0);
            if (result == null || result.isNull()) {
                return List.of();
            }

            JsonNode timestamps = result.path("timestamp");
            JsonNode indicators = result.path("indicators").path("quote").get(0);
            if (timestamps.isMissingNode() || indicators.isMissingNode()) {
                return List.of();
            }

            JsonNode opens = indicators.path("open");
            JsonNode highs = indicators.path("high");
            JsonNode lows = indicators.path("low");
            JsonNode closes = indicators.path("close");
            JsonNode volumes = indicators.path("volume");

            List<DailyCandleData> list = new ArrayList<>();
            for (int i = 0; i < timestamps.size(); i++) {
                if (opens.get(i).isNull() || highs.get(i).isNull() || lows.get(i).isNull() || closes.get(i).isNull()) {
                    continue;
                }

                LocalDate date = Instant.ofEpochSecond(timestamps.get(i).asLong())
                        .atZone(ZoneId.of("UTC")).toLocalDate();

                BigDecimal open = opens.get(i).decimalValue();
                BigDecimal high = highs.get(i).decimalValue();
                BigDecimal low = lows.get(i).decimalValue();
                BigDecimal close = closes.get(i).decimalValue();
                BigDecimal volume = volumes.get(i).decimalValue();

                list.add(DailyCandleData.builder()
                        .ticker(ticker)
                        .candleDataDate(date)
                        .priceOpen(open)
                        .priceHigh(high)
                        .priceLow(low)
                        .priceClose(close)
                        .volume(volume)
                        .build());
            }
            return list;
        }
    }
}
