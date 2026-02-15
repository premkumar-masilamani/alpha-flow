package com.alphaflow.backtest.repositories;

import com.alphaflow.backtest.entities.BacktestSignal;
import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BacktestSignalRepository extends JpaRepository<BacktestSignal, Long> {
    void deleteByTickerAndStrategy(Ticker ticker, BacktestStrategy strategy);

    List<BacktestSignal> findByTickerAndActionIn(Ticker ticker, List<TradeSignal> actions);

    Optional<BacktestSignal> findTopByTickerAndStrategyOrderBySignalDateDesc(Ticker ticker, BacktestStrategy strategy);
}
