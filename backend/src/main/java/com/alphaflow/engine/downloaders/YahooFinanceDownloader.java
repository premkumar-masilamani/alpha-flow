package com.alphaflow.engine.downloaders;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class YahooFinanceDownloader {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceDownloader.class);
    private static final LocalDate DEFAULT_LATEST_CANDLE_DATE = LocalDate.of(1900, 1, 1);

    private final YahooFinanceConfig yahooFinanceConfig;
    private final TickerRepository tickerRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final ObjectMapper objectMapper;

    public YahooFinanceDownloader(
            YahooFinanceConfig yahooFinanceConfig,
            TickerRepository tickerRepository,
            DailyPriceRepository dailyPriceRepository,
            ObjectMapper objectMapper
    ) {
        this.yahooFinanceConfig = yahooFinanceConfig;
        this.tickerRepository = tickerRepository;
        this.dailyPriceRepository = dailyPriceRepository;
        this.objectMapper = objectMapper;
    }

    public void download() {
        log.info("Starting Yahoo Finance data download process...");

        var tickers = tickerRepository.findByIsActiveTrue();
        log.info("Found {} active Yahoo Finance tickers to sync.", tickers.size());

        ZoneId utcZone = ZoneId.of("UTC");
        Map<Long, LocalDate> latestSavedDatesByTickerId = dailyPriceRepository.findLatestPriceDatesForAllTickers(
                        DEFAULT_LATEST_CANDLE_DATE
                ).stream()
                .collect(Collectors.toMap(
                        DailyPriceRepository.TickerLatestPriceDateView::getTickerId,
                        DailyPriceRepository.TickerLatestPriceDateView::getLatestPriceDate,
                        (left, right) -> left
                ));

        for (int i = 0; i < tickers.size(); i++) {
            Ticker ticker = tickers.get(i);
            downloadDataForTicker(ticker, latestSavedDatesByTickerId.get(ticker.getTickerId()), utcZone);

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
     * @return {@code false} if the thread was interrupted (the caller should stop), {@code true} otherwise.
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

    private void downloadDataForTicker(Ticker ticker, LocalDate latestSavedDate, ZoneId utcZone) {
        LocalDate startDate = latestSavedDate != null
                ? latestSavedDate.plusDays(1)
                : DEFAULT_LATEST_CANDLE_DATE;

        long startTs = startDate.atStartOfDay(utcZone).toEpochSecond();
        long endTs = Instant.now().truncatedTo(ChronoUnit.DAYS).getEpochSecond();

        if (startTs >= endTs) {
            log.info("Ticker {} is already up to date (last sync: {}).", ticker.getTickerSymbol(), startDate.minusDays(1));
            return;
        }

        log.info("Syncing Yahoo Finance data for {} from {} (timestamp: {}) to start of today (timestamp: {})",
                ticker.getTickerSymbol(), startDate, startTs, endTs);

        String encodedSymbol = URLEncoder.encode(ticker.getTickerSymbol(), StandardCharsets.UTF_8);
        String url = yahooFinanceConfig.getDownloadUrl()
                .replace("{symbol}", encodedSymbol)
                .replace("{start}", String.valueOf(startTs))
                .replace("{end}", String.valueOf(endTs));

        try {
            List<DailyPrice> candleDataList = fetchAndParseJson(url, ticker);
            if (!candleDataList.isEmpty()) {
                LocalDate checkStartDate = latestSavedDate != null
                        ? latestSavedDate.minusDays(7)
                        : DEFAULT_LATEST_CANDLE_DATE;
                List<LocalDate> existingDates = dailyPriceRepository.findDatesByTickerAndDateGreaterThanEqual(ticker, checkStartDate);
                Set<LocalDate> existingDatesSet = new HashSet<>(existingDates);

                List<DailyPrice> newCandleData = candleDataList.stream()
                        .filter(c -> !existingDatesSet.contains(c.getPriceDate()))
                        .collect(Collectors.toList());

                if (!newCandleData.isEmpty()) {
                    dailyPriceRepository.saveAll(newCandleData);
                    log.info("Successfully synced {} rows for {}", newCandleData.size(), ticker.getTickerSymbol());
                } else {
                    log.info("No new data points to save for ticker {} (all downloaded points already exist).", ticker.getTickerSymbol());
                }
            }
        } catch (Exception e) {
            log.error("Failed to download Yahoo Finance data for {}", ticker.getTickerSymbol(), e);
        }
    }

    private List<DailyPrice> fetchAndParseJson(String url, Ticker ticker) throws IOException {
        URLConnection connection = URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        // Add a realistic User-Agent to avoid 401/403 errors
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        connection.setRequestProperty("Accept", "application/json");

        InputStream in = connection.getInputStream();
        try {
            JsonNode root = objectMapper.readTree(in);
            JsonNode result = root.path("chart").path("result").get(0);
            if (result == null || result.isNull()) {
                return List.of();
            }

            JsonNode timestamps = result.path("timestamp");
            JsonNode indicators = result.path("indicators").path("quote").path(0);
            if (timestamps.isMissingNode() || indicators.isMissingNode()) {
                return List.of();
            }

            JsonNode opens = indicators.path("open");
            JsonNode highs = indicators.path("high");
            JsonNode lows = indicators.path("low");
            JsonNode closes = indicators.path("close");
            JsonNode volumes = indicators.path("volume");

            if (!opens.isArray() || !highs.isArray() || !lows.isArray() || !closes.isArray() || !volumes.isArray()) {
                log.warn("Ticker {}: Yahoo response is missing one or more OHLCV arrays; skipping.", ticker.getTickerSymbol());
                return List.of();
            }

            // Bound the loop by the shortest array so a truncated/uneven payload cannot cause an index-out-of-range / NPE.
            int count = Math.min(timestamps.size(),
                    Math.min(Math.min(opens.size(), highs.size()), Math.min(lows.size(), Math.min(closes.size(), volumes.size()))));

            List<DailyPrice> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                // Skip any row with a null/missing OHLCV component rather than coercing it to a misleading value.
                if (opens.get(i).isNull() || highs.get(i).isNull() || lows.get(i).isNull()
                        || closes.get(i).isNull() || volumes.get(i).isNull()) {
                    continue;
                }

                LocalDate date = Instant.ofEpochSecond(timestamps.get(i).asLong())
                        .atZone(ZoneId.of("UTC")).toLocalDate();

                BigDecimal open = opens.get(i).decimalValue();
                BigDecimal high = highs.get(i).decimalValue();
                BigDecimal low = lows.get(i).decimalValue();
                BigDecimal close = closes.get(i).decimalValue();
                long volume = volumes.get(i).asLong();

                list.add(DailyPrice.builder()
                        .ticker(ticker)
                        .priceDate(date)
                        .priceOpen(open)
                        .priceHigh(high)
                        .priceLow(low)
                        .priceClose(close)
                        .volume(volume)
                        .build());
            }
            return list;
        } finally {
            in.close();
        }
    }
}
