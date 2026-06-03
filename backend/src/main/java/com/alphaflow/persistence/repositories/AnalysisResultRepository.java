package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.AnalysisResult;
import com.alphaflow.persistence.entities.Ticker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
  Optional<AnalysisResult> findByTicker(Ticker ticker);

  Optional<AnalysisResult> findByTickerTickerSymbolIgnoreCase(String symbol);
}
