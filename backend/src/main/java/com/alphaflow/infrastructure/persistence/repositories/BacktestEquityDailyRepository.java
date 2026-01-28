package com.alphaflow.infrastructure.persistence.repositories;

import com.alphaflow.infrastructure.persistence.entities.BacktestEquityDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestEquityDailyRepository extends JpaRepository<BacktestEquityDaily, Long> {
    @Modifying
    @Query("DELETE FROM BacktestEquityDaily b WHERE b.ticker.tickerId = :tickerId AND b.strategyName = :strategyName")
    void deleteByTickerIdAndStrategyName(Long tickerId, String strategyName);
}
