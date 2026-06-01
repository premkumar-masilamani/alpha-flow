package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyPriceRepository extends JpaRepository<DailyPrice, Long> {

    @Query("""
                SELECT dp FROM DailyPrice dp
                JOIN FETCH dp.ticker tk
                WHERE LOWER(tk.tickerSymbol) = LOWER(:tickerName)
                ORDER BY dp.priceDate DESC
            """)
    List<DailyPrice> findLatestByTickerName(String tickerName, Pageable pageable);

    Optional<DailyPrice> findTopByTickerOrderByPriceDateAsc(Ticker ticker);

    @Query("""
                SELECT tk.tickerId AS tickerId,
                       COALESCE(MAX(dp.priceDate), :defaultDate) AS latestPriceDate
                FROM Ticker tk
                LEFT JOIN DailyPrice dp ON dp.ticker = tk
                GROUP BY tk.tickerId
            """)
    List<TickerLatestPriceDateView> findLatestPriceDatesForAllTickers(LocalDate defaultDate);

    @Query("SELECT dp.priceDate FROM DailyPrice dp WHERE dp.ticker = :ticker AND dp.priceDate >= :startDate")
    List<LocalDate> findDatesByTickerAndDateGreaterThanEqual(Ticker ticker, LocalDate startDate);

    @Query("SELECT dp.priceDate FROM DailyPrice dp WHERE dp.ticker = :ticker AND dp.priceDate IN :dates")
    List<LocalDate> findDatesByTickerAndPriceDateIn(Ticker ticker, List<LocalDate> dates);

    List<DailyPrice> findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(Ticker ticker, LocalDate startDate);

    List<DailyPrice> findByTickerOrderByPriceDateAsc(Ticker ticker);

    @Query("""
                SELECT dp.priceDate FROM DailyPrice dp
                WHERE LOWER(dp.ticker.tickerSymbol) = LOWER(:symbol)
                ORDER BY dp.priceDate DESC
            """)
    List<LocalDate> findRecentPriceDates(String symbol, Pageable pageable);

    interface TickerLatestPriceDateView {
        Long getTickerId();

        LocalDate getLatestPriceDate();
    }

}
