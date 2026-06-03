package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyPriceRepository extends JpaRepository<WeeklyPrice, Long> {

  Optional<WeeklyPrice> findTopByTickerOrderByPriceDateDesc(Ticker ticker);

  List<WeeklyPrice> findByTickerAndPriceDateGreaterThanEqual(Ticker ticker, LocalDate priceDate);

  List<WeeklyPrice> findByTickerOrderByPriceDateAsc(Ticker ticker);

  @Query(
      """
                SELECT wp FROM WeeklyPrice wp
                JOIN FETCH wp.ticker tk
                WHERE LOWER(tk.tickerSymbol) = LOWER(:tickerName)
                ORDER BY wp.priceDate DESC
            """)
  List<WeeklyPrice> findLatestByTickerName(String tickerName, Pageable pageable);

  @Query(
      """
                SELECT wp.priceDate FROM WeeklyPrice wp
                WHERE LOWER(wp.ticker.tickerSymbol) = LOWER(:symbol)
                ORDER BY wp.priceDate DESC
            """)
  List<LocalDate> findRecentPriceDates(String symbol, Pageable pageable);
}
