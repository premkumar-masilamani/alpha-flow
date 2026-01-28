package com.alphaflow.infrastructure.persistence.repositories;

import com.alphaflow.infrastructure.persistence.entities.BacktestSignalIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestSignalIntentRepository extends JpaRepository<BacktestSignalIntent, Long> {
    @Modifying
    @Query("DELETE FROM BacktestSignalIntent b WHERE b.ticker.tickerId = :tickerId AND b.strategyName = :strategyName")
    void deleteByTickerIdAndStrategyName(Long tickerId, String strategyName);
}
