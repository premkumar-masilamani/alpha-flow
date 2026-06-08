package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyPriceRepository extends JpaRepository<DailyPrice, Long> {
  @Query(
      """
                SELECT dp FROM DailyPrice dp
                JOIN FETCH dp.ticker tk
                WHERE LOWER(tk.tickerSymbol) = LOWER(:tickerName)
                ORDER BY dp.priceDate DESC
            """)
  List<DailyPrice> findLatestByTickerName(String tickerName, Pageable pageable);

  Optional<DailyPrice> findTopByTickerOrderByPriceDateAsc(Ticker ticker);

  Optional<DailyPrice> findTopByTickerOrderByPriceDateDesc(Ticker ticker);

  @Query(
      """
                SELECT tk, COALESCE(MAX(dp.priceDate), {d '1900-01-01'})
                FROM Ticker tk
                LEFT JOIN DailyPrice dp ON dp.ticker = tk
                WHERE tk.isActive = true
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
                SELECT dp.priceDate FROM DailyPrice dp
                WHERE LOWER(dp.ticker.tickerSymbol) = LOWER(:symbol)
                ORDER BY dp.priceDate DESC
            """)
  List<LocalDate> findRecentPriceDates(String symbol, Pageable pageable);
}
