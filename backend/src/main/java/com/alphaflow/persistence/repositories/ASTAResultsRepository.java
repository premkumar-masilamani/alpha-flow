package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.ASTAResults;
import com.alphaflow.persistence.entities.Ticker;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ASTAResultsRepository extends JpaRepository<ASTAResults, Long> {
  Optional<ASTAResults> findTopByTickerOrderByPriceDateDesc(Ticker ticker);

  Optional<ASTAResults> findByTickerAndPriceDate(Ticker ticker, LocalDate priceDate);

  java.util.List<ASTAResults> findByTickerAndPriceDateGreaterThanEqual(
      Ticker ticker, LocalDate priceDate);
}
