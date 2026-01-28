package com.alphaflow.core;

import com.alphaflow.infrastructure.config.AppConfig;
import com.alphaflow.infrastructure.persistence.entities.File;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
import com.alphaflow.infrastructure.persistence.repositories.FileRepository;
import com.alphaflow.infrastructure.persistence.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

import static com.alphaflow.infrastructure.config.Constants.getBinanceDateString;
import static com.alphaflow.infrastructure.config.Constants.getBinanceZipFileName;

@Service
public class TickDataDownloader {

    private static final Logger log = LoggerFactory.getLogger(TickDataDownloader.class);

    private final AppConfig appConfig;
    private final TickerRepository tickerRepository;
    private final FileRepository fileRepository;

    public TickDataDownloader(AppConfig appConfig, TickerRepository tickerRepository, FileRepository fileRepository) {
        this.appConfig = appConfig;
        this.tickerRepository = tickerRepository;
        this.fileRepository = fileRepository;
    }

    /**
     * Entry point for downloading historical tick data.
     * Iterates through active tickers and synchronizes missing data from the Binance Public Data repository.
     * Downloads are processed sequentially to respect rate limits and system resources.
     */
    public void download() {
        log.info("Starting tick data download process...");
        log.info("Remote Repository: {}", appConfig.getDownloadUrl());
        log.info("Local Storage: {}", appConfig.getDownloadDir());

        var activeTickers = tickerRepository.findByIsActiveTrue();
        log.info("Found {} active tickers to sync.", activeTickers.size());

        activeTickers.forEach(this::downloadTickDataForTicker);

        log.info("All downloads completed!");
    }

    /**
     * Synchronizes tick data for a specific ticker from its last recorded date until yesterday.
     *
     * @param ticker The ticker to sync.
     */
    private void downloadTickDataForTicker(Ticker ticker) {
        // Determine the start date: either the day after the last downloaded file, or the ticker's initial date.
        LocalDate startDate = fileRepository.findTopByTickerOrderByFileDateDesc(ticker)
                .map(file -> file.getFileDate().plusDays(1))
                .orElse(ticker.getTickerDate());

        String tickerSymbol = ticker.getTickerSymbol();
        Path outDir = Path.of(appConfig.getDownloadDir(), tickerSymbol);

        try {
            Files.createDirectories(outDir);
        } catch (IOException e) {
            log.error("Failed to create directory for {}: {}", tickerSymbol, outDir, e);
            return;
        }

        LocalDate today = LocalDate.now();
        log.info("Syncing {} from {} to {}", tickerSymbol, startDate, today);
        String downloadPattern = appConfig.getDownloadUrl();

        // Loop through each day and download sequentially
        // Usually, today's data isn't fully available on public archives yet.
        // So, we still check and fail to download the file, if not available.
        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            String dateStr = getBinanceDateString(date);
            String fileName = getBinanceZipFileName(tickerSymbol, dateStr);
            Path localFile = outDir.resolve(fileName);
            String url = downloadPattern.replace("{ticker}", tickerSymbol).replace("{filename}", fileName);

            try {
                if (!Files.exists(localFile)) {
                    log.debug("Downloading {} to {}", url, localFile);
                    downloadFileWithTimeout(url, localFile);
                } else {
                    log.trace("File already exists locally: {}", fileName);
                }
                saveFileRecord(ticker, date, url);
            } catch (IOException e) {
                log.warn("Failed to download {} on {}. It might not be available yet. Error: {}", tickerSymbol, dateStr, e.getMessage());
            }
        }
    }

    /**
     * Downloads a file from a URL with basic timeout handling.
     */
    private void downloadFileWithTimeout(String remoteFileURL, Path localFilePath) throws IOException {
        URLConnection connection = URI.create(remoteFileURL).toURL().openConnection();
        connection.setConnectTimeout(5000); // 5 seconds
        connection.setReadTimeout(10000);    // 10 seconds

        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, localFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }


    private void saveFileRecord(Ticker ticker, LocalDate date, String baseUrl) {

        File file = new File();
        file.setTicker(ticker);
        file.setFileDate(date);
        file.setFileUrl(baseUrl);
        file.setIsProcessed(false);

        try {
            fileRepository.save(file);
            log.info("Saved file for {} on {}", ticker.getTickerSymbol(), date);
        } catch (DataIntegrityViolationException ignore) {
            log.debug("FileRecord already exists for {} on {}. Skipped.", ticker.getTickerSymbol(), date);
        }
    }

}
