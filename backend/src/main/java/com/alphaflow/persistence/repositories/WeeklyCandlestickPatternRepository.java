package com.alphaflow.persistence.repositories;

import com.alphaflow.engine.calculators.dtos.TickerSyncState;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyCandlestickPattern;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyCandlestickPatternRepository
    extends JpaRepository<WeeklyCandlestickPattern, Long> {

  @Query(
      """
      SELECT new com.alphaflow.engine.calculators.dtos.TickerSyncState(
          tk,
          (SELECT MAX(wp.priceDate) FROM WeeklyPrice wp WHERE wp.ticker = tk),
          (SELECT MAX(wcp.priceDate) FROM WeeklyCandlestickPattern wcp WHERE wcp.ticker = tk)
      )
      FROM Ticker tk
      WHERE tk.isActive = true
      """)
  List<TickerSyncState> findSyncStatesForActiveTickers();

  @Query(
      """
      SELECT p FROM WeeklyCandlestickPattern p
      WHERE p.ticker = :ticker
        AND p.priceDate >= :from
        AND p.priceDate <= :to
      ORDER BY p.priceDate ASC
      """)
  List<WeeklyCandlestickPattern> findSeriesBetween(Ticker ticker, LocalDate from, LocalDate to);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      DELETE FROM WeeklyCandlestickPattern wcp
      WHERE wcp.ticker = :ticker AND wcp.priceDate >= :priceDate
      """)
  void deleteByTickerAndPriceDateGreaterThanEqual(
      @Param("ticker") Ticker ticker, @Param("priceDate") LocalDate priceDate);
}
