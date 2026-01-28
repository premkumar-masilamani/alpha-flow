package com.alphaflow.infrastructure.persistence.repositories;

import com.alphaflow.infrastructure.persistence.entities.BacktestCagr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestCagrRepository extends JpaRepository<BacktestCagr, Long> {
    @Modifying
    @Query("DELETE FROM BacktestCagr b WHERE b.ticker.tickerId = :tickerId AND b.strategyName = :strategyName")
    void deleteByTickerIdAndStrategyName(Long tickerId, String strategyName);
}
