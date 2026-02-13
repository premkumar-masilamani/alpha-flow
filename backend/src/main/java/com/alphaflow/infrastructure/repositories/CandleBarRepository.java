package com.alphaflow.infrastructure.repositories;

import com.alphaflow.infrastructure.entities.CandleBar;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandleBarRepository extends JpaRepository<CandleBar, Long> {

    @Query("""
                SELECT md FROM CandleBar md
                JOIN FETCH md.ticker tk
                WHERE LOWER(tk.tickerSymbol) = LOWER(:tickerName)
                ORDER BY md.candleBarDate ASC
            """)
    List<CandleBar> findAllByTickerNameWithTicker(String tickerName);

    Optional<CandleBar> findByTickerAndCandleBarDate(Ticker ticker, LocalDate candleBarDate);

    List<CandleBar> findByTickerOrderByCandleBarDateAsc(Ticker ticker);

    Optional<CandleBar> findTopByTickerOrderByCandleBarDateDesc(Ticker ticker);

}
