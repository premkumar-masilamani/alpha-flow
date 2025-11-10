package com.prem.ta.repositories;

import com.prem.ta.entities.FileRecord;
import com.prem.ta.entities.FileRecordId;
import com.prem.ta.entities.Ticker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface FileRepository
        extends JpaRepository<FileRecord, FileRecordId> {
    // Latest file for a ticker (based on file_date DESC)
    Optional<FileRecord> findTopByTickerOrderByFileDateDesc(Ticker ticker);

    // Pending files (with ticker eagerly loaded)
    @EntityGraph(attributePaths = "ticker")
    Page<FileRecord> findByIsDownloadedTrueAndIsProcessedFalse(
            Pageable pageable
    );

    Optional<FileRecord> findByTickerAndFileDate(
            Ticker ticker,
            OffsetDateTime fileDate
    );
}
