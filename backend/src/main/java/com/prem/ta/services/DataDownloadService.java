package com.prem.ta.services;

import com.prem.ta.configs.AppConfig;
import com.prem.ta.configs.Constants;
import com.prem.ta.entities.FileRecord;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class DataDownloadService implements com.prem.ta.services.Service {

    private static final Logger log = LoggerFactory.getLogger(
            DataDownloadService.class
    );
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

    @Override
    public void doService() {
        log.info("Download URL: {}", appConfig.getDownloadUrl());
        log.info("Download directory: {}", appConfig.getDownloadDir());

        tickerRepository
                .findAll()
                .forEach(ticker -> {
                    log.info(
                            "Syncing ticker {} starting from {}",
                            ticker.getSymbol(),
                            ticker.getStartDate()
                    );
                    downloadTicker(ticker);
                });

        log.info("All downloads completed!");
    }

    public void downloadTicker(Ticker ticker) {

        LocalDate startDate = fileRepository
                .findTopByTickerOrderByFileDateDesc(ticker)
                .map(fileRecord -> fileRecord.getFileDate().toLocalDate().plusDays(1))
                .orElse(ticker.getStartDate().toLocalDate());

        LocalDate today = LocalDate.now();
        if (startDate.isAfter(today)) {
            return; // nothing to download
        }

        String tickerSymbol = ticker.getSymbol();
        Path outDir = Path.of(appConfig.getDownloadDir(), tickerSymbol);

        try {
            Files.createDirectories(outDir);
        } catch (IOException e) {
            log.error("Error creating directory {}: {}", outDir, e.getMessage());
            return;
        }

        String downloadPattern = appConfig.getDownloadUrl();
        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {

            String dateStr = date.format(Constants.getDateFormatter());
            String fileName = tickerSymbol + "-trades-" + dateStr + ".zip";
            Path localFile = outDir.resolve(fileName);

            if (Files.exists(localFile)) {
                continue;
            }

            String url = downloadPattern
                    .replace("{ticker}", tickerSymbol)
                    .replace("{filename}", fileName);

            try {
                log.info("Downloading {}", url);
                downloadFile(url, localFile);
                saveFileRecord(ticker, date, url);

            } catch (IOException e) {
                log.error("Download failed for {} {}: {}", tickerSymbol, dateStr, e.getMessage());
            }
        }
    }

    @Transactional
    protected void saveFileRecord(Ticker ticker, LocalDate date, String baseUrl) {

        OffsetDateTime fileDate = date.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        FileRecord record = new FileRecord();
        record.setTicker(ticker);
        record.setFileDate(fileDate);
        record.setFileDownloadUrl(baseUrl);
        record.setIsDownloaded(true);
        record.setIsProcessed(false);
        record.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        record.setUpdatedBy(getClass().getSimpleName());

        try {
            fileRepository.save(record);
            log.info("Saved file record for {} on {}", ticker.getSymbol(), date);
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
