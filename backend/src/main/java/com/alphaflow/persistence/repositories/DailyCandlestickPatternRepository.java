package com.alphaflow.persistence.repositories;

import com.alphaflow.engine.calculators.dtos.TickerSyncState;
import com.alphaflow.persistence.entities.DailyCandlestickPattern;
import com.alphaflow.persistence.entities.Ticker;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyCandlestickPatternRepository
    extends JpaRepository<DailyCandlestickPattern, Long> {

  @Query(
      """
      SELECT new com.alphaflow.engine.calculators.dtos.TickerSyncState(
          tk,
          (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk),
          (SELECT MAX(dcp.priceDate) FROM DailyCandlestickPattern dcp WHERE dcp.ticker = tk)
      )
      FROM Ticker tk
      WHERE tk.isActive = true
      """)
  List<TickerSyncState> findSyncStatesForActiveTickers();

  @Query(
      """
      SELECT p FROM DailyCandlestickPattern p
      WHERE p.ticker = :ticker
        AND p.priceDate >= :from
        AND p.priceDate <= :to
      ORDER BY p.priceDate ASC
      """)
  List<DailyCandlestickPattern> findSeriesBetween(Ticker ticker, LocalDate from, LocalDate to);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      DELETE FROM DailyCandlestickPattern dcp
      WHERE dcp.ticker = :ticker AND dcp.priceDate >= :priceDate
      """)
  void deleteByTickerAndPriceDateGreaterThanEqual(
      @Param("ticker") Ticker ticker, @Param("priceDate") LocalDate priceDate);
}
