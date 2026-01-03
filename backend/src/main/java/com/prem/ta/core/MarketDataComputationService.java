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
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.prem.ta.configs.Constants.getBinanceDateString;
import static com.prem.ta.configs.Constants.getBinanceZipFileName;

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
        log.info("Starting data processing...");

        while (true) {
            Page<File> page =
                    fileRepository.findByIsProcessedFalse(PageRequest.of(0, 10));

            if (page.isEmpty()) {
                break;
            }

            log.info("Processing {} pending files...", page.getNumberOfElements());
            page.getContent().forEach(this::processTradeDataFile);
        }

        log.info("Data processing completed.");
    }

    public void processTradeDataFile(File file) {

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

            ZipEntry entry = zipFile.stream().findFirst().orElse(null);
            if (entry == null) {
                log.warn("Empty ZIP for {} on {}", tickerSymbol, dateStr);
                return;
            }

            try (InputStream inputStream = zipFile.getInputStream(entry)) {

                Table table = Table.read().csv(
                        CsvReadOptions.builder(new InputStreamReader(inputStream))
                                .header(false)
                                .columnTypes(new ColumnType[]{
                                        ColumnType.LONG,
                                        ColumnType.DOUBLE,
                                        ColumnType.DOUBLE,
                                        ColumnType.DOUBLE,
                                        ColumnType.LONG,
                                        ColumnType.BOOLEAN,
                                        ColumnType.BOOLEAN,
                                })
                                .build()
                );

                MarketData computedData = computeMetrics(file, table);
                MarketData mergedData = marketDataRepository
                        .findByTickerAndMarketDataDate(
                                file.getTicker(),
                                file.getFileDate()
                        )
                        .map(existingData -> mergeWithComputed(existingData, computedData))
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

    private MarketData computeMetrics(File file, Table table) {

        OHLCVMetrics ohlcvMetrics = technicalAnalysisEngine.computeOHLCV(table);
        OrderFlowMetrics orderFlowMetrics = technicalAnalysisEngine.computeOrderFlow(table);
        VolumeProfileMetrics volumeProfileMetrics = technicalAnalysisEngine.computeVolumeProfile(table, ohlcvMetrics);

        MarketData marketData = new MarketData();
        marketData.setTicker(file.getTicker());
        marketData.setMarketDataDate(file.getFileDate());

        marketData.setPriceOpen(BigDecimal.valueOf(ohlcvMetrics.open()));
        marketData.setPriceHigh(BigDecimal.valueOf(ohlcvMetrics.high()));
        marketData.setPriceLow(BigDecimal.valueOf(ohlcvMetrics.low()));
        marketData.setPriceClose(BigDecimal.valueOf(ohlcvMetrics.close()));

        marketData.setVolume(BigDecimal.valueOf(ohlcvMetrics.volume()));
        marketData.setVwap(BigDecimal.valueOf(ohlcvMetrics.vwap()));
        marketData.setVolumeProfilePOC(volumeProfileMetrics.pointOfControl());
        marketData.setVolumeProfileVAH(volumeProfileMetrics.valueAreaHigh());
        marketData.setVolumeProfileVAL(volumeProfileMetrics.valueAreaLow());

        marketData.setBuyerVolumeShare(orderFlowMetrics.buyerVolumeShare());
        marketData.setBuyerCapitalShare(orderFlowMetrics.buyerCapitalShare());

        return marketData;
    }


    private MarketData mergeWithComputed(MarketData existing, MarketData computed) {
        existing.setPriceOpen(computed.getPriceOpen());
        existing.setPriceHigh(computed.getPriceHigh());
        existing.setPriceLow(computed.getPriceLow());
        existing.setPriceClose(computed.getPriceClose());
        existing.setVolume(computed.getVolume());
        existing.setVwap(computed.getVwap());
        existing.setVolumeProfilePOC(computed.getVolumeProfilePOC());
        existing.setVolumeProfileVAH(computed.getVolumeProfileVAH());
        existing.setVolumeProfileVAL(computed.getVolumeProfileVAL());
        existing.setBuyerVolumeShare(computed.getBuyerVolumeShare());
        existing.setBuyerCapitalShare(computed.getBuyerCapitalShare());
        return existing;
    }

}
