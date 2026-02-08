package com.alphaflow.backtest.repositories;

import com.alphaflow.backtest.entities.BacktestEquities;
import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestEquitiesRepository extends JpaRepository<BacktestEquities, Long> {
    void deleteByTickerAndStrategy(Ticker ticker, BacktestStrategy strategy);
}
