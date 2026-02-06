package com.alphaflow.backtest.repositories;

import com.alphaflow.backtest.entities.BacktestEquity;
import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestEquityRepository extends JpaRepository<BacktestEquity, Long> {
    void deleteByTickerAndStrategyName(Ticker ticker, String strategyName);

    void deleteByTickerAndStrategy(Ticker ticker, BacktestStrategy strategy);
}
