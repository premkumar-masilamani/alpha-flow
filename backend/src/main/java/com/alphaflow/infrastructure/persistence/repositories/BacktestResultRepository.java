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

    @Modifying
    @Transactional
    @Query("DELETE FROM BacktestResult br WHERE br.ticker = :ticker AND br.strategyName = :strategyName")
    void deleteByTickerAndStrategyName(Ticker ticker, String strategyName);
}
