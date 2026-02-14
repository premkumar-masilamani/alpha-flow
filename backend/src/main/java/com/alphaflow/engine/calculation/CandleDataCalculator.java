package com.alphaflow.engine.calculation;

import com.alphaflow.engine.configs.BinanceConfig;
import com.alphaflow.engine.entities.TickerDataFile;
import com.alphaflow.engine.metrics.CapitalProfileMetrics;
import com.alphaflow.engine.metrics.MetricsCalculator;
import com.alphaflow.engine.metrics.OHLCVMetrics;
import com.alphaflow.engine.metrics.OrderFlowMetrics;
import com.alphaflow.engine.repositories.TickerDataFileRepository;
import com.alphaflow.infrastructure.entities.CandleData;
import com.alphaflow.infrastructure.repositories.CandleDataRepository;
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
public class CandleDataCalculator {

    private static final Logger log = LoggerFactory.getLogger(CandleDataCalculator.class);

    private final BinanceConfig binanceConfig;
    private final TickerDataFileRepository tickerDataFileRepository;
    private final CandleDataRepository candleDataRepository;

    public CandleDataCalculator(
            BinanceConfig binanceConfig,
            TickerDataFileRepository tickerDataFileRepository,
            CandleDataRepository candleDataRepository
    ) {
        this.binanceConfig = binanceConfig;
        this.tickerDataFileRepository = tickerDataFileRepository;
        this.candleDataRepository = candleDataRepository;
    }

    /**
     * Entry point for computing candle data.
     * Iterates through all unprocessed files in the database, extracts tick data from ZIP archives,
     * computes OHLCV, Order Flow, and Volume Profile metrics, and persists the results.
     */
    public void calculate() {
        log.info("Starting CandleData Bar Computation");

        int totalProcessed = 0;
        while (true) {
            // Fetch a page of unprocessed files to avoid loading too many records into memory
            Page<TickerDataFile> page = tickerDataFileRepository.findByIsProcessedFalse(PageRequest.of(0, DB_QUERY_PAGE_SIZE));

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

        log.info("Completed CandleData Bar Computation. Total files processed: {}", totalProcessed);
    }

    /**
     * Processes a single tick data file:
     * 1. Locates the ZIP file on disk.
     * 2. Extracts the CSV content.
     * 3. Computes metrics.
     * 4. Merges with existing candle data if applicable (to handle multiple files for the same date/ticker).
     * 5. Updates the file status to processed.
     *
     * @param file The file record from the database.
     */
    public void processTickDataFile(TickerDataFile file) {
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
                CandleData computedData = computeMetrics(file, tickTable);

                // If we already have candle data for this ticker/date, merge it.
                CandleData mergedData = candleDataRepository.findByTickerAndCandleDataDate(file.getTicker(), file.getDataFileDate())
                        .map(existingData -> {
                            log.debug("Existing candle data found for {} on {}. Merging metrics.", tickerSymbol, dateStr);
                            return existingData.merge(computedData);
                        })
                        .orElse(computedData);

                log.debug("Saving candle data: {}", mergedData);
                candleDataRepository.save(mergedData);

                // Mark the file as processed to avoid re-computation
                file.setIsProcessed(true);
                tickerDataFileRepository.save(file);

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

    private CandleData computeMetrics(TickerDataFile file, Table table) {

        OHLCVMetrics ohlcvMetrics = MetricsCalculator.ohlcv(table);
        OrderFlowMetrics orderFlowMetrics = MetricsCalculator.orderFlow(table);
        CapitalProfileMetrics capitalProfileMetrics = MetricsCalculator.capitalProfile(table, ohlcvMetrics);

        return CandleData.builder()
                .ticker(file.getTicker())
                .candleDataDate(file.getDataFileDate())
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
