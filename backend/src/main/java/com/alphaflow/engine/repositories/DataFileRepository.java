package com.alphaflow.engine.repositories;

import com.alphaflow.engine.entities.DataFile;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DataFileRepository extends JpaRepository<DataFile, Long> {

    Optional<DataFile> findTopByTickerOrderByDataFileDateDesc(Ticker ticker);

    @EntityGraph(attributePaths = "ticker")
    Page<DataFile> findByIsProcessedFalse(Pageable pageable);

}
