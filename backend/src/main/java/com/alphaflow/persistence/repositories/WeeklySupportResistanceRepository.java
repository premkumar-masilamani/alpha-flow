package com.alphaflow.persistence.repositories;

import com.alphaflow.engine.calculators.dtos.TickerSyncState;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklySupportResistance;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklySupportResistanceRepository
    extends JpaRepository<WeeklySupportResistance, Long> {

  @Query(
      """
      SELECT new com.alphaflow.engine.calculators.dtos.TickerSyncState(
          tk,
          (SELECT MAX(wp.priceDate) FROM WeeklyPrice wp WHERE wp.ticker = tk),
          (SELECT MAX(wsr.priceDate) FROM WeeklySupportResistance wsr WHERE wsr.ticker = tk)
      )
      FROM Ticker tk
      WHERE tk.isActive = true
      """)
  List<TickerSyncState> findSyncStatesForActiveTickers();

  List<WeeklySupportResistance> findByTickerAndPriceDate(Ticker ticker, LocalDate priceDate);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      DELETE FROM WeeklySupportResistance wsr
      WHERE wsr.ticker = :ticker AND wsr.priceDate = :priceDate
      """)
  void deleteByTickerAndPriceDate(
      @Param("ticker") Ticker ticker, @Param("priceDate") LocalDate priceDate);

  @Modifying
  @Query(
      """
      DELETE FROM WeeklySupportResistance wsr
      WHERE wsr.ticker = :ticker AND wsr.priceDate < :cutoffDate
      """)
  void deleteByTickerAndPriceDateLessThan(
      @Param("ticker") Ticker ticker, @Param("cutoffDate") LocalDate cutoffDate);
}
