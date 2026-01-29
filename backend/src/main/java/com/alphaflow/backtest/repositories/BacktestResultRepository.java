package com.alphaflow.backtest.repositories;

import com.alphaflow.backtest.entities.BacktestResult;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestResultRepository extends JpaRepository<BacktestResult, Long> {
    void deleteByTickerAndStrategyName(Ticker ticker, String strategyName);
}
