package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklySupportResistance;
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
public interface WeeklySupportResistanceRepository
    extends JpaRepository<WeeklySupportResistance, Long> {

  @Query(
      """
      SELECT CASE WHEN COUNT(tk) > 0 THEN true ELSE false END
      FROM Ticker tk
      WHERE tk.isActive = true
        AND EXISTS (SELECT 1 FROM WeeklyPrice wp WHERE wp.ticker = tk)
        AND (
          NOT EXISTS (SELECT 1 FROM WeeklySupportResistance wsr WHERE wsr.ticker = tk)
          OR (SELECT MAX(wsr.priceDate) FROM WeeklySupportResistance wsr WHERE wsr.ticker = tk) <
             (SELECT MAX(wp.priceDate) FROM WeeklyPrice wp WHERE wp.ticker = tk)
          OR (:isTradingWeekday = true AND (SELECT MAX(wp.priceDate) FROM WeeklyPrice wp WHERE wp.ticker = tk) = :currentWeekMonday)
        )
      """)
  boolean hasPendingWeeklySupportResistance(
      @Param("currentWeekMonday") LocalDate currentWeekMonday,
      @Param("isTradingWeekday") boolean isTradingWeekday);

  default boolean hasPendingWeeklySupportResistance() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    DayOfWeek dow = today.getDayOfWeek();
    boolean isTradingWeekday = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    LocalDate currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    return hasPendingWeeklySupportResistance(currentWeekMonday, isTradingWeekday);
  }

  @Query("SELECT MAX(wsr.priceDate) FROM WeeklySupportResistance wsr WHERE wsr.ticker = :ticker")
  LocalDate findLatestPriceDateByTicker(@Param("ticker") Ticker ticker);

  @Query(
      """
      SELECT tk, MAX(wsr.priceDate)
      FROM Ticker tk
      LEFT JOIN WeeklySupportResistance wsr ON wsr.ticker = tk
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

  @Modifying
  @Query("DELETE FROM WeeklySupportResistance wsr WHERE wsr.ticker = :ticker")
  void deleteByTicker(@Param("ticker") Ticker ticker);
}
