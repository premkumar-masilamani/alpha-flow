package com.alphaflow.backtest.repositories;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.entities.BacktestTrades;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestTradesRepository extends JpaRepository<BacktestTrades, Long> {
    void deleteByTickerAndStrategy(Ticker ticker, BacktestStrategy strategy);
}
