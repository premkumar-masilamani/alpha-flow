package com.alphaflow.backtest.repositories;

import com.alphaflow.backtest.entities.BacktestResult;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestResultRepository extends JpaRepository<BacktestResult, Long> {
    void deleteByTickerAndStrategyName(Ticker ticker, String strategyName);

    @Modifying
    @Query(value = """
        WITH stats AS (
            SELECT backtest_result_id,
                   PERCENT_RANK() OVER (ORDER BY expectancy) as p
            FROM backtest_results
            WHERE expectancy IS NOT NULL
        )
        UPDATE backtest_results
        SET expectancy_score = CASE
            WHEN stats.p <= 0.25 THEN 40
            WHEN stats.p <= 0.75 THEN 70
            ELSE 100
        END
        FROM stats
        WHERE backtest_results.backtest_result_id = stats.backtest_result_id
    """, nativeQuery = true)
    void updateExpectancyScores();

    @Modifying
    @Query(value = """
        UPDATE backtest_results
        SET final_score = ROUND(0.30 * cagr_score + 0.30 * mdd_score + 0.20 * sharpe_score + 0.10 * profit_factor_score + 0.10 * expectancy_score, 2)
        WHERE cagr_score IS NOT NULL
          AND mdd_score IS NOT NULL
          AND sharpe_score IS NOT NULL
          AND profit_factor_score IS NOT NULL
          AND expectancy_score IS NOT NULL
    """, nativeQuery = true)
    void updateFinalScores();

    @Modifying
    @Query(value = """
        UPDATE backtest_results
        SET cagr_score = CASE
                WHEN cagr < 10 THEN 30
                WHEN cagr < 20 THEN 60
                WHEN cagr < 30 THEN 80
                ELSE 100
            END,
            mdd_score = CASE
                WHEN max_drawdown_pct > 40 THEN 20
                WHEN max_drawdown_pct > 30 THEN 40
                WHEN max_drawdown_pct > 20 THEN 70
                WHEN max_drawdown_pct > 10 THEN 85
                ELSE 100
            END,
            sharpe_score = CASE
                WHEN sharpe_ratio < 1 THEN 40
                WHEN sharpe_ratio < 1.5 THEN 70
                WHEN sharpe_ratio < 2 THEN 85
                ELSE 100
            END,
            profit_factor_score = CASE
                WHEN profit_factor < 1.3 THEN 30
                WHEN profit_factor < 1.5 THEN 50
                WHEN profit_factor < 2 THEN 70
                WHEN profit_factor < 3 THEN 85
                ELSE 100
            END,
            passes_filters = (
                max_drawdown_pct <= 40 AND
                sharpe_ratio >= 0.8 AND
                profit_factor >= 1.3 AND
                total_trades >= 30 AND
                expectancy > 0
            )
    """, nativeQuery = true)
    void updateFixedScoresAndFilters();
}
