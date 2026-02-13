package com.alphaflow.engine.calculation;

import com.alphaflow.engine.configs.BinanceConfig;
import com.alphaflow.engine.entities.DataFile;
import com.alphaflow.engine.metrics.CapitalProfileMetrics;
import com.alphaflow.engine.metrics.MetricsCalculator;
import com.alphaflow.engine.metrics.OHLCVMetrics;
import com.alphaflow.engine.metrics.OrderFlowMetrics;
import com.alphaflow.engine.repositories.DataFileRepository;
import com.alphaflow.infrastructure.entities.CandleBar;
import com.alphaflow.infrastructure.repositories.CandleBarRepository;
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

import static com.alphaflow.infrastructure.constants.AppConstants.*;
import static tech.tablesaw.io.csv.CsvReadOptions.builder;

@Service
public class CandleBarCalculator {

    private static final Logger log = LoggerFactory.getLogger(CandleBarCalculator.class);

    private final BinanceConfig binanceConfig;
    private final DataFileRepository dataFileRepository;
    private final CandleBarRepository candleBarRepository;

    public CandleBarCalculator(
            BinanceConfig binanceConfig,
            DataFileRepository dataFileRepository,
            CandleBarRepository candleBarRepository
    ) {
        this.binanceConfig = binanceConfig;
        this.dataFileRepository = dataFileRepository;
        this.candleBarRepository = candleBarRepository;
    }

    /**
     * Entry point for computing candle bar data.
     * Iterates through all unprocessed files in the database, extracts tick data from ZIP archives,
     * computes OHLCV, Order Flow, and Volume Profile metrics, and persists the results.
     */
    public void calculate() {
        log.info("Starting CandleBar Bar Computation");

        int totalProcessed = 0;
        while (true) {
            // Fetch a page of unprocessed files to avoid loading too many records into memory
            Page<DataFile> page = dataFileRepository.findByIsProcessedFalse(PageRequest.of(0, DB_QUERY_PAGE_SIZE));

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

        log.info("Completed CandleBar Bar Computation. Total files processed: {}", totalProcessed);
    }

    /**
     * Processes a single tick data file:
     * 1. Locates the ZIP file on disk.
     * 2. Extracts the CSV content.
     * 3. Computes metrics.
     * 4. Merges with existing candle bar data if applicable (to handle multiple files for the same date/ticker).
     * 5. Updates the file status to processed.
     *
     * @param file The file record from the database.
     */
    public void processTickDataFile(DataFile file) {
        final String tickerSymbol = file.getTicker().getTickerSymbol();
        final String dateStr = getBinanceDateString(file.getDataFileDate());
        final String baseFileName = getBinanceZipFileName(tickerSymbol, dateStr);
        final Path filePath = Paths.get(binanceConfig.getDownloadDir(), tickerSymbol, baseFileName);

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
                CandleBar computedData = computeMetrics(file, tickTable);

                // If we already have candle bar data for this ticker/date, merge it.
                CandleBar mergedData = candleBarRepository.findByTickerAndCandleBarDate(file.getTicker(), file.getDataFileDate())
                        .map(existingData -> {
                            log.debug("Existing candle bar data found for {} on {}. Merging metrics.", tickerSymbol, dateStr);
                            return existingData.merge(computedData);
                        })
                        .orElse(computedData);

                log.debug("Saving candle bar data: {}", mergedData);
                candleBarRepository.save(mergedData);

                // Mark the file as processed to avoid re-computation
                file.setIsProcessed(true);
                dataFileRepository.save(file);

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

    private CandleBar computeMetrics(DataFile file, Table table) {

        OHLCVMetrics ohlcvMetrics = MetricsCalculator.ohlcv(table);
        OrderFlowMetrics orderFlowMetrics = MetricsCalculator.orderFlow(table);
        CapitalProfileMetrics capitalProfileMetrics = MetricsCalculator.capitalProfile(table, ohlcvMetrics);

        return CandleBar.builder()
                .ticker(file.getTicker())
                .candleBarDate(file.getDataFileDate())
                .priceOpen(ohlcvMetrics.open())
                .priceHigh(ohlcvMetrics.high())
                .priceLow(ohlcvMetrics.low())
                .priceClose(ohlcvMetrics.close())
                .volume(ohlcvMetrics.volume())
                .vwap(ohlcvMetrics.vwap())
                .buyerCapital(orderFlowMetrics.buyerCapital())
                .totalCapital(orderFlowMetrics.totalCapital())
                .capitalPOC(capitalProfileMetrics.pointOfControl())
                .capitalVAH(capitalProfileMetrics.valueAreaHigh())
                .capitalVAL(capitalProfileMetrics.valueAreaLow())
                .build();
    }

}
