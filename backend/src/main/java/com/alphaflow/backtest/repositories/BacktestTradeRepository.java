package com.alphaflow.backtest.repositories;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.entities.BacktestTrade;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestTradeRepository extends JpaRepository<BacktestTrade, Long> {
    void deleteByTickerAndStrategy(Ticker ticker, BacktestStrategy strategy);
}
