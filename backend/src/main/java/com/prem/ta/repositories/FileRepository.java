package com.prem.ta.repositories;

import com.prem.ta.entities.File;
import com.prem.ta.entities.Ticker;
import java.time.OffsetDateTime;
import com.prem.ta.entities.File;
import com.prem.ta.entities.Ticker;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    Optional<File> findTopByTickerOrderByFileDateDesc(Ticker ticker);

    Optional<File> findByTickerAndFileDate(
        Ticker ticker,
        OffsetDateTime fileDate
    );
    @Query("SELECT f FROM File f JOIN FETCH f.ticker WHERE f.downloaded = true AND f.processed = false")
    Page<File> findByDownloadedTrueAndProcessedFalse(Pageable pageable);
}
