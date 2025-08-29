package com.prem.ta.services;

import com.prem.ta.config.ApplicationProperties;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class BinanceService {

    private final ApplicationProperties properties;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd"
    );

    public BinanceService(ApplicationProperties properties) {
        this.properties = properties;
    }

    public void run() {
        if (
            properties.getBinance().getTickers() == null ||
            properties.getBinance().getTickers().isEmpty()
        ) {
            throw new IllegalStateException(
                "No tickers configured. " +
                "Set app.binance.tickers.<TICKER>=<date> in application.properties"
            );
        }

        System.out.println(
            "Download directory: " + properties.getDownloadDir()
        );
        properties
            .getBinance()
            .getTickers()
            .forEach((ticker, startDate) -> {
                System.out.println(
                    "Syncing ticker " + ticker + " starting from " + startDate
                );
                syncTicker(ticker, startDate);
            });
    }

    private void syncTicker(String ticker, LocalDate startDate) {
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
                    System.out.println("Already downloaded: " + localFile);
                    continue;
                }

                // Step 1: download checksum and save locally
                String checksumUrl = baseUrl + ".CHECKSUM";
                String expectedHash = downloadAndSaveChecksum(
                    checksumUrl,
                    checksumFile
                );
                if (expectedHash == null) {
                    System.err.println(
                        "Checksum missing for " + baseFileName + " — skipping"
                    );
                    continue;
                }

                // Step 2: download actual file
                System.out.println("Downloading " + baseUrl);
                downloadFileWithChecksum(baseUrl, localFile, expectedHash);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error syncing " + ticker, e);
        }
    }

    private String downloadAndSaveChecksum(String url, Path checksumFile) {
        try (InputStream in = new URL(url).openStream()) {
            String content = new String(
                in.readAllBytes(),
                StandardCharsets.UTF_8
            ).trim();
            Files.writeString(checksumFile, content, StandardCharsets.UTF_8);

            // Binance CHECKSUM files are of form: "<hash> <filename>"
            String[] parts = content.split("\s+");
            return parts[0];
        } catch (IOException e) {
            System.err.println(
                "Failed to download checksum: " +
                url +
                " (" +
                e.getMessage() +
                ")"
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
                new URL(fileUrl).openStream(),
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

        System.out.println("Verified checksum OK: " + outputPath.getFileName());
    }
}
