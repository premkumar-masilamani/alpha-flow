package com.alphaflow.backtest.repositories;

import com.alphaflow.backtest.entities.BacktestSignals;
import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BacktestSignalsRepository extends JpaRepository<BacktestSignals, Long> {
    void deleteByTickerAndStrategy(Ticker ticker, BacktestStrategy strategy);

    List<BacktestSignals> findByTickerAndActionIn(Ticker ticker, List<TradeSignal> actions);
}
