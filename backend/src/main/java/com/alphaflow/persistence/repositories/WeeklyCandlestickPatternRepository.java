package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyCandlestickPattern;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Repository interface for WeeklyCandlestickPattern entity. */
@Repository
public interface WeeklyCandlestickPatternRepository
    extends JpaRepository<WeeklyCandlestickPattern, Long> {

  Optional<WeeklyCandlestickPattern> findFirstByTickerOrderByPriceDateDesc(Ticker ticker);

  @Query(
      """
        SELECT p FROM WeeklyCandlestickPattern p
        WHERE p.ticker = :ticker
          AND p.priceDate >= :from
          AND p.priceDate <= :to
        ORDER BY p.priceDate ASC
        """)
  List<WeeklyCandlestickPattern> findSeriesBetween(Ticker ticker, LocalDate from, LocalDate to);
}
