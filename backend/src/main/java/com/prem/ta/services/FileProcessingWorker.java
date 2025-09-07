package com.prem.ta.services;

import com.prem.ta.configs.AppConfig;
import com.prem.ta.entities.File;
import com.prem.ta.entities.TradeData;
import com.prem.ta.repositories.FileRepository;
import com.prem.ta.repositories.TradeDataRepository;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

@Service
public class FileProcessingWorker {

    private static final Logger log = LoggerFactory.getLogger(FileProcessingWorker.class);
    private final FileRepository fileRepository;
    private final TradeDataRepository tradeDataRepository;
    private final AppConfig appConfig;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public FileProcessingWorker(
        FileRepository fileRepository,
        TradeDataRepository tradeDataRepository,
        AppConfig appConfig
    ) {
        this.fileRepository = fileRepository;
        this.tradeDataRepository = tradeDataRepository;
        this.appConfig = appConfig;
    }

    @Transactional
    public void processFile(File file) {
        log.info("Processing file for ticker {} on date {}", file.getTicker().getSymbol(), file.getFileDate());
        String dateStr = file.getFileDate().format(dateFormatter);
        String tickerSymbol = file.getTicker().getSymbol();
        String baseFileName = tickerSymbol + "-trades-" + dateStr + ".zip";
        Path filePath = Paths.get(appConfig.getDownloadDir(), tickerSymbol, baseFileName);

        try {
            byte[] fileContent = Files.readAllBytes(filePath);
            try (
                ByteArrayInputStream bais = new ByteArrayInputStream(fileContent);
                ZipInputStream zis = new ZipInputStream(bais)
            ) {
                zis.getNextEntry(); // Assuming one file per zip
                CsvReadOptions options = CsvReadOptions
                    .builder(new InputStreamReader(zis))
                    .header(false)
                    .columnTypes(
                        new ColumnType[] {
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
                Table allRows = Table.read().csv(options);

                // Calculations
                DoubleColumn price = allRows.doubleColumn(1);
                DoubleColumn qty = allRows.doubleColumn(2);
                DoubleColumn quoteQty = allRows.doubleColumn(3);
                BooleanColumn isBuyerMaker = allRows.booleanColumn(5);

                double open = price.get(0);
                double close = price.get(price.size() - 1);
                double high = price.max();
                double low = price.min();
                double volume = qty.sum();
                double totalQuoteQty = quoteQty.sum();
                double vwap = volume > 0 ? totalQuoteQty / volume : 0;

                long buyerMakerCount = isBuyerMaker.countFalse();
                double buyerParticipationRatio = allRows.rowCount() == 0 ? 0 : (double) buyerMakerCount / allRows.rowCount();

                double buyerCapital = quoteQty.where(isBuyerMaker.isFalse()).sum();
                double buyerCapitalRatio = totalQuoteQty > 0 ? buyerCapital / totalQuoteQty : 0;

                double buyerVolume = qty.where(isBuyerMaker.isFalse()).sum();
                double sellerVolume = qty.where(isBuyerMaker.isTrue()).sum();
                double buyerVolumeRatio = volume > 0 ? (buyerVolume - sellerVolume) / volume : 0;

                double whaleImpact = Math.abs(buyerCapitalRatio - buyerParticipationRatio);

                log.debug("Calculated metrics for {} on {}:", tickerSymbol, dateStr);
                log.debug("Open: {}, High: {}, Low: {}, Close: {}", open, high, low, close);
                log.debug("Volume: {}", volume);
                log.debug("VWAP: {}", vwap);
                log.debug("Buyer Capital Ratio: {}", buyerCapitalRatio);
                log.debug("Buyer Participation Ratio: {}", buyerParticipationRatio);
                log.debug("Buyer Volume Ratio: {}", buyerVolumeRatio);
                log.debug("Whale Impact: {}", whaleImpact);

                TradeData tradeData = new TradeData();
                tradeData.setTickerId(file.getTicker().getTickerId());
                tradeData.setIntervalId((short) 4); // 4 for daily
                tradeData.setTradeTime(file.getFileDate());
                tradeData.setPriceOpen(open);
                tradeData.setPriceHigh(high);
                tradeData.setPriceLow(low);
                tradeData.setPriceClose(close);
                tradeData.setVolume(volume);
                tradeData.setVwap(vwap);
                tradeData.setBuyerCapitalRatio(buyerCapitalRatio);
                tradeData.setBuyerParticipationRatio(buyerParticipationRatio);
                tradeData.setBuyerVolumeRatio(buyerVolumeRatio);
                tradeData.setWhaleImpact(whaleImpact);

                tradeDataRepository.save(tradeData);
            }

            file.setProcessed(true);
            file.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            file.setUpdatedBy(this.getClass().getSimpleName());
            fileRepository.save(file);

        } catch (Exception e) {
            log.error("Error processing file for ticker {} on date {}", tickerSymbol, dateStr, e);
        }
    }
}
