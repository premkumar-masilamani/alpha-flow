package com.prem.ta.repo;

import com.prem.ta.domain.CryptoFile;
import com.prem.ta.domain.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface CryptoFileRepository extends JpaRepository<CryptoFile, Long> {

    Optional<CryptoFile> findTopByTickerOrderByFileDateDesc(Ticker ticker);

    Optional<CryptoFile> findByTickerAndFileDate(Ticker ticker, OffsetDateTime fileDate);
}
