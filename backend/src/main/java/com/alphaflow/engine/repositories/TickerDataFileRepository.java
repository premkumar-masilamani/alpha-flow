package com.alphaflow.engine.repositories;

import com.alphaflow.engine.entities.TickerDataFile;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TickerDataFileRepository extends JpaRepository<TickerDataFile, Long> {

    Optional<TickerDataFile> findTopByTickerOrderByDataFileDateDesc(Ticker ticker);

    @EntityGraph(attributePaths = "ticker")
    Page<TickerDataFile> findByIsProcessedFalse(Pageable pageable);

}
