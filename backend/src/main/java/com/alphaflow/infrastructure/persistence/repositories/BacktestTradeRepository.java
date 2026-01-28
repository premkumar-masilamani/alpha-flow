package com.alphaflow.infrastructure.persistence.repositories;

import com.alphaflow.infrastructure.persistence.entities.BacktestTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BacktestTradeRepository extends JpaRepository<BacktestTrade, UUID> {
    @Modifying
    @Query("DELETE FROM BacktestTrade b WHERE b.ticker.tickerId = :tickerId AND b.strategyName = :strategyName")
    void deleteByTickerIdAndStrategyName(Long tickerId, String strategyName);
}
