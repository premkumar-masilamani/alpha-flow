package com.alphaflow.engine.downloaders;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import com.alphaflow.infrastructure.entities.MarketData;
import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.enums.DataSource;
import com.alphaflow.infrastructure.repositories.MarketDataRepository;
import com.alphaflow.infrastructure.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static com.alphaflow.infrastructure.constants.AppConstants.DB_MATH_CONTEXT;

@Service
public class YahooFinanceDataDownloader {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceDataDownloader.class);

    private final YahooFinanceConfig yahooFinanceConfig;
    private final TickerRepository tickerRepository;
    private final MarketDataRepository marketDataRepository;

    public YahooFinanceDataDownloader(
            YahooFinanceConfig yahooFinanceConfig,
            TickerRepository tickerRepository,
            MarketDataRepository marketDataRepository
    ) {
        this.yahooFinanceConfig = yahooFinanceConfig;
        this.tickerRepository = tickerRepository;
        this.marketDataRepository = marketDataRepository;
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

        // We only sync until yesterday to avoid partial daily bars.
        LocalDate endDate = LocalDate.now().minusDays(1);

        if (startDate.isAfter(endDate)) {
            log.info("Ticker {} is already up to date (last sync: {}).", ticker.getTickerSymbol(), startDate.minusDays(1));
            return;
        }

        log.info("Syncing Yahoo Finance data for {} from {} to {}", ticker.getTickerSymbol(), startDate, endDate);

        long startTs = startDate.atStartOfDay(ZoneId.of("UTC")).toEpochSecond();
        long endTs = endDate.atStartOfDay(ZoneId.of("UTC")).toEpochSecond() + 86399; // End of day

        String url = yahooFinanceConfig.getDownloadUrl()
                .replace("{symbol}", ticker.getTickerSymbol())
                .replace("{start}", String.valueOf(startTs))
                .replace("{end}", String.valueOf(endTs));

        try {
            Table table = downloadAndParseCsv(url);
            saveMarketData(ticker, table);
            log.info("Successfully synced {} rows for {}", table.rowCount(), ticker.getTickerSymbol());
        } catch (Exception e) {
            log.error("Failed to download Yahoo Finance data for {}: {}", ticker.getTickerSymbol(), e.getMessage());
        }
    }

    private Table downloadAndParseCsv(String url) throws IOException {
        URLConnection connection = URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        // Add a realistic User-Agent to avoid 401/403 errors
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        connection.setRequestProperty("Accept", "text/csv,text/plain,application/csv");

        try (InputStream in = connection.getInputStream()) {
            return Table.read().csv(CsvReadOptions.builder(in)
                    .header(true)
                    .missingValueIndicator("null")
                    .build());
        }
    }

    private void saveMarketData(Ticker ticker, Table table) {
        List<MarketData> toSave = new ArrayList<>();
        for (Row row : table) {
            try {
                LocalDate date = row.getDate("Date");
                BigDecimal open = BigDecimal.valueOf(row.getDouble("Open"));
                BigDecimal high = BigDecimal.valueOf(row.getDouble("High"));
                BigDecimal low = BigDecimal.valueOf(row.getDouble("Low"));
                BigDecimal close = BigDecimal.valueOf(row.getDouble("Close"));
                BigDecimal volume = BigDecimal.valueOf(row.getDouble("Volume"));

                BigDecimal vwapOHLC4 = open.add(high, DB_MATH_CONTEXT)
                        .add(low, DB_MATH_CONTEXT)
                        .add(close, DB_MATH_CONTEXT)
                        .divide(BigDecimal.valueOf(4), DB_MATH_CONTEXT);

                BigDecimal vwapHLC3 = high.add(low, DB_MATH_CONTEXT)
                        .add(close, DB_MATH_CONTEXT)
                        .divide(BigDecimal.valueOf(3), DB_MATH_CONTEXT);

                MarketData marketData = MarketData.builder()
                        .ticker(ticker)
                        .marketDataDate(date)
                        .priceOpen(open)
                        .priceHigh(high)
                        .priceLow(low)
                        .priceClose(close)
                        .volume(volume)
                        .vwapOHLC4(vwapOHLC4)
                        .vwapHLC3(vwapHLC3)
                        .build();

                toSave.add(marketData);
            } catch (Exception e) {
                log.warn("Failed to process row for ticker {}: {}", ticker.getTickerSymbol(), e.getMessage());
            }
        }
        if (!toSave.isEmpty()) {
            marketDataRepository.saveAll(toSave);
        }
    }
}
