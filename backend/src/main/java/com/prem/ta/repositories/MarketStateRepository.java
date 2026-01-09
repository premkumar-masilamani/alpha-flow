package com.prem.ta.repositories;

import com.prem.ta.entities.MarketState;
import com.prem.ta.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface MarketStateRepository
        extends JpaRepository<MarketState, Long> {

    Optional<MarketState>
    findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
            Ticker ticker,
            String metric,
            String maType,
            int period
    );

    Optional<MarketState>
    findByTickerAndMarketStateDateAndMetricAndMaTypeAndPeriod(
            Ticker ticker,
            LocalDate marketStateDate,
            String metric,
            String maType,
            int period
    );
}
