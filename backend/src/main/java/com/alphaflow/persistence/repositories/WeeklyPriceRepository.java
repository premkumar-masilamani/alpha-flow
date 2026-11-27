package com.alphaflow.persistence.repositories;

import com.alphaflow.engine.calculators.dtos.TickerSyncState;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyPriceRepository extends JpaRepository<WeeklyPrice, Long> {

  @Query(
      """
      SELECT new com.alphaflow.engine.calculators.dtos.TickerSyncState(
          tk,
          (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk),
          (SELECT MAX(wp.priceDate) FROM WeeklyPrice wp WHERE wp.ticker = tk)
      )
      FROM Ticker tk
      WHERE tk.isActive = true
      """)
  List<TickerSyncState> findSyncStatesForActiveTickers();

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
