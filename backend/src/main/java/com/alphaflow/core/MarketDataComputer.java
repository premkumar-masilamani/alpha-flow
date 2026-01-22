package com.alphaflow.core;

import com.alphaflow.infrastructure.config.AppConfig;
import com.alphaflow.infrastructure.persistence.entities.File;
import com.alphaflow.infrastructure.persistence.entities.MarketData;
import com.alphaflow.infrastructure.persistence.repositories.FileRepository;
import com.alphaflow.infrastructure.persistence.repositories.MarketDataRepository;
import com.alphaflow.domain.model.OHLCVMetrics;
import com.alphaflow.domain.model.OrderFlowMetrics;
import com.alphaflow.domain.model.VolumeProfileMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tech.tablesaw.api.Table;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.alphaflow.infrastructure.config.Constants.*;
import static tech.tablesaw.io.csv.CsvReadOptions.builder;

@Service
public class MarketDataComputer {

    private static final Logger log = LoggerFactory.getLogger(MarketDataComputer.class);

    private final AppConfig appConfig;
    private final FileRepository fileRepository;
    private final MarketDataRepository marketDataRepository;
    private final MetricsComputer metricsComputer;

    public MarketDataComputer(
            AppConfig appConfig,
            FileRepository fileRepository,
            MarketDataRepository marketDataRepository,
            MetricsComputer metricsComputer
    ) {
        this.appConfig = appConfig;
        this.fileRepository = fileRepository;
        this.marketDataRepository = marketDataRepository;
        this.metricsComputer = metricsComputer;
    }

    /**
     * Entry point for computing market data.
     * Iterates through all unprocessed files in the database, extracts tick data from ZIP archives,
     * computes OHLCV, Order Flow, and Volume Profile metrics, and persists the results.
     */
    public void compute() {
        log.info("Starting Market Data Computation from Tick Data...");

        int totalProcessed = 0;
        while (true) {
            // Fetch a page of unprocessed files to avoid loading too many records into memory
            Page<File> page = fileRepository.findByIsProcessedFalse(PageRequest.of(0, DB_QUERY_PAGE_SIZE));

            if (page.isEmpty()) {
                break;
            }

            log.info("Processing page with {} pending files...", page.getNumberOfElements());

            // Process files sequentially to avoid OutOfMemoryError.
            // Binance tick data files can be large, and loading multiple tables into memory concurrently
            // can exceed available heap space.
            page.getContent().forEach(this::processTickDataFile);

            totalProcessed += page.getNumberOfElements();
        }

        log.info("Completed Market Data Computation. Total files processed: {}", totalProcessed);
    }

    /**
     * Processes a single tick data file:
     * 1. Locates the ZIP file on disk.
     * 2. Extracts the CSV content.
     * 3. Computes metrics.
     * 4. Merges with existing market data if applicable (to handle multiple files for the same date/ticker).
     * 5. Updates the file status to processed.
     *
     * @param file The file record from the database.
     */
    public void processTickDataFile(File file) {
        final String tickerSymbol = file.getTicker().getTickerSymbol();
        final String dateStr = getBinanceDateString(file.getFileDate());
        final String baseFileName = getBinanceZipFileName(tickerSymbol, dateStr);
        final Path filePath = Paths.get(appConfig.getDownloadDir(), tickerSymbol, baseFileName);

        log.debug("Processing trades for ticker: {}, date: {}, file: {}", tickerSymbol, dateStr, baseFileName);

        if (!Files.exists(filePath)) {
            log.warn("File not found on disk: {}. Skipping processing for this file.", filePath);
            return;
        }

        try (ZipFile zipFile = new ZipFile(filePath.toFile())) {
            // Binance ZIPs typically contain a single CSV file
            ZipEntry entry = zipFile.stream()
                    .findFirst()
                    .orElse(null);

            if (entry == null) {
                log.warn("ZIP archive is empty for {} on {}. Path: {}", tickerSymbol, dateStr, filePath);
                return;
            }

            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                log.trace("Reading CSV data from ZIP for {}", baseFileName);
                Table tickTable = getFileAsTable(inputStream);

                log.trace("Computing metrics for {}", baseFileName);
                MarketData computedData = computeMetrics(file, tickTable);

                // If we already have market data for this ticker/date, merge it.
                MarketData mergedData = marketDataRepository.findByTickerAndMarketDataDate(file.getTicker(), file.getFileDate())
                        .map(existingData -> {
                            log.debug("Existing market data found for {} on {}. Merging metrics.", tickerSymbol, dateStr);
                            return existingData.merge(computedData);
                        })
                        .orElse(computedData);

                log.debug("Saving market data: {}", mergedData);
                marketDataRepository.save(mergedData);

                // Mark the file as processed to avoid re-computation
                file.setIsProcessed(true);
                fileRepository.save(file);

                log.info("Successfully processed and saved data for {} on {}", tickerSymbol, dateStr);
            }

        } catch (Exception e) {
            log.error("Failed to process tick data for ticker: {}, date: {}. Error: {}", tickerSymbol, dateStr, e.getMessage(), e);
        }
    }

    private Table getFileAsTable(InputStream inputStream) {
        return Table.read()
                .csv(builder(inputStream).header(false)
                        .columnTypes(BINANCE_TICK_DATA_SCHEMA)
                        .build()
                );
    }

    private MarketData computeMetrics(File file, Table table) {

        OHLCVMetrics ohlcvMetrics = metricsComputer.computeOHLCV(table);
        OrderFlowMetrics orderFlowMetrics = metricsComputer.computeOrderFlow(table);
        VolumeProfileMetrics volumeProfileMetrics = metricsComputer.computeVolumeProfile(table, ohlcvMetrics);

        MarketData marketData = new MarketData();
        marketData.setTicker(file.getTicker());
        marketData.setMarketDataDate(file.getFileDate());

        marketData.setPriceOpen(ohlcvMetrics.open());
        marketData.setPriceHigh(ohlcvMetrics.high());
        marketData.setPriceLow(ohlcvMetrics.low());
        marketData.setPriceClose(ohlcvMetrics.close());

        marketData.setVolume(ohlcvMetrics.volume());
        marketData.setVwap(ohlcvMetrics.vwap());
        marketData.setVolumeProfilePOC(volumeProfileMetrics.pointOfControl());
        marketData.setVolumeProfileVAH(volumeProfileMetrics.valueAreaHigh());
        marketData.setVolumeProfileVAL(volumeProfileMetrics.valueAreaLow());

        marketData.setBuyerVolumeShare(orderFlowMetrics.buyerVolumeShare());
        marketData.setBuyerCapitalShare(orderFlowMetrics.buyerCapitalShare());

        return marketData;
    }

}
