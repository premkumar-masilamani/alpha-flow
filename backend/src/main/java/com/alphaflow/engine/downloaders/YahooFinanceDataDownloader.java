package com.alphaflow.engine.downloaders;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import com.alphaflow.infrastructure.entities.MarketData;
import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.enums.DataSource;
import com.alphaflow.infrastructure.repositories.MarketDataRepository;
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
import java.util.List;

import static com.alphaflow.infrastructure.constants.AppConstants.DB_MATH_CONTEXT;

@Service
public class YahooFinanceDataDownloader {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceDataDownloader.class);

    private final YahooFinanceConfig yahooFinanceConfig;
    private final TickerRepository tickerRepository;
    private final MarketDataRepository marketDataRepository;
    private final ObjectMapper objectMapper;

    public YahooFinanceDataDownloader(
            YahooFinanceConfig yahooFinanceConfig,
            TickerRepository tickerRepository,
            MarketDataRepository marketDataRepository,
            ObjectMapper objectMapper
    ) {
        this.yahooFinanceConfig = yahooFinanceConfig;
        this.tickerRepository = tickerRepository;
        this.marketDataRepository = marketDataRepository;
        this.objectMapper = objectMapper;
    }

    public void download() {
        log.info("Starting Yahoo Finance data download process...");

        var activeTickers = tickerRepository.findByIsActiveTrueAndSource(DataSource.YAHOO_FINANCE);
        log.info("Found {} active Yahoo Finance tickers to sync.", activeTickers.size());

        activeTickers.forEach(this::downloadDataForTicker);

        log.info("All Yahoo Finance downloads completed!");
    }

    private void downloadDataForTicker(Ticker ticker) {
        LocalDate startDate = marketDataRepository.findTopByTickerOrderByMarketDataDateDesc(ticker)
                .map(md -> md.getMarketDataDate().plusDays(1))
                .orElse(ticker.getTickerDate());

        long startTs = startDate.atStartOfDay(ZoneId.of("UTC")).toEpochSecond();
        // Use the start of today (UTC) as the end timestamp.
        // This ensures the API only returns completed daily bars from previous days,
        // as the current day's bar timestamp will be at market open (later than 00:00 UTC).
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
            List<MarketData> data = fetchAndParseJson(url, ticker);
            if (!data.isEmpty()) {
                marketDataRepository.saveAll(data);
                log.info("Successfully synced {} rows for {}", data.size(), ticker.getTickerSymbol());
            }
        } catch (Exception e) {
            log.error("Failed to download Yahoo Finance data for {}: {}", ticker.getTickerSymbol(), e.getMessage());
        }
    }

    private List<MarketData> fetchAndParseJson(String url, Ticker ticker) throws IOException {
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

            List<MarketData> list = new ArrayList<>();
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

                list.add(MarketData.builder()
                        .ticker(ticker)
                        .marketDataDate(date)
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
