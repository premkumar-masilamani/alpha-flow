package com.prem.ta.repositories;

import com.prem.ta.entities.FileRecord;
import com.prem.ta.entities.Ticker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileRecord, Long> {

    // Latest file for a ticker
    Optional<FileRecord> findTopByTickerOrderByFileDateDesc(Ticker ticker);

    // Specific file lookup
    Optional<FileRecord> findByTickerAndFileDate(Ticker ticker, OffsetDateTime fileDate);

    // Pending files (with ticker eagerly loaded)
    @EntityGraph(attributePaths = "ticker")
    Page<FileRecord> findByDownloadedTrueAndProcessedFalse(Pageable pageable);

    // Additional useful queries
    List<FileRecord> findByTickerAndProcessedFalse(Ticker ticker);

    long countByProcessedFalse();
}
