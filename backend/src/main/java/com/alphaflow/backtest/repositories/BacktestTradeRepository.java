package com.alphaflow.backtest.repositories;

import com.alphaflow.backtest.entities.BacktestTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BacktestTradeRepository extends JpaRepository<BacktestTrade, UUID> {

}
