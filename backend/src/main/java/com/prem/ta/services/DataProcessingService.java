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
import org.springframework.transaction.annotation.Transactional;
import tech.tablesaw.api.*;
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

    private static final Logger log = LoggerFactory.getLogger(DataProcessingService.class);
    private final AppConfig appConfig;
    private final FileRepository fileRepository;
    private final TradeDataRepository tradeDataRepository;
    private final IntervalCache intervalCache;

    public DataProcessingService(
            AppConfig appConfig,
            FileRepository fileRepository,
            TradeDataRepository tradeDataRepository,
            IntervalCache intervalCache
    ) {
        this.appConfig = appConfig;
        this.fileRepository = fileRepository;
        this.tradeDataRepository = tradeDataRepository;
        this.intervalCache = intervalCache;
    }

    @Override
    public void doService() {
        log.info("Starting data processing...");
        Pageable pageable = PageRequest.of(0, Utils.PAGE_SIZE);
        Page<FileRecord> filePage;

        do {
            filePage = fileRepository.findByDownloadedTrueAndProcessedFalse(pageable);
            log.info("Found {} files to process in this batch.", filePage.getNumberOfElements());
            filePage.getContent().forEach(this::processFile);
            pageable = filePage.nextPageable();
        } while (filePage.hasNext());

        log.info("Data processing completed.");
    }

    @Transactional
    public void processFile(FileRecord fileRecord) {
        log.info("Processing fileRecord for ticker {} on date {}", fileRecord.getTicker().getSymbol(), fileRecord.getFileDate());
        String dateStr = fileRecord.getFileDate().format(Utils.getDateFormatter());
        String tickerSymbol = fileRecord.getTicker().getSymbol();
        String baseFileName = tickerSymbol + "-trades-" + dateStr + ".zip";
        Path filePath = Paths.get(appConfig.getDownloadDir(), tickerSymbol, baseFileName);

        try {
            byte[] fileContent = Files.readAllBytes(filePath);
            try (
                    ByteArrayInputStream bais = new ByteArrayInputStream(fileContent);
                    ZipInputStream zis = new ZipInputStream(bais)
            ) {
                zis.getNextEntry(); // Assuming one fileRecord per zip
                CsvReadOptions options = CsvReadOptions
                        .builder(new InputStreamReader(zis))
                        .header(false)
                        .columnTypes(
                                new ColumnType[]{
                                        ColumnType.LONG,    // trade_id
                                        ColumnType.DOUBLE,  // price
                                        ColumnType.DOUBLE,  // qty
                                        ColumnType.DOUBLE,  // quote_qty
                                        ColumnType.LONG,    // time
                                        ColumnType.BOOLEAN, // is_buyer_maker
                                        ColumnType.BOOLEAN, // is_best_match
                                }
                        )
                        .build();
                Table allRows = Table.read().csv(options);

                if (allRows.rowCount() == 0) {
                    log.warn("No trades for {} on {}", tickerSymbol, dateStr);
                    return;
                }

                // Extract columns
                DoubleColumn price = allRows.doubleColumn(1);
                DoubleColumn qty = allRows.doubleColumn(2);
                DoubleColumn quoteQty = allRows.doubleColumn(3);
                LongColumn time = allRows.longColumn(4);
                BooleanColumn isBuyerMaker = allRows.booleanColumn(5);

                // === OHLC + VWAP ===
                double open = price.get(0);
                double close = price.get(price.size() - 1);
                double high = price.max();
                double low = price.min();
                double volume = qty.sum();
                double totalQuoteQty = quoteQty.sum();
                double vwap = volume > 0 ? totalQuoteQty / volume : 0;

                // === Buyer/Seller pressure ===
                double buyerVolume = qty.where(isBuyerMaker.isFalse()).sum();
                double sellerVolume = qty.where(isBuyerMaker.isTrue()).sum();
                double buyerCapital = quoteQty.where(isBuyerMaker.isFalse()).sum();

                double buyerCapitalRatio = totalQuoteQty > 0 ? buyerCapital / totalQuoteQty : 0;
                double buyerVolumeRatio = volume > 0 ? (buyerVolume - sellerVolume) / volume : 0;

                // === Time & Trade Dynamics ===
                double minTime = time.min();
                double maxTime = time.max();
                double durationSec = (maxTime - minTime) / 1000.0;
                double tradesPerSec = durationSec > 0 ? allRows.rowCount() / durationSec : 0;

                DoubleColumn timeDiffs = time.difference();
                double avgInterTradeMs = timeDiffs.mean();

                // === Micro Volatility ===
                double microVolatility = 0.0;
                if (price.size() > 1) {
                    int n = price.size();
                    double[] returnsArray = new double[n - 1];
                    for (int i = 1; i < n; i++) {
                        double prev = price.getDouble(i - 1);
                        double curr = price.getDouble(i);
                        if (prev != 0) {
                            returnsArray[i - 1] = (curr - prev) / prev;
                        } else {
                            returnsArray[i - 1] = 0; // avoid division by zero
                        }
                    }
                    // compute standard deviation
                    double mean = 0;
                    for (double r : returnsArray) mean += r;
                    mean /= returnsArray.length;

                    double variance = 0;
                    for (double r : returnsArray) variance += Math.pow(r - mean, 2);
                    variance /= returnsArray.length;

                    microVolatility = Math.sqrt(variance);
                }


                // === VPIN proxy ===
                double vpin = volume > 0 ? Math.abs(buyerVolume - sellerVolume) / volume : 0;

                log.debug("Metrics for {} on {}:", tickerSymbol, dateStr);
                log.debug("OHLC: O={} H={} L={} C={}", open, high, low, close);
                log.debug("Volume={}, VWAP={}", volume, vwap);
                log.debug("BuyerCapRatio={}, BuyerVolRatio={}", buyerCapitalRatio, buyerVolumeRatio);
                log.debug("Trades/s={}, MicroVol={}, AvgInterTradeMs={}, VPIN={}",
                        tradesPerSec, microVolatility, avgInterTradeMs, vpin);

                // === Persist ===
                TradeData tradeData = new TradeData();
                tradeData.setTicker(fileRecord.getTicker());
                tradeData.setInterval(intervalCache.get(Utils.INTERVAL_DAILY));
                tradeData.setTradeTime(fileRecord.getFileDate());
                tradeData.setPriceOpen(open);
                tradeData.setPriceHigh(high);
                tradeData.setPriceLow(low);
                tradeData.setPriceClose(close);
                tradeData.setVolume(volume);
                tradeData.setVwap(vwap);
                tradeData.setBuyerCapitalRatio(buyerCapitalRatio);
                tradeData.setBuyerVolumeRatio(buyerVolumeRatio);
                tradeData.setTradesPerSec(tradesPerSec);
                tradeData.setMicroVolatility(microVolatility);
                tradeData.setAvgInterTradeMs(avgInterTradeMs);
                tradeData.setVpin(vpin);

                tradeDataRepository.save(tradeData);
            }

            fileRecord.setProcessed(true);
            fileRecord.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            fileRecord.setUpdatedBy(this.getClass().getSimpleName());
            fileRepository.save(fileRecord);

        } catch (Exception e) {
            log.error("Error processing fileRecord for ticker {} on date {}", tickerSymbol, dateStr, e);
        }
    }

}
