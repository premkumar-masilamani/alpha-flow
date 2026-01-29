package com.alphaflow.infrastructure.repositories;

import com.alphaflow.infrastructure.entities.MarketState;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
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

    List<MarketState> findByTickerAndMetricAndMaTypeAndPeriodAndMarketStateDateGreaterThanEqualOrderByMarketStateDateAsc(
            Ticker ticker,
            String metric,
            String maType,
            int period,
            LocalDate startDate
    );

    List<MarketState> findByTickerOrderByMarketStateDateAsc(Ticker ticker);
}
