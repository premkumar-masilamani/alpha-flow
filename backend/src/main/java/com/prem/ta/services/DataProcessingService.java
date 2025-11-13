package com.prem.ta.services;

import com.prem.ta.configs.AppConfig;
import com.prem.ta.configs.Utils;
import com.prem.ta.entities.FileRecord;
import com.prem.ta.entities.TradeData;
import com.prem.ta.repositories.FileRepository;
import com.prem.ta.repositories.TradeDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.zip.ZipInputStream;

@Service
public class DataProcessingService implements com.prem.ta.services.Service {

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

    @Override
    public void doService() {
        log.info("Starting data processing...");

        Pageable pageable = PageRequest.of(0, Utils.PAGE_SIZE);
        Page<FileRecord> filePage;

        do {
            filePage = fileRepository.findByIsDownloadedTrueAndIsProcessedFalse(
                    pageable
            );
            int count = filePage.getNumberOfElements();
            if (count == 0) break;

            log.info("Processing {} pending files...", count);
            filePage.getContent().forEach(this::safeProcessFile);
            pageable = filePage.nextPageable();
        } while (filePage.hasNext());

        log.info("Data processing completed.");
    }

    private void safeProcessFile(FileRecord fileRecord) {
        try {
            processFile(fileRecord);
        } catch (Exception e) {
            log.error(
                    "Unexpected error while processing {} on {}",
                    fileRecord.getTicker().getSymbol(),
                    fileRecord.getFileDate(),
                    e
            );
        }
    }

    public void processFile(FileRecord fileRecord) {
        String tickerSymbol = fileRecord.getTicker().getSymbol();
        String dateStr = fileRecord
                .getFileDate()
                .format(Utils.getDateFormatter());
        String baseFileName = tickerSymbol + "-trades-" + dateStr + ".zip";
        Path filePath = Paths.get(
                appConfig.getDownloadDir(),
                tickerSymbol,
                baseFileName
        );

        log.info("Processing trades for {} on {}", tickerSymbol, dateStr);

        if (!Files.exists(filePath)) {
            log.warn("File not found: {}", filePath);
            return;
        }

        try {
            byte[] fileContent = Files.readAllBytes(filePath);

            try (
                    ByteArrayInputStream bais = new ByteArrayInputStream(
                            fileContent
                    );
                    ZipInputStream zis = new ZipInputStream(bais)
            ) {
                if (zis.getNextEntry() == null) {
                    log.warn("Empty ZIP for {} on {}", tickerSymbol, dateStr);
                    return;
                }

                CsvReadOptions options = CsvReadOptions.builder(
                                new InputStreamReader(zis)
                        )
                        .header(false)
                        .columnTypes(
                                new ColumnType[]{
                                        ColumnType.LONG, // trade_id
                                        ColumnType.DOUBLE, // price
                                        ColumnType.DOUBLE, // qty
                                        ColumnType.DOUBLE, // quote_qty
                                        ColumnType.LONG, // time
                                        ColumnType.BOOLEAN, // is_buyer_maker
                                        ColumnType.BOOLEAN, // is_best_match
                                }
                        )
                        .build();

                Table table = Table.read().csv(options);

                if (table.isEmpty()) {
                    log.warn(
                            "No trade rows found for {} on {}",
                            tickerSymbol,
                            dateStr
                    );
                    return;
                }

                TradeData tradeData = computeMetrics(fileRecord, table);
                tradeDataRepository.save(tradeData);

                fileRecord.setIsProcessed(true);
                fileRecord.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                fileRecord.setUpdatedBy(getClass().getSimpleName());
                fileRepository.save(fileRecord);

                log.info(
                        "Processed {} rows for {}",
                        table.rowCount(),
                        tickerSymbol
                );
            }
        } catch (Exception e) {
            log.error("Failed to process {} on {}", tickerSymbol, dateStr, e);
        }
    }

    private TradeData computeMetrics(FileRecord fileRecord, Table table) {
        DoubleColumn price = table.doubleColumn(1);
        DoubleColumn qty = table.doubleColumn(2);
        DoubleColumn quoteQty = table.doubleColumn(3);
        BooleanColumn isBuyerMaker = table.booleanColumn(5);

        double open = price.get(0);
        double close = price.get(price.size() - 1);
        double high = price.max();
        double low = price.min();
        double volume = qty.sum();
        double totalQuoteQty = quoteQty.sum();
        double vwap = volume > 0 ? totalQuoteQty / volume : 0.0;

        double buyerVolume = qty.where(isBuyerMaker.isFalse()).sum();
        double buyerCapital = quoteQty.where(isBuyerMaker.isFalse()).sum();

        double buyerCapitalRatio = totalQuoteQty > 0
                ? buyerCapital / totalQuoteQty
                : 0.0;
        double buyerVolumeRatio = volume > 0 ? buyerVolume / volume : 0.0;

        log.debug(
                "Ticker {} metrics — O:{} H:{} L:{} C:{} V:{} VWAP:{} BuyerCapRatio:{} BuyerVolRatio:{}",
                fileRecord.getTicker().getSymbol(),
                open,
                high,
                low,
                close,
                volume,
                vwap,
                buyerCapitalRatio,
                buyerVolumeRatio
        );

        TradeData td = new TradeData();
        td.setTradeTime(fileRecord.getFileDate());
        td.setTicker(fileRecord.getTicker());
        td.setPriceOpen(open);
        td.setPriceHigh(high);
        td.setPriceLow(low);
        td.setPriceClose(close);
        td.setVolume(volume);
        td.setVwap(vwap);
        td.setBuyerCapitalRatio((float) buyerCapitalRatio);
        td.setBuyerVolumeRatio((float) buyerVolumeRatio);
        return td;
    }
}
