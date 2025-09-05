package com.prem.ta.services;

import com.prem.ta.configs.BinanceProperties;
import com.prem.ta.entities.File;
import com.prem.ta.entities.Ticker;
import com.prem.ta.repositories.FileRepository;
import com.prem.ta.repositories.TickerRepository;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataIngestionService {

    private static final Logger log = LoggerFactory.getLogger(
        DataIngestionService.class
    );
    private final BinanceProperties binanceProperties;
    private final TickerRepository tickerRepository;
    private final FileRepository FileRepository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd"
    );

    public DataIngestionService(
        BinanceProperties binanceProperties,
        TickerRepository tickerRepository,
        FileRepository FileRepository
    ) {
        this.binanceProperties = binanceProperties;
        this.tickerRepository = tickerRepository;
        this.FileRepository = FileRepository;
    }

    public void downloadData() {
        log.info("Download URL: {}", binanceProperties.getDownloadUrl());
        log.info("Download directory: {}", binanceProperties.getDownloadDir());
        tickerRepository
            .findAll()
            .forEach(ticker -> {
                log.info(
                    "Syncing ticker {} starting from {}",
                    ticker.getSymbol(),
                    ticker.getStartDate()
                );
                downloadTickerData(ticker);
            });
        log.info("All Downloads completed!");
    }

    @Transactional
    public void downloadTickerData(Ticker ticker) {
        Optional<File> latestFile =
            FileRepository.findTopByTickerOrderByFileDateDesc(ticker);
        LocalDate startDate = latestFile
            .map(file -> file.getFileDate().toLocalDate().plusDays(1))
            .orElse(ticker.getStartDate().toLocalDate());

        try {
            String tickerSymbol = ticker.getSymbol();
            Path outDir = Path.of(
                binanceProperties.getDownloadDir(),
                tickerSymbol
            );
            Files.createDirectories(outDir);

            LocalDate today = LocalDate.now();
            for (
                LocalDate date = startDate;
                !date.isAfter(today);
                date = date.plusDays(1)
            ) {
                String dateStr = date.format(dateFormatter);
                String baseFileName =
                    tickerSymbol + "-trades-" + dateStr + ".zip";
                String baseUrl = binanceProperties
                    .getDownloadUrl()
                    .replace("{ticker}", tickerSymbol)
                    .replace("{filename}", baseFileName);

                Path localFile = outDir.resolve(baseFileName);
                Path checksumFile = outDir.resolve(baseFileName + ".CHECKSUM");

                if (Files.exists(localFile)) {
                    log.info("Already downloaded: {}", localFile);
                    // Still record in DB if it's not there
                    saveFileRecord(ticker, date, true);
                    continue;
                }

                // Step 1: download checksum and save locally
                String checksumUrl = baseUrl + ".CHECKSUM";
                String expectedHash = downloadAndSaveChecksum(
                    checksumUrl,
                    checksumFile
                );
                if (expectedHash == null) {
                    log.warn(
                        "Checksum missing for {} — skipping",
                        baseFileName
                    );
                    continue;
                }

                // Step 2: download actual file
                log.info("Downloading {}", baseUrl);
                downloadFileWithChecksum(baseUrl, localFile, expectedHash);

                // Step 3: Save record to database
                saveFileRecord(ticker, date, true);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                "Error syncing " + ticker.getSymbol(),
                e
            );
        }
    }

    private void saveFileRecord(
        Ticker ticker,
        LocalDate date,
        boolean downloaded
    ) {
        OffsetDateTime fileDate = date
            .atStartOfDay(ZoneOffset.UTC)
            .toOffsetDateTime();
        Optional<File> existingFile = FileRepository.findByTickerAndFileDate(
            ticker,
            fileDate
        );

        if (existingFile.isPresent()) {
            log.debug(
                "File record for {} on {} already exists. Skipping.",
                ticker.getSymbol(),
                date
            );
            return;
        }

        File File = new File();
        File.setTicker(ticker);
        File.setFileDate(fileDate);
        File.setSource("Binance");
        File.setDownloaded(downloaded);
        File.setProcessed(false);
        File.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        File.setUpdatedBy("DataIngestionService");
        FileRepository.save(File);
        log.info("Saved file record for {} on {}", ticker.getSymbol(), date);
    }

    private String downloadAndSaveChecksum(String url, Path checksumFile) {
        try (InputStream in = URI.create(url).toURL().openStream()) {
            String content = new String(
                in.readAllBytes(),
                StandardCharsets.UTF_8
            ).trim();
            Files.writeString(checksumFile, content, StandardCharsets.UTF_8);

            // Binance CHECKSUM files are of form: "<hash> <filename>"
            String[] parts = content.split("\s+");
            return parts[0];
        } catch (IOException e) {
            log.error(
                "Failed to download checksum: {} ({})",
                url,
                e.getMessage()
            );
            return null;
        }
    }

    private void downloadFileWithChecksum(
        String fileUrl,
        Path outputPath,
        String expectedHash
    ) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        try (
            DigestInputStream in = new DigestInputStream(
                URI.create(fileUrl).toURL().openStream(),
                sha256
            );
            FileOutputStream out = new FileOutputStream(outputPath.toFile())
        ) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        // Verify checksum
        String actualHash = HexFormat.of().formatHex(sha256.digest());
        if (!actualHash.equalsIgnoreCase(expectedHash)) {
            Files.deleteIfExists(outputPath);
            throw new IOException(
                "Checksum mismatch for " +
                outputPath.getFileName() +
                ": expected " +
                expectedHash +
                " but got " +
                actualHash
            );
        }

        log.info("Verified checksum OK: {}", outputPath.getFileName());
    }
}
