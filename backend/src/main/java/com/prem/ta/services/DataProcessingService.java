package com.prem.ta.services;

import com.prem.ta.entities.File;
import com.prem.ta.repositories.FileRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DataProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DataProcessingService.class);
    private final FileRepository fileRepository;
    private final FileProcessingWorker fileProcessingWorker;

    public DataProcessingService(
        FileRepository fileRepository,
        FileProcessingWorker fileProcessingWorker
    ) {
        this.fileRepository = fileRepository;
        this.fileProcessingWorker = fileProcessingWorker;
    }

    public void processData() {
        log.info("Starting data processing...");
        List<File> filesToProcess = fileRepository.findByDownloadedTrueAndProcessedFalse();
        log.info("Found {} files to process.", filesToProcess.size());
        filesToProcess.forEach(fileProcessingWorker::processFile);
        log.info("Data processing completed.");
    }
}
