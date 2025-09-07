package com.prem.ta.services;

import com.prem.ta.configs.Utils;
import com.prem.ta.entities.File;
import com.prem.ta.repositories.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DataProcessingService implements com.prem.ta.services.Service {

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

    @Override
    public void doService() {
        log.info("Starting data processing...");
        Pageable pageable = PageRequest.of(0, Utils.PAGE_SIZE);
        Page<File> filePage;

        do {
            filePage = fileRepository.findByDownloadedTrueAndProcessedFalse(pageable);
            log.info("Found {} files to process in this batch.", filePage.getNumberOfElements());
            filePage.getContent().forEach(fileProcessingWorker::processFile);
            pageable = filePage.nextPageable();
        } while (filePage.hasNext());

        log.info("Data processing completed.");
    }
}
