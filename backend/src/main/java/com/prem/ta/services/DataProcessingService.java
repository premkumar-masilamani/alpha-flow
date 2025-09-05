package com.prem.ta.services;

import com.prem.ta.entities.File;
import com.prem.ta.repositories.FileRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
        this.executor = Executors.newFixedThreadPool(10);
    }

    public void processData() {
        log.info("Starting data processing...");
        List<File> filesToProcess = fileRepository.findByDownloadedTrueAndProcessedFalse();
        log.info("Found {} files to process.", filesToProcess.size());

        int batchSize = 10;
        for (int i = 0; i < filesToProcess.size(); i += batchSize) {
            List<File> batch = filesToProcess.subList(i, Math.min(i + batchSize, filesToProcess.size()));
            log.info("Processing batch of {} files (from index {} to {}).", batch.size(), i, i + batch.size() -1);
            CountDownLatch latch = new CountDownLatch(batch.size());
            batch.forEach(file -> {
                executor.submit(() -> {
                    try {
                        fileProcessingWorker.processFile(file);
                    } finally {
                        latch.countDown();
                    }
                });
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Data processing batch was interrupted.", e);
            }
        }
        log.info("Data processing completed.");
    }

    @Override
    public void destroy() {
        log.info("Shutting down data processing executor.");
        executor.shutdown();
    }
}
