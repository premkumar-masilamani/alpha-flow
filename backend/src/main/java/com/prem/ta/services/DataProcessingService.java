package com.prem.ta.services;

import com.prem.ta.configs.AppConfig;
import com.prem.ta.configs.Constants;
import com.prem.ta.entities.File;
import com.prem.ta.entities.TradeData;
import com.prem.ta.repositories.FileRepository;
import com.prem.ta.repositories.TradeDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.selection.Selection;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class DataProcessingService {

    private static final Logger log = LoggerFactory.getLogger(
            DataProcessingService.class
    );

    private final AppConfig appConfig;
    private final FileRepository fileRepository;
    private final TradeDataRepository tradeDataRepository;

    public DataProcessingService(
            AppConfig appConfig,
            FileRepository fileRepository,
            TradeDataRepository tradeDataRepository
    ) {
        this.appConfig = appConfig;
        this.fileRepository = fileRepository;
        this.tradeDataRepository = tradeDataRepository;
    }

    public void process() {
        log.info("Starting data processing...");

        while (true) {
            Page<File> page =
                    fileRepository.findByIsDownloadedTrueAndIsProcessedFalse(PageRequest.of(0, 10));

            if (page.isEmpty()) {
                break;
            }

            log.info("Processing {} pending files...", page.getNumberOfElements());
            page.getContent().forEach(this::processFile);
        }

        log.info("Data processing completed.");
    }

    public void processFile(File file) {

        final String tickerSymbol = file.getTicker().getSymbol();
        final String dateStr = Constants.getBinanceFormattedDateString(file.getFileDate());
        final String baseFileName = Constants.getBinanceZipFileName(tickerSymbol, dateStr);
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

                TradeData tradeData = computeMetrics(file, inputStream);
                tradeDataRepository.save(tradeData);

                file.setIsProcessed(true);
                fileRepository.save(file);

                log.info("Processed file {} for {}", baseFileName, tickerSymbol);
            }

        } catch (Exception e) {
            log.error("Failed to process {} on {}", tickerSymbol, dateStr, e);
        }
    }

    private TradeData computeMetrics(File file, InputStream inputStream) {

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

        int rowCount = table.rowCount();
        if (rowCount == 0) {
            throw new IllegalStateException("CSV contains no rows");
        }

        DoubleColumn price = table.doubleColumn(1);
        DoubleColumn qty = table.doubleColumn(2);
        DoubleColumn quoteQty = table.doubleColumn(3);
        BooleanColumn isBuyerMaker = table.booleanColumn(5);

        double open = price.get(0);
        double close = price.get(rowCount - 1);
        double high = price.max();
        double low = price.min();

        double volume = qty.sum();
        double totalQuoteQty = quoteQty.sum();

        double vwap = (volume > 0) ? totalQuoteQty / volume : 0.0;

        Selection buyerInitiatedTrades = isBuyerMaker.isFalse();

        double buyerVolume = qty.where(buyerInitiatedTrades).sum();
        double buyerCapital = quoteQty.where(buyerInitiatedTrades).sum();

        double buyerVolumeRatio = (volume > 0) ? buyerVolume / volume : 0.0;
        double buyerCapitalRatio = (totalQuoteQty > 0) ? buyerCapital / totalQuoteQty : 0.0;

        log.info("Processed {} rows for {}", rowCount, file.getTicker().getSymbol());

        log.debug(
                "Ticker {} metrics — O:{} H:{} L:{} C:{} V:{} VWAP:{} BuyerCapRatio:{} BuyerVolRatio:{}",
                file.getTicker().getSymbol(),
                open, high, low, close,
                volume, vwap,
                buyerCapitalRatio, buyerVolumeRatio
        );

        TradeData tradeData = new TradeData();
        tradeData.setTradeDate(file.getFileDate());
        tradeData.setTicker(file.getTicker());
        tradeData.setPriceOpen(BigDecimal.valueOf(open));
        tradeData.setPriceHigh(BigDecimal.valueOf(high));
        tradeData.setPriceLow(BigDecimal.valueOf(low));
        tradeData.setPriceClose(BigDecimal.valueOf(close));
        tradeData.setVolume(BigDecimal.valueOf(volume));
        tradeData.setVwap(BigDecimal.valueOf(vwap));
        tradeData.setBuyerCapitalRatio(buyerCapitalRatio);
        tradeData.setBuyerVolumeRatio(buyerVolumeRatio);

        return tradeData;
    }

}
