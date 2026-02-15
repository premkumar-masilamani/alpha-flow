package com.alphaflow.backtest.repositories;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.entities.BacktestTrade;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BacktestTradeRepository extends JpaRepository<BacktestTrade, Long> {
    void deleteByTickerAndStrategy(Ticker ticker, BacktestStrategy strategy);

    Optional<BacktestTrade> findTopByTickerAndStrategyOrderByEntryDateDesc(Ticker ticker, BacktestStrategy strategy);

    List<BacktestTrade> findByTickerAndStrategyOrderByEntryDateAsc(Ticker ticker, BacktestStrategy strategy);
}
