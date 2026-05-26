package com.alphaflow.infrastructure.repositories;

import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.entities.WeeklyCandleData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyCandleDataRepository extends JpaRepository<WeeklyCandleData, Long> {

    Optional<WeeklyCandleData> findByTickerAndCandleDataDate(Ticker ticker, LocalDate candleDataDate);

    Optional<WeeklyCandleData> findTopByTickerOrderByCandleDataDateDesc(Ticker ticker);

    @Query("""
                SELECT wmd FROM WeeklyCandleData wmd
                JOIN FETCH wmd.ticker tk
                WHERE LOWER(tk.tickerSymbol) = LOWER(:tickerName)
                ORDER BY wmd.candleDataDate DESC
            """)
    List<WeeklyCandleData> findLatestByTickerName(String tickerName, Pageable pageable);

}
