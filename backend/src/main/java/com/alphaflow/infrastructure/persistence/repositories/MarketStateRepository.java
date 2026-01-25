package com.alphaflow.infrastructure.persistence.repositories;

import com.alphaflow.infrastructure.persistence.entities.MarketState;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

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

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO market_state (market_state_date, ticker_id, metric, ma_type, period, value, capital_momentum)
            SELECT t10.market_state_date, t10.ticker_id, 'CAP_MOM', 'DIFF', 0, t10.value - t20.value, t10.value - t20.value
            FROM market_state t10
            JOIN market_state t20 ON t10.ticker_id = t20.ticker_id AND t10.market_state_date = t20.market_state_date
            WHERE t10.metric = 'T_CAP' AND t10.ma_type = 'EMA' AND t10.period = 10
            AND t20.metric = 'T_CAP' AND t20.ma_type = 'EMA' AND t20.period = 20
            AND NOT EXISTS (
                SELECT 1 FROM market_state m3
                WHERE m3.ticker_id = t10.ticker_id
                AND m3.market_state_date = t10.market_state_date
                AND m3.metric = 'CAP_MOM'
            )
            """, nativeQuery = true)
    int computeCapitalMomentumBulk();
}
