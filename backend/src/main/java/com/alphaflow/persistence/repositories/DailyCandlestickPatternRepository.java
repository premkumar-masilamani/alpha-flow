package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.DailyCandlestickPattern;
import com.alphaflow.persistence.entities.Ticker;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyCandlestickPatternRepository
    extends JpaRepository<DailyCandlestickPattern, Long> {

  Optional<DailyCandlestickPattern> findFirstByTickerOrderByPriceDateDesc(Ticker ticker);

  @Query(
      """
        SELECT p FROM DailyCandlestickPattern p
        WHERE p.ticker = :ticker
          AND p.priceDate >= :from
          AND p.priceDate <= :to
        ORDER BY p.priceDate ASC
        """)
  List<DailyCandlestickPattern> findSeriesBetween(Ticker ticker, LocalDate from, LocalDate to);
}
