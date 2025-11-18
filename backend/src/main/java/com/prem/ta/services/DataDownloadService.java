package com.prem.ta.services;

import com.prem.ta.configs.AppConfig;
import com.prem.ta.configs.Constants;
import com.prem.ta.entities.File;
import com.prem.ta.entities.Ticker;
import com.prem.ta.repositories.FileRepository;
import com.prem.ta.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

@Service
public class DataDownloadService {

    private static final Logger log = LoggerFactory.getLogger(DataDownloadService.class);
    private final AppConfig appConfig;
    private final TickerRepository tickerRepository;
    private final FileRepository fileRepository;

    public DataDownloadService(
            AppConfig appConfig,
            TickerRepository tickerRepository,
            FileRepository fileRepository) {
        this.appConfig = appConfig;
        this.tickerRepository = tickerRepository;
        this.fileRepository = fileRepository;
    }

    public void download() {
        log.info("Download URL: {}", appConfig.getDownloadUrl());
        log.info("Download directory: {}", appConfig.getDownloadDir());

        tickerRepository
                .findAll()
                .forEach(ticker -> {
                    downloadTicker(ticker);
                });

        log.info("All downloads completed!");
    }

    public void downloadTicker(Ticker ticker) {

        LocalDate startDate = fileRepository
                .findTopByTickerOrderByFileDateDesc(ticker)
                .map(file -> file.getFileDate().plusDays(1))
                .orElse(ticker.getStartDate());

        LocalDate today = LocalDate.now();
        if (startDate.isAfter(today)) {
            return; // nothing to download
        }

        String tickerSymbol = ticker.getSymbol();
        Path outDir = Path.of(appConfig.getDownloadDir(), tickerSymbol);

        try {
            Files.createDirectories(outDir);
        } catch (IOException e) {
            log.error("Error creating directory {}:", outDir, e);
            return;
        }

        log.info("Syncing ticker {} from {}", ticker.getSymbol(), startDate);
        String downloadPattern = appConfig.getDownloadUrl();
        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {

            String dateStr = Constants.getBinanceFormattedDateString(date);
            String fileName = Constants.getBinanceZipFileName(tickerSymbol, dateStr);
            Path localFile = outDir.resolve(fileName);
            String url = downloadPattern
                    .replace("{ticker}", tickerSymbol)
                    .replace("{filename}", fileName);

            if (Files.exists(localFile)) {
                saveFileRecord(ticker, date, url);
                continue;
            }

            try {
                log.info("Downloading {}", url);
                downloadFile(url, localFile);
                saveFileRecord(ticker, date, url);

            } catch (IOException e) {
                log.error("Download failed for {} on {}: ", tickerSymbol, dateStr, e);
            }
        }
    }

    @Transactional
    protected void saveFileRecord(Ticker ticker, LocalDate date, String baseUrl) {

        File file = new File();
        file.setTicker(ticker);
        file.setFileDate(date);
        file.setFileUrl(baseUrl);
        file.setIsDownloaded(true);
        file.setIsProcessed(false);

        try {
            fileRepository.save(file);
            log.info("Saved file file for {} on {}", ticker.getSymbol(), date);
        } catch (DataIntegrityViolationException ignore) {
            log.debug("FileRecord already exists for {} on {}. Skipped.", ticker.getSymbol(), date);
        }
    }

    private void downloadFile(String remoteFileURL, Path localFilePath) throws IOException {
        InputStream in = URI.create(remoteFileURL)
                .toURL()
                .openConnection()
                .getInputStream();
        Files.copy(in, localFilePath, StandardCopyOption.REPLACE_EXISTING);
    }

}
