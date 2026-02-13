package com.alphaflow.infrastructure.repositories;

import com.alphaflow.infrastructure.entities.CandleData;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandleBarRepository extends JpaRepository<CandleData, Long> {

    @Query("""
                SELECT md FROM CandleData md
                JOIN FETCH md.ticker tk
                WHERE LOWER(tk.tickerSymbol) = LOWER(:tickerName)
                ORDER BY md.candleBarDate ASC
            """)
    List<CandleData> findAllByTickerNameWithTicker(String tickerName);

    Optional<CandleData> findByTickerAndCandleBarDate(Ticker ticker, LocalDate candleBarDate);

    List<CandleData> findByTickerOrderByCandleBarDateAsc(Ticker ticker);

    Optional<CandleData> findTopByTickerOrderByCandleBarDateDesc(Ticker ticker);

}
