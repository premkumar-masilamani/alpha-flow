package com.alphaflow.engine.repositories;

import com.alphaflow.engine.entities.TickDataFile;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DataFileRepository extends JpaRepository<TickDataFile, Long> {

    Optional<TickDataFile> findTopByTickerOrderByDataFileDateDesc(Ticker ticker);

    @EntityGraph(attributePaths = "ticker")
    Page<TickDataFile> findByIsProcessedFalse(Pageable pageable);

}
