package com.alphaflow.infrastructure.repositories;

import com.alphaflow.infrastructure.entities.DailyCandleData;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyCandleDataRepository extends JpaRepository<DailyCandleData, Long> {

    interface TickerLatestCandleDateView {
        Long getTickerId();

        LocalDate getLatestCandleDate();
    }

    @Query("""
                SELECT md FROM DailyCandleData md
                JOIN FETCH md.ticker tk
                WHERE LOWER(tk.tickerSymbol) = LOWER(:tickerName)
                ORDER BY md.candleDataDate DESC
            """)
    List<DailyCandleData> findLatestByTickerName(String tickerName, Pageable pageable);

    Optional<DailyCandleData> findByTickerAndCandleDataDate(Ticker ticker, LocalDate candleDataDate);

    List<DailyCandleData> findByTickerOrderByCandleDataDateAsc(Ticker ticker);

    Optional<DailyCandleData> findTopByTickerOrderByCandleDataDateDesc(Ticker ticker);

    @Query("""
                SELECT tk.tickerId AS tickerId,
                       COALESCE(MAX(md.candleDataDate), :defaultDate) AS latestCandleDate
                FROM Ticker tk
                LEFT JOIN DailyCandleData md ON md.ticker = tk
                GROUP BY tk.tickerId
            """)
    List<TickerLatestCandleDateView> findLatestCandleDatesForAllTickers(LocalDate defaultDate);

    @Query("SELECT md.candleDataDate FROM DailyCandleData md WHERE md.ticker = :ticker AND md.candleDataDate >= :startDate")
    List<LocalDate> findDatesByTickerAndDateGreaterThanEqual(Ticker ticker, LocalDate startDate);

    List<DailyCandleData> findByTickerAndCandleDataDateGreaterThanEqualOrderByCandleDataDateAsc(Ticker ticker, LocalDate startDate);

}
