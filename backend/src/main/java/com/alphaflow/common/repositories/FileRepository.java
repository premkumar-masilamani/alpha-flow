package com.alphaflow.common.repositories;

import com.alphaflow.common.entities.File;
import com.alphaflow.common.entities.Ticker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {

    Optional<File> findTopByTickerOrderByFileDateDesc(Ticker ticker);

    @EntityGraph(attributePaths = "ticker")
    Page<File> findByIsProcessedFalse(Pageable pageable);

}
