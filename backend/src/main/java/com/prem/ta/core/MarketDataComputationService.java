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

    public void compute() {
        log.info("Computing Market Data from Tick Data...");

        while (true) {
            Page<File> page = fileRepository.findByIsProcessedFalse(PageRequest.of(0, DB_QUERY_PAGE_SIZE));

            if (page.isEmpty()) {
                break;
            }

            log.info("Processing {} pending files...", page.getNumberOfElements());
            page.getContent().forEach(this::processTickDataFile);
        }

        log.info("Completed Market Data Computation...");
    }

    public void processTickDataFile(File file) {

        final String tickerSymbol = file.getTicker().getTickerSymbol();
        final String dateStr = getBinanceDateString(file.getFileDate());
        final String baseFileName = getBinanceZipFileName(tickerSymbol, dateStr);
        final Path filePath = Paths.get(appConfig.getDownloadDir(), tickerSymbol, baseFileName);

        log.info("Processing trades for {} on {}", tickerSymbol, dateStr);

        if (!Files.exists(filePath)) {
            log.warn("File not found: {}", filePath);
            return;
        }

        try (ZipFile zipFile = new ZipFile(filePath.toFile())) {

            ZipEntry entry = zipFile.stream()
                    .findFirst()
                    .orElse(null);

            if (entry == null) {
                log.warn("Empty ZIP for {} on {}", tickerSymbol, dateStr);
                return;
            }

            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                MarketData computedData = computeMetrics(file, getFileAsTable(inputStream));
                MarketData mergedData = marketDataRepository.findByTickerAndMarketDataDate(file.getTicker(), file.getFileDate())
                        .map(existingData -> existingData.merge(computedData))
                        .orElse(computedData);

                log.debug(mergedData.toString());
                marketDataRepository.save(mergedData);

                file.setIsProcessed(true);
                fileRepository.save(file);

                log.info("Processed file {} for {}", baseFileName, tickerSymbol);
            }

        } catch (Exception e) {
            log.error("Failed to process {} on {}", tickerSymbol, dateStr, e);
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
