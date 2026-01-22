package com.alphaflow.infrastructure.persistence.repositories;

import com.alphaflow.infrastructure.persistence.entities.MarketData;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {

    @Query("""
                SELECT md FROM MarketData md
                JOIN FETCH md.ticker tk
                WHERE LOWER(tk.tickerSymbol) = LOWER(:tickerName)
                ORDER BY md.marketDataDate ASC
            """)
    List<MarketData> findAllByTickerNameWithTicker(String tickerName);

    Optional<MarketData> findByTickerAndMarketDataDate(Ticker ticker, LocalDate marketDataDate);

    List<MarketData> findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(Ticker ticker, LocalDate startDate);

}
