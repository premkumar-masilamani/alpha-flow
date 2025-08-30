package com.prem.ta.services;

import com.prem.ta.configs.ApplicationProperties;
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
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BinanceService {

    private static final Logger log = LoggerFactory.getLogger(
        BinanceService.class
    );
    private final ApplicationProperties properties;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd"
    );

    public BinanceService(ApplicationProperties properties) {
        this.properties = properties;
    }

    public void downloadTickData() {
        if (
            properties.getBinance().getTickers() == null ||
            properties.getBinance().getTickers().isEmpty()
        ) {
            throw new IllegalStateException(
                "No tickers configured. " +
                "Set app.binance.tickers.<TICKER>=<date> in application.properties"
            );
        }

        log.info("Download directory: {}", properties.getDownloadDir());
        properties
            .getBinance()
            .getTickers()
            .forEach((ticker, startDate) -> {
                log.info(
                    "Syncing ticker {} starting from {}",
                    ticker,
                    startDate
                );
                downloadTicker(ticker, startDate);
            });
        log.info("All Downloads completed!");
    }

    private void downloadTicker(String ticker, LocalDate startDate) {
        try {
            Path outDir = Path.of(properties.getDownloadDir(), ticker);
            Files.createDirectories(outDir);

            LocalDate today = LocalDate.now();
            for (
                LocalDate date = startDate;
                !date.isAfter(today);
                date = date.plusDays(1)
            ) {
                String dateStr = date.format(dateFormatter);
                String baseFileName = ticker + "-trades-" + dateStr + ".zip";
                String baseUrl =
                    "https://data.binance.vision/data/spot/daily/trades/" +
                    ticker +
                    "/" +
                    baseFileName;

                Path localFile = outDir.resolve(baseFileName);
                Path checksumFile = outDir.resolve(baseFileName + ".CHECKSUM");

                if (Files.exists(localFile)) {
                    log.info("Already downloaded: {}", localFile);
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
            }
        } catch (Exception e) {
            throw new RuntimeException("Error syncing " + ticker, e);
        }
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
