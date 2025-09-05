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

        if (filesToProcess.isEmpty()) {
            log.info("No files to process.");
            return;
        }

        // Submit all tasks asynchronously using the executor
        List<CompletableFuture<Void>> futures = filesToProcess.stream()
                .map(file -> CompletableFuture.runAsync(() -> {
                    try {
                        fileProcessingWorker.processFile(file);
                    } catch (Exception e) {
                        log.error("Error processing file {}", file, e);
                    }
                }, executor))
                .toList();

        // Wait for all tasks to finish
        CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            allDone.join();
        } catch (CompletionException e) {
            log.error("One or more files failed during processing.", e);
        }
        log.info("Data processing completed.");
    }

    @Override
    public void destroy() {
        log.info("Shutting down data processing executor.");
        executor.shutdown();
    }
}
