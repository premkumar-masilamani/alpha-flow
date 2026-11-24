package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.dtos.TickerSyncState;
import com.alphaflow.persistence.entities.DailySupportResistance;
import com.alphaflow.persistence.entities.Ticker;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DailySupportResistanceRepository
    extends JpaRepository<DailySupportResistance, Long> {

  @Query(
      """
      SELECT new com.alphaflow.persistence.dtos.TickerSyncState(
          tk,
          (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk),
          (SELECT MAX(dsr.priceDate) FROM DailySupportResistance dsr WHERE dsr.ticker = tk)
      )
      FROM Ticker tk
      WHERE tk.isActive = true
      """)
  List<TickerSyncState> findSyncStatesForActiveTickers();

  List<DailySupportResistance> findByTickerAndPriceDate(Ticker ticker, LocalDate priceDate);

  @Modifying
  @Query(
      """
      DELETE FROM DailySupportResistance dsr
      WHERE dsr.ticker = :ticker AND dsr.priceDate < :cutoffDate
      """)
  void deleteByTickerAndPriceDateLessThan(
      @Param("ticker") Ticker ticker, @Param("cutoffDate") LocalDate cutoffDate);
}
