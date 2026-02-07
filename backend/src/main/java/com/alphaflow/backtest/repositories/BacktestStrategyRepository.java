package com.alphaflow.backtest.repositories;

import com.alphaflow.backtest.entities.BacktestStrategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BacktestStrategyRepository extends JpaRepository<BacktestStrategy, Long> {
    Optional<BacktestStrategy> findByName(String name);
}
