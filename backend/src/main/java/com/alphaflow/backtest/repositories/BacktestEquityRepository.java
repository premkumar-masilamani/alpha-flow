package com.alphaflow.backtest.repositories;

import com.alphaflow.backtest.entities.BacktestEquity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestEquityRepository extends JpaRepository<BacktestEquity, Long> {

}
