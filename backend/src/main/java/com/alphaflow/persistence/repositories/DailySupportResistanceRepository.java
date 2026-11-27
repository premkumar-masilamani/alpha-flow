package com.alphaflow.persistence.repositories;

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
      SELECT CASE WHEN COUNT(tk) > 0 THEN true ELSE false END
      FROM Ticker tk
      WHERE tk.isActive = true
        AND EXISTS (SELECT 1 FROM DailyPrice dp WHERE dp.ticker = tk)
        AND (
          NOT EXISTS (SELECT 1 FROM DailySupportResistance dsr WHERE dsr.ticker = tk)
          OR (SELECT MAX(dsr.priceDate) FROM DailySupportResistance dsr WHERE dsr.ticker = tk) <
             (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk)
        )
      """)
  boolean hasPendingDailySupportResistance();

  @Query("SELECT MAX(dsr.priceDate) FROM DailySupportResistance dsr WHERE dsr.ticker = :ticker")
  LocalDate findLatestPriceDateByTicker(@Param("ticker") Ticker ticker);

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
