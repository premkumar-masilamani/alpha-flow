package com.prem.ta.repositories;

import com.prem.ta.entities.File;
import com.prem.ta.entities.Ticker;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    Optional<File> findTopByTickerOrderByFileDateDesc(Ticker ticker);

    Optional<File> findByTickerAndFileDate(
        Ticker ticker,
        OffsetDateTime fileDate
    );
    List<File> findByDownloadedTrueAndProcessedFalse();
}
