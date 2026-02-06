package com.alphaflow.backtest.repositories;

import com.alphaflow.backtest.entities.BacktestSignal;
import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BacktestSignalRepository extends JpaRepository<BacktestSignal, Long> {
    void deleteByTickerAndStrategyName(Ticker ticker, String strategyName);

    void deleteByTickerAndStrategy(Ticker ticker, BacktestStrategy strategy);

    List<BacktestSignal> findByTickerAndActionIn(Ticker ticker, List<TradeSignal> actions);
}
