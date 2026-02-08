package com.alphaflow.infrastructure.repositories;

import com.alphaflow.infrastructure.entities.Candle;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandleRepository extends JpaRepository<Candle, Long> {

    @Query("""
                SELECT md FROM Candle md
                JOIN FETCH md.ticker tk
                WHERE LOWER(tk.tickerSymbol) = LOWER(:tickerName)
                ORDER BY md.candleDate ASC
            """)
    List<Candle> findAllByTickerNameWithTicker(String tickerName);

    Optional<Candle> findByTickerAndCandleDate(Ticker ticker, LocalDate candleDate);

    List<Candle> findByTickerOrderByCandleDateAsc(Ticker ticker);

    Optional<Candle> findTopByTickerOrderByCandleDateDesc(Ticker ticker);

}
