package com.alphaflow.infrastructure.persistence.repositories;

import com.alphaflow.infrastructure.persistence.entities.BacktestResult;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface BacktestResultRepository extends JpaRepository<BacktestResult, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM BacktestResult br WHERE br.ticker.tickerId = :tickerId AND br.strategyName = :strategyName")
    void deleteByTickerIdAndStrategyName(Long tickerId, String strategyName);
}
