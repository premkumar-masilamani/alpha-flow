package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.DailyCandlestickPattern;
import com.alphaflow.persistence.entities.Ticker;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
      SELECT CASE WHEN COUNT(tk) > 0 THEN true ELSE false END
      FROM Ticker tk
      WHERE tk.isActive = true
        AND EXISTS (SELECT 1 FROM DailyPrice dp WHERE dp.ticker = tk)
        AND (
          NOT EXISTS (SELECT 1 FROM DailyCandlestickPattern dcp WHERE dcp.ticker = tk)
          OR (SELECT MAX(dcp.priceDate) FROM DailyCandlestickPattern dcp WHERE dcp.ticker = tk) <
             (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk)
        )
      """)
  boolean hasPendingDailyCandlestickPatterns();

  @Query("SELECT MAX(dcp.priceDate) FROM DailyCandlestickPattern dcp WHERE dcp.ticker = :ticker")
  LocalDate findLatestPriceDateByTicker(@Param("ticker") Ticker ticker);

  @Query(
      """
  @Query(
      """
      SELECT tk, MAX(dcp.priceDate)
      FROM Ticker tk
      LEFT JOIN DailyCandlestickPattern dcp ON dcp.ticker = tk
      WHERE tk.isActive = true
      GROUP BY tk
      """)
  List<Object[]> findLatestPriceDatesForActiveTickersQuery();

  default Map<Ticker, LocalDate> findLatestPriceDatesForActiveTickers() {
    Map<Ticker, LocalDate> map = new LinkedHashMap<>();
    for (Object[] row : findLatestPriceDatesForActiveTickersQuery()) {
      map.put((Ticker) row[0], (LocalDate) row[1]);
    }
    return map;
  }

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
