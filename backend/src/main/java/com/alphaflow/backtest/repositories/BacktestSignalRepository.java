package com.alphaflow.backtest.repositories;

import com.alphaflow.backtest.entities.BacktestSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestSignalRepository extends JpaRepository<BacktestSignal, Long> {

}
