package com.alphaflow.backtest.repositories;

import com.alphaflow.backtest.entities.BacktestEquity;
import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BacktestEquityRepository extends JpaRepository<BacktestEquity, Long> {
    void deleteByTickerAndStrategy(Ticker ticker, BacktestStrategy strategy);

    Optional<BacktestEquity> findTopByTickerAndStrategyOrderByEquityDateDesc(Ticker ticker, BacktestStrategy strategy);

    List<BacktestEquity> findByTickerAndStrategyOrderByEquityDateAsc(Ticker ticker, BacktestStrategy strategy);
}
