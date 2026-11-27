package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyChartPattern;
import com.alphaflow.persistence.enums.ChartPatternStatus;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyChartPatternRepository extends JpaRepository<WeeklyChartPattern, Long> {

  @Query(
      """
      SELECT CASE WHEN COUNT(tk) > 0 THEN true ELSE false END
      FROM Ticker tk
      WHERE tk.isActive = true
        AND EXISTS (SELECT 1 FROM WeeklyPrice wp WHERE wp.ticker = tk)
        AND (
          NOT EXISTS (SELECT 1 FROM WeeklyChartPattern wcp WHERE wcp.ticker = tk)
          OR (SELECT MAX(wcp.priceDate) FROM WeeklyChartPattern wcp WHERE wcp.ticker = tk) <
             (SELECT MAX(wp.priceDate) FROM WeeklyPrice wp WHERE wp.ticker = tk)
          OR (:isTradingWeekday = true AND (SELECT MAX(wp.priceDate) FROM WeeklyPrice wp WHERE wp.ticker = tk) = :currentWeekMonday)
        )
      """)
  boolean hasPendingWeeklyChartPatterns(
      @Param("currentWeekMonday") LocalDate currentWeekMonday,
      @Param("isTradingWeekday") boolean isTradingWeekday);

  default boolean hasPendingWeeklyChartPatterns() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    DayOfWeek dow = today.getDayOfWeek();
    boolean isTradingWeekday = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    LocalDate currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    return hasPendingWeeklyChartPatterns(currentWeekMonday, isTradingWeekday);
  }

  @Query(
      """
      SELECT tk, MAX(wcp.priceDate)
      FROM Ticker tk
      LEFT JOIN WeeklyChartPattern wcp ON wcp.ticker = tk
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

  List<WeeklyChartPattern> findByTickerAndPriceDate(Ticker ticker, LocalDate priceDate);

  Page<WeeklyChartPattern> findByTickerAndPriceDateAndStatusInOrderByEndDateDesc(
      Ticker ticker,
      LocalDate priceDate,
      Collection<ChartPatternStatus> statuses,
      Pageable pageable);

  boolean existsByTickerAndPriceDate(Ticker ticker, LocalDate priceDate);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      DELETE FROM WeeklyChartPattern wcp
      WHERE wcp.ticker = :ticker AND wcp.priceDate = :priceDate
      """)
  void deleteByTickerAndPriceDate(
      @Param("ticker") Ticker ticker, @Param("priceDate") LocalDate priceDate);

  @Modifying
  @Query(
      """
      DELETE FROM WeeklyChartPattern wcp
      WHERE wcp.ticker = :ticker AND wcp.priceDate < :cutoffDate
      """)
  void deleteByTickerAndPriceDateLessThan(
      @Param("ticker") Ticker ticker, @Param("cutoffDate") LocalDate cutoffDate);
}
