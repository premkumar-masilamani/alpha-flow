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
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

                TradeData computedData = computeMetrics(file, inputStream);
                TradeData mergedData = tradeDataRepository
                        .findByTickerAndTradeDate(
                                file.getTicker(),
                                file.getFileDate()
                        )
                        .map(existingData -> mergeMissingFields(existingData, computedData))
                        .orElse(computedData);

                tradeDataRepository.save(mergedData);

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
        double vwap = volume > 0 ? totalQuoteQty / volume : 0.0;

        Selection buyerInitiated = isBuyerMaker.isFalse();
        double buyerVolume = qty.where(buyerInitiated).sum();
        double buyerCapital = quoteQty.where(buyerInitiated).sum();

        double buyerVolumeRatio = volume > 0 ? buyerVolume / volume : 0.0;
        double buyerCapitalRatio = totalQuoteQty > 0 ? buyerCapital / totalQuoteQty : 0.0;


        BigDecimal bin = deriveVolumeProfileBinSize(
                BigDecimal.valueOf(open),
                BigDecimal.valueOf(high),
                BigDecimal.valueOf(low),
                BigDecimal.valueOf(close)
        );

        Map<BigDecimal, BigDecimal> volumeAtPrice = new HashMap<>();

        for (int i = 0; i < rowCount; i++) {

            BigDecimal p = BigDecimal.valueOf(price.get(i));
            BigDecimal q = BigDecimal.valueOf(qty.get(i));

            BigDecimal bucketIndex = p.divide(bin, 0, RoundingMode.FLOOR);
            BigDecimal bucketPrice = bucketIndex.multiply(bin);

            volumeAtPrice.merge(bucketPrice, q, BigDecimal::add);
        }

        BigDecimal volumeProfilePointOfControl = volumeAtPrice.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        BigDecimal totalVolume = volumeAtPrice.values()
                .stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal targetVolume =
                totalVolume.multiply(
                        BigDecimal.valueOf(Constants.VOLUME_PROFILE_VALUE_AREA_PERCENT)
                );

        List<Map.Entry<BigDecimal, BigDecimal>> sorted =
                volumeAtPrice.entrySet()
                        .stream()
                        .sorted(Comparator.comparing(
                                e -> e.getKey().subtract(volumeProfilePointOfControl).abs()
                        ))
                        .toList();

        BigDecimal cumulative = BigDecimal.ZERO;
        BigDecimal volumeProfileValueAreaHigh = volumeProfilePointOfControl;
        BigDecimal volumeProfileValueAreaLow = volumeProfilePointOfControl;

        for (Map.Entry<BigDecimal, BigDecimal> e : sorted) {
            cumulative = cumulative.add(e.getValue());

            volumeProfileValueAreaHigh = volumeProfileValueAreaHigh.max(e.getKey());
            volumeProfileValueAreaLow = volumeProfileValueAreaLow.min(e.getKey());

            if (cumulative.compareTo(targetVolume) >= 0) {
                break;
            }
        }


        log.info("Processed {} rows for {}", rowCount, file.getTicker().getSymbol());
        log.debug(
                "Ticker {} — O:{} H:{} L:{} C:{} V:{} VWAP:{} BCS:{} BVS:{} POC:{} VAH:{} VAL:{}",
                file.getTicker().getSymbol(),
                open, high, low, close, volume, vwap, buyerCapitalRatio, buyerVolumeRatio,
                volumeProfilePointOfControl, volumeProfileValueAreaHigh, volumeProfileValueAreaLow
        );

        TradeData tradeData = new TradeData();
        tradeData.setTicker(file.getTicker());
        tradeData.setTradeDate(file.getFileDate());

        tradeData.setPriceOpen(BigDecimal.valueOf(open));
        tradeData.setPriceHigh(BigDecimal.valueOf(high));
        tradeData.setPriceLow(BigDecimal.valueOf(low));
        tradeData.setPriceClose(BigDecimal.valueOf(close));

        tradeData.setVolume(BigDecimal.valueOf(volume));
        tradeData.setVolumeWeightedAveragePrice(BigDecimal.valueOf(vwap));

        tradeData.setVolumeProfilePointOfControl(volumeProfilePointOfControl);
        tradeData.setVolumeProfileValueAreaHigh(volumeProfileValueAreaHigh);
        tradeData.setVolumeProfileValueAreaLow(volumeProfileValueAreaLow);

        tradeData.setBuyerVolumeShare(buyerVolumeRatio);
        tradeData.setBuyerCapitalShare(buyerCapitalRatio);

        return tradeData;
    }

    private TradeData mergeMissingFields(TradeData existing, TradeData computed) {

        if (existing.getPriceOpen() == null) {
            existing.setPriceOpen(computed.getPriceOpen());
        }
        if (existing.getPriceHigh() == null) {
            existing.setPriceHigh(computed.getPriceHigh());
        }
        if (existing.getPriceLow() == null) {
            existing.setPriceLow(computed.getPriceLow());
        }
        if (existing.getPriceClose() == null) {
            existing.setPriceClose(computed.getPriceClose());
        }
        if (existing.getVolume() == null) {
            existing.setVolume(computed.getVolume());
        }
        if (existing.getVolumeWeightedAveragePrice() == null) {
            existing.setVolumeWeightedAveragePrice(computed.getVolumeWeightedAveragePrice());
        }
        if (existing.getVolumeProfilePointOfControl() == null) {
            existing.setVolumeProfilePointOfControl(
                    computed.getVolumeProfilePointOfControl()
            );
        }
        if (existing.getVolumeProfileValueAreaHigh() == null) {
            existing.setVolumeProfileValueAreaHigh(
                    computed.getVolumeProfileValueAreaHigh()
            );
        }
        if (existing.getVolumeProfileValueAreaLow() == null) {
            existing.setVolumeProfileValueAreaLow(
                    computed.getVolumeProfileValueAreaLow()
            );
        }
        if (existing.getBuyerVolumeShare() == null) {
            existing.setBuyerVolumeShare(computed.getBuyerVolumeShare());
        }
        if (existing.getBuyerCapitalShare() == null) {
            existing.setBuyerCapitalShare(computed.getBuyerCapitalShare());
        }

        return existing;
    }

    private BigDecimal deriveVolumeProfileBinSize(
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close
    ) {
        BigDecimal range = high.subtract(low);

        // 5% of daily range
        BigDecimal bin = range.multiply(BigDecimal.valueOf(0.05));

        // If range == 0, fallback to price scale
        if (bin.signum() == 0) {
            BigDecimal priceScale = open
                    .add(high)
                    .add(low)
                    .add(close)
                    .divide(BigDecimal.valueOf(4), 18, RoundingMode.HALF_UP);

            bin = priceScale.multiply(BigDecimal.valueOf(0.001));
        }

        return bin.stripTrailingZeros();
    }

}
