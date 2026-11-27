package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyPriceRepository extends JpaRepository<DailyPrice, Long> {

  @Query(
      """
      SELECT CASE WHEN COUNT(tk) > 0 THEN true ELSE false END
      FROM Ticker tk
      WHERE tk.isActive = true
        AND tk.isStale = false
        AND (
          (tk.country = com.alphaflow.persistence.enums.Country.US AND (
            (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk) IS NULL
            OR (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk) < :usExpectedTradingDate
          ))
          OR
          (tk.country = com.alphaflow.persistence.enums.Country.IN AND (
            (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk) IS NULL
            OR (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk) < :inExpectedTradingDate
          ))
        )
      """)
  boolean hasPendingDailyPrices(
      @Param("usExpectedTradingDate") LocalDate usExpectedTradingDate,
      @Param("inExpectedTradingDate") LocalDate inExpectedTradingDate);

  @Query("SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = :ticker")
  LocalDate findLatestPriceDateByTicker(@Param("ticker") Ticker ticker);

  @Query(
      """
          SELECT tk, COALESCE(MAX(dp.priceDate), {d '1900-01-01'})
          FROM Ticker tk
          LEFT JOIN DailyPrice dp ON dp.ticker = tk
          WHERE tk.isActive = true
            AND tk.isStale = false
          GROUP BY tk
      """)
  List<Object[]> findLatestPriceDatesForActiveTickersQuery();

  default Map<Ticker, LocalDate> findLatestPriceDatesForActiveTickers() {
    return findLatestPriceDatesForActiveTickersQuery().stream()
        .collect(
            Collectors.toMap(
                row -> (Ticker) row[0],
                row -> (LocalDate) row[1],
                (left, right) -> left,
                LinkedHashMap::new));
  }

  List<DailyPrice> findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
      Ticker ticker, LocalDate startDate);

  List<DailyPrice> findByTickerOrderByPriceDateAsc(Ticker ticker);

  @Query(
      """
          SELECT dp FROM DailyPrice dp
          JOIN FETCH dp.ticker tk
          WHERE dp.ticker = :ticker
            AND dp.priceDate <= :endDate
          ORDER BY dp.priceDate DESC
      """)
  List<DailyPrice> findLatestByTickerAndEndDate(
      Ticker ticker, LocalDate endDate, Pageable pageable);

  @Query(
      """
          SELECT dp.priceDate FROM DailyPrice dp
          WHERE dp.ticker = :ticker
            AND dp.priceDate <= :endDate
          ORDER BY dp.priceDate DESC
      """)
  List<LocalDate> findRecentPriceDatesUpTo(Ticker ticker, LocalDate endDate, Pageable pageable);

  @Modifying
  @Query("DELETE FROM DailyPrice dp WHERE dp.ticker = :ticker")
  void deleteByTicker(@Param("ticker") Ticker ticker);
}
