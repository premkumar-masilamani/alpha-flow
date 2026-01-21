package com.alphaflow.infrastructure.persistence.repositories;

import com.alphaflow.infrastructure.persistence.entities.MarketState;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MarketStateRepository extends JpaRepository<MarketState, Long> {

    Optional<MarketState> findTopByTickerAndMetricAndMaTypeAndPeriodOrderByMarketStateDateDesc(
            Ticker ticker,
            String metric,
            String maType,
            int period
    );

    Optional<MarketState> findByTickerAndMarketStateDateAndMetricAndMaTypeAndPeriod(
            Ticker ticker,
            LocalDate marketStateDate,
            String metric,
            String maType,
            int period
    );
}
