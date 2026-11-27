package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.DailyChartPattern;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.ChartPatternStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyChartPatternRepository extends JpaRepository<DailyChartPattern, Long> {

  @Query(
      """
      SELECT CASE WHEN COUNT(tk) > 0 THEN true ELSE false END
      FROM Ticker tk
      WHERE tk.isActive = true
        AND EXISTS (SELECT 1 FROM DailyPrice dp WHERE dp.ticker = tk)
        AND (
          NOT EXISTS (SELECT 1 FROM DailyChartPattern dcp WHERE dcp.ticker = tk)
          OR (SELECT MAX(dcp.priceDate) FROM DailyChartPattern dcp WHERE dcp.ticker = tk) <
             (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk)
        )
      """)
  boolean hasPendingDailyChartPatterns();

  @Query(
      """
      SELECT tk
      FROM Ticker tk
      WHERE tk.isActive = true
        AND EXISTS (SELECT 1 FROM DailyPrice dp WHERE dp.ticker = tk)
        AND (
          NOT EXISTS (SELECT 1 FROM DailyChartPattern dcp WHERE dcp.ticker = tk)
          OR (SELECT MAX(dcp.priceDate) FROM DailyChartPattern dcp WHERE dcp.ticker = tk) <
             (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk)
        )
      """)
  List<Ticker> findTickersWithPendingDailyChartPatterns();

  List<DailyChartPattern> findByTickerAndPriceDate(Ticker ticker, LocalDate priceDate);

  Page<DailyChartPattern> findByTickerAndPriceDateAndStatusInOrderByEndDateDesc(
      Ticker ticker,
      LocalDate priceDate,
      Collection<ChartPatternStatus> statuses,
      Pageable pageable);

  @Modifying
  @Query(
      """
      DELETE FROM DailyChartPattern dcp
      WHERE dcp.ticker = :ticker AND dcp.priceDate < :cutoffDate
      """)
  void deleteByTickerAndPriceDateLessThan(
      @Param("ticker") Ticker ticker, @Param("cutoffDate") LocalDate cutoffDate);

  @Modifying
  @Query("DELETE FROM DailyChartPattern dcp WHERE dcp.ticker = :ticker")
  void deleteByTicker(@Param("ticker") Ticker ticker);
}
