package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyCandlestickPattern;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
      SELECT CASE WHEN COUNT(tk) > 0 THEN true ELSE false END
      FROM Ticker tk
      WHERE tk.isActive = true
        AND EXISTS (SELECT 1 FROM WeeklyPrice wp WHERE wp.ticker = tk)
        AND (
          NOT EXISTS (SELECT 1 FROM WeeklyCandlestickPattern wcp WHERE wcp.ticker = tk)
          OR (SELECT MAX(wcp.priceDate) FROM WeeklyCandlestickPattern wcp WHERE wcp.ticker = tk) <
             (SELECT MAX(wp.priceDate) FROM WeeklyPrice wp WHERE wp.ticker = tk)
          OR (:isTradingWeekday = true AND (SELECT MAX(wp.priceDate) FROM WeeklyPrice wp WHERE wp.ticker = tk) = :currentWeekMonday)
        )
      """)
  boolean hasPendingWeeklyCandlestickPatterns(
      @Param("currentWeekMonday") LocalDate currentWeekMonday,
      @Param("isTradingWeekday") boolean isTradingWeekday);

  default boolean hasPendingWeeklyCandlestickPatterns() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    DayOfWeek dow = today.getDayOfWeek();
    boolean isTradingWeekday = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    LocalDate currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    return hasPendingWeeklyCandlestickPatterns(currentWeekMonday, isTradingWeekday);
  }

  @Query("SELECT MAX(wcp.priceDate) FROM WeeklyCandlestickPattern wcp WHERE wcp.ticker = :ticker")
  LocalDate findLatestPriceDateByTicker(@Param("ticker") Ticker ticker);

  @Query(
      """
      SELECT tk, MAX(wcp.priceDate)
      FROM Ticker tk
      LEFT JOIN WeeklyCandlestickPattern wcp ON wcp.ticker = tk
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

  @Modifying
  @Query("DELETE FROM WeeklyCandlestickPattern wcp WHERE wcp.ticker = :ticker")
  void deleteByTicker(@Param("ticker") Ticker ticker);
}
