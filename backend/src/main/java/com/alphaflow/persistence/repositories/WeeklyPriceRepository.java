package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyPriceRepository extends JpaRepository<WeeklyPrice, Long> {

  @Query(
      """
      SELECT CASE WHEN COUNT(tk) > 0 THEN true ELSE false END
      FROM Ticker tk
      WHERE tk.isActive = true
        AND EXISTS (SELECT 1 FROM DailyPrice dp WHERE dp.ticker = tk)
        AND (
          NOT EXISTS (SELECT 1 FROM WeeklyPrice wp WHERE wp.ticker = tk)
          OR (:isTradingWeekday = true AND (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk) >= :currentWeekMonday)
          OR (:isTradingWeekday = false AND (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk) >= :currentWeekMonday
              AND (SELECT MAX(wp.priceDate) FROM WeeklyPrice wp WHERE wp.ticker = tk) < :currentWeekMonday)
        )
      """)
  boolean hasPendingWeeklyPrices(
      @Param("currentWeekMonday") LocalDate currentWeekMonday,
      @Param("isTradingWeekday") boolean isTradingWeekday);

  default boolean hasPendingWeeklyPrices() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    DayOfWeek dow = today.getDayOfWeek();
    boolean isTradingWeekday = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    LocalDate currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    return hasPendingWeeklyPrices(currentWeekMonday, isTradingWeekday);
  }

  @Query("SELECT MAX(wp.priceDate) FROM WeeklyPrice wp WHERE wp.ticker = :ticker")
  LocalDate findLatestPriceDateByTicker(@Param("ticker") Ticker ticker);

  @Query(
      """
      SELECT tk, MAX(wp.priceDate)
      FROM Ticker tk
      LEFT JOIN WeeklyPrice wp ON wp.ticker = tk
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

  List<WeeklyPrice> findByTickerAndPriceDateGreaterThanEqual(Ticker ticker, LocalDate priceDate);

  List<WeeklyPrice> findByTickerOrderByPriceDateAsc(Ticker ticker);

  @Query(
      """
        SELECT wp FROM WeeklyPrice wp
        JOIN FETCH wp.ticker tk
        WHERE wp.ticker = :ticker
        ORDER BY wp.priceDate DESC
      """)
  List<WeeklyPrice> findLatestByTicker(Ticker ticker, Pageable pageable);

  @Query(
      """
        SELECT wp.priceDate FROM WeeklyPrice wp
        WHERE wp.ticker = :ticker
          AND wp.priceDate <= :endDate
        ORDER BY wp.priceDate DESC
      """)
  List<LocalDate> findRecentPriceDatesUpTo(Ticker ticker, LocalDate endDate, Pageable pageable);
}
