package com.prem.ta.core;

import com.prem.ta.configs.AppConfig;
import com.prem.ta.entities.File;
import com.prem.ta.entities.MarketData;
import com.prem.ta.models.OHLCVMetrics;
import com.prem.ta.models.OrderFlowMetrics;
import com.prem.ta.models.VolumeProfileMetrics;
import com.prem.ta.repositories.FileRepository;
import com.prem.ta.repositories.MarketDataRepository;
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

import static com.prem.ta.configs.Constants.*;
import static tech.tablesaw.io.csv.CsvReadOptions.builder;

@Service
public class MarketDataComputationService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataComputationService.class);

    private final AppConfig appConfig;
    private final FileRepository fileRepository;
    private final MarketDataRepository marketDataRepository;
    private final TechnicalAnalysisEngine technicalAnalysisEngine;

    public MarketDataComputationService(
            AppConfig appConfig,
            FileRepository fileRepository,
            MarketDataRepository marketDataRepository,
            TechnicalAnalysisEngine technicalAnalysisEngine
    ) {
        this.appConfig = appConfig;
        this.fileRepository = fileRepository;
        this.marketDataRepository = marketDataRepository;
        this.technicalAnalysisEngine = technicalAnalysisEngine;
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

            // Group files by Ticker and Date within the page to ensure we process each (ticker, date) group sequentially.
            // This prevents race conditions where multiple threads might try to insert the same MarketData record
            // for different files belonging to the same ticker and date.
            var groupedFiles = page.getContent().stream()
                    .collect(java.util.stream.Collectors.groupingBy(f -> f.getTicker().getTickerId() + "-" + f.getFileDate()));

            // Process each (ticker, date) group in parallel, but files within a group sequentially.
            groupedFiles.values().parallelStream().forEach(filesInGroup -> {
                for (File file : filesInGroup) {
                    processTickDataFile(file);
                }
            });

            totalProcessed += page.getNumberOfElements();
        }

        log.info("Completed Market Data Computation. Total files processed: {}", totalProcessed);
    }

    /**
     * Processes a single tick data file:
     * 1. Locates the ZIP file on disk.
     * 2. Extracts the CSV content.
     * 3. Computes technical metrics.
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

                log.trace("Computing technical metrics for {}", baseFileName);
                MarketData computedData = computeMetrics(file, tickTable);

                // Attempt to find and merge with existing market data.
                // We use a retry mechanism to handle potential race conditions during parallel inserts.
                MarketData mergedData = null;
                int retryCount = 0;
                while (retryCount < 3) {
                    try {
                        mergedData = marketDataRepository.findByTickerAndMarketDataDate(file.getTicker(), file.getFileDate())
                                .map(existingData -> {
                                    log.debug("Existing market data found for {} on {}. Merging metrics.", tickerSymbol, dateStr);
                                    return existingData.merge(computedData);
                                })
                                .orElse(computedData);

                        log.debug("Saving market data: {}", mergedData);
                        marketDataRepository.save(mergedData);
                        break; // Success
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        retryCount++;
                        log.warn("Data integrity violation for {} on {} (retry {}/3). Likely a concurrent insert race condition.", tickerSymbol, dateStr, retryCount);
                        if (retryCount >= 3) throw e;
                    }
                }

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

        OHLCVMetrics ohlcvMetrics = technicalAnalysisEngine.computeOHLCV(table);
        OrderFlowMetrics orderFlowMetrics = technicalAnalysisEngine.computeOrderFlow(table);
        VolumeProfileMetrics volumeProfileMetrics = technicalAnalysisEngine.computeVolumeProfile(table, ohlcvMetrics);

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
