package com.alphaflow.backtest.engine;

import com.alphaflow.backtest.entities.BacktestResult;
import com.alphaflow.backtest.repositories.BacktestResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PerformanceScoringServiceTest {

    private PerformanceScoringService service;
    private BacktestResultRepository repository;

    @BeforeEach
    void setUp() {
        repository = mock(BacktestResultRepository.class);
        service = new PerformanceScoringService(repository);
    }

    @Test
    void testCalculateScores() {
        BacktestResult result = BacktestResult.builder()
                .cagr(BigDecimal.valueOf(25)) // Score 80
                .maxDrawdownPct(BigDecimal.valueOf(15)) // Score 85
                .sharpeRatio(BigDecimal.valueOf(1.8)) // Score 85
                .profitFactor(BigDecimal.valueOf(2.5)) // Score 85
                .build();

        service.calculateScores(result);

        assertEquals(80, result.getCagrScore());
        assertEquals(85, result.getMddScore());
        assertEquals(85, result.getSharpeScore());
        assertEquals(85, result.getProfitFactorScore());
        assertEquals(70, result.getExpectancyScore());
        assertNotNull(result.getFinalScore());
    }

    @Test
    void testApplyFilters_Pass() {
        BacktestResult result = BacktestResult.builder()
                .maxDrawdownPct(BigDecimal.valueOf(39))
                .sharpeRatio(BigDecimal.valueOf(0.9))
                .profitFactor(BigDecimal.valueOf(1.4))
                .totalTrades(31)
                .expectancy(BigDecimal.valueOf(0.1))
                .build();

        service.applyFilters(result);

        assertTrue(result.getPassesFilters());
    }

    @Test
    void testApplyFilters_Fail() {
        BacktestResult result = BacktestResult.builder()
                .maxDrawdownPct(BigDecimal.valueOf(41)) // Fail
                .sharpeRatio(BigDecimal.valueOf(0.9))
                .profitFactor(BigDecimal.valueOf(1.4))
                .totalTrades(31)
                .expectancy(BigDecimal.valueOf(0.1))
                .build();

        service.applyFilters(result);

        assertFalse(result.getPassesFilters());
    }

    @Test
    void testCalculateFinalScore() {
        BacktestResult result = BacktestResult.builder()
                .cagrScore(100)        // 0.3 * 100 = 30
                .mddScore(100)         // 0.3 * 100 = 30
                .sharpeScore(100)      // 0.2 * 100 = 20
                .profitFactorScore(100) // 0.1 * 100 = 10
                .expectancyScore(100)  // 0.1 * 100 = 10
                .build();              // Total = 100

        BigDecimal score = service.calculateFinalScore(result);
        assertEquals(new BigDecimal("100.00"), score);
    }
}
