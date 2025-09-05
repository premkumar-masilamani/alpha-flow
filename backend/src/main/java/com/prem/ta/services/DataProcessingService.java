package com.prem.ta.services;

import com.prem.ta.entities.File;
import com.prem.ta.repositories.FileRepository;
import java.util.List;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

@Service
public class DataProcessingService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DataProcessingService.class);
    private final FileRepository fileRepository;
    private final FileProcessingWorker fileProcessingWorker;
    private final ExecutorService executor;
    private static final int BATCH_SIZE = 100;

    public DataProcessingService(
        FileRepository fileRepository,
        FileProcessingWorker fileProcessingWorker
    ) {
        this.fileRepository = fileRepository;
        this.fileProcessingWorker = fileProcessingWorker;
        this.executor = Executors.newFixedThreadPool(5);
    }

    public void processData() {
        log.info("Starting data processing...");
        List<File> filesToProcess = fileRepository.findByDownloadedTrueAndProcessedFalse();
        log.info("Found {} files to process.", filesToProcess.size());

        for (int i = 0; i < filesToProcess.size(); i += BATCH_SIZE) {
            List<File> batch = filesToProcess.subList(i, Math.min(i + BATCH_SIZE, filesToProcess.size()));
            log.info("Processing batch of {} files (from index {} to {}).", batch.size(), i, i + batch.size() - 1);

            List<CompletableFuture<Void>> futures = batch.stream()
                    .map(file -> CompletableFuture.runAsync(() -> {
                        try {
                            fileProcessingWorker.processFile(file);
                        } catch (Exception e) {
                            log.error("Error processing file {}", file, e);
                        }
                    }, executor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        log.info("Data processing completed.");
    }

    @Override
    public void destroy() {
        log.info("Shutting down data processing executor.");
        executor.shutdown();
    }
}
