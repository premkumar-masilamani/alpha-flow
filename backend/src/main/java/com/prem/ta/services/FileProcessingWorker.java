package com.prem.ta.services;

import com.opencsv.CSVReader;
import com.prem.ta.configs.BinanceProperties;
import com.prem.ta.entities.File;
import com.prem.ta.entities.TradeData;
import com.prem.ta.repositories.FileRepository;
import com.prem.ta.repositories.TradeDataRepository;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.lingala.zip4j.ZipFile;
import org.slf4j.Logger;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileProcessingWorker {

    private static final Logger log = LoggerFactory.getLogger(FileProcessingWorker.class);
    private final FileRepository fileRepository;
    private final TradeDataRepository tradeDataRepository;
    private final BinanceProperties binanceProperties;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @PersistenceContext
    private EntityManager entityManager;

    public FileProcessingWorker(
        FileRepository fileRepository,
        TradeDataRepository tradeDataRepository,
        BinanceProperties binanceProperties
    ) {
        this.fileRepository = fileRepository;
        this.tradeDataRepository = tradeDataRepository;
        this.binanceProperties = binanceProperties;
    }

    @Transactional
    public void processFile(File file) {
        log.info("Processing file for ticker {} on date {}", file.getTicker().getSymbol(), file.getFileDate());
        String dateStr = file.getFileDate().format(dateFormatter);
        String tickerSymbol = file.getTicker().getSymbol();
        String baseFileName = tickerSymbol + "-trades-" + dateStr + ".zip";
        Path filePath = Paths.get(binanceProperties.getDownloadDir(), tickerSymbol, baseFileName);

        try {
            ZipFile zipFile = new ZipFile(filePath.toFile());
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            zipFile.extractAll(tempDir.toString());
            String csvFileName = zipFile.getFileHeaders().get(0).getFileName();
            Path csvFilePath = tempDir.resolve(csvFileName);

            try (CSVReader reader = new CSVReader(new FileReader(csvFilePath.toFile()))) {
                List<String[]> allRows = reader.readAll();

                // Calculations
                double open = Double.parseDouble(allRows.get(0)[1]);
                double close = Double.parseDouble(allRows.get(allRows.size() - 1)[1]);
                double high = allRows.stream().mapToDouble(row -> Double.parseDouble(row[1])).max().getAsDouble();
                double low = allRows.stream().mapToDouble(row -> Double.parseDouble(row[1])).min().getAsDouble();
                double volume = allRows.stream().mapToDouble(row -> Double.parseDouble(row[2])).sum();
                double totalQuoteQty = allRows.stream().mapToDouble(row -> Double.parseDouble(row[3])).sum();
                double vwap = totalQuoteQty / volume;

                long buyerMakerCount = allRows.stream().filter(row -> !Boolean.parseBoolean(row[5])).count();
                double buyerParticipationRatio = (double) buyerMakerCount / allRows.size();

                double buyerCapital = allRows.stream()
                    .filter(row -> !Boolean.parseBoolean(row[5]))
                    .mapToDouble(row -> Double.parseDouble(row[3]))
                    .sum();
                double buyerCapitalRatio = buyerCapital / totalQuoteQty;

                double buyerVolume = allRows.stream()
                    .filter(row -> !Boolean.parseBoolean(row[5]))
                    .mapToDouble(row -> Double.parseDouble(row[2]))
                    .sum();
                double sellerVolume = allRows.stream()
                    .filter(row -> Boolean.parseBoolean(row[5]))
                    .mapToDouble(row -> Double.parseDouble(row[2]))
                    .sum();
                double buyerVolumeRatio = (buyerVolume - sellerVolume) / volume;

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

                ensurePartitionExists(file.getFileDate());
                tradeDataRepository.save(tradeData);
            }

            file.setProcessed(true);
            file.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            file.setUpdatedBy("DataProcessingService");
            fileRepository.save(file);

        } catch (Exception e) {
            log.error("Error processing file: " + filePath, e);
        }
    }

    private void ensurePartitionExists(OffsetDateTime tradeTime) {
        int year = tradeTime.getYear();
        String partitionTableName = "trade_data_y" + year;

        String checkPartitionSql = "SELECT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = ?)";
        jakarta.persistence.Query query = entityManager.createNativeQuery(checkPartitionSql, Boolean.class);
        query.setParameter(1, partitionTableName);
        boolean exists = (Boolean) query.getSingleResult();

        if (!exists) {
            log.info("Partition {} does not exist. Creating it.", partitionTableName);
            String createPartitionSql = String.format(
                "CREATE TABLE %s PARTITION OF trade_data FOR VALUES FROM ('%s-01-01 00:00:00 UTC') TO ('%s-01-01 00:00:00 UTC')",
                partitionTableName,
                year,
                year + 1
            );
            entityManager.createNativeQuery(createPartitionSql).executeUpdate();
            log.info("Partition {} created.", partitionTableName);
        } else {
            log.debug("Partition {} already exists.", partitionTableName);
        }
    }
}
