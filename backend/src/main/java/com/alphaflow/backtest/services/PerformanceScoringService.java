package com.alphaflow.backtest.services;

import com.alphaflow.backtest.entities.BacktestResult;
import com.alphaflow.backtest.repositories.BacktestResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceScoringService {

    private final BacktestResultRepository repository;

    public void calculateScores(BacktestResult result) {
        result.setCagrScore(calculateCagrScore(result.getCagr()));
        result.setMddScore(calculateMddScore(result.getMaxDrawdownPct()));
        result.setSharpeScore(calculateSharpeScore(result.getSharpeRatio()));
        result.setProfitFactorScore(calculateProfitFactorScore(result.getProfitFactor()));
        // Default expectancy score until batch update runs
        if (result.getExpectancyScore() == null) {
            result.setExpectancyScore(70);
        }
        result.setFinalScore(calculateFinalScore(result));
    }

    public void applyFilters(BacktestResult result) {
        boolean passes = true;
        if (result.getMaxDrawdownPct() != null && result.getMaxDrawdownPct().compareTo(BigDecimal.valueOf(40)) > 0) passes = false;
        if (result.getSharpeRatio() != null && result.getSharpeRatio().compareTo(BigDecimal.valueOf(0.8)) < 0) passes = false;
        if (result.getProfitFactor() != null && result.getProfitFactor().compareTo(BigDecimal.valueOf(1.3)) < 0) passes = false;
        if (result.getTotalTrades() != null && result.getTotalTrades() < 30) passes = false;
        if (result.getExpectancy() != null && result.getExpectancy().compareTo(BigDecimal.ZERO) <= 0) passes = false;
        result.setPassesFilters(passes);
    }

    @Transactional
    public void updateAllScores() {
        repository.updateFixedScoresAndFilters();
        repository.updateExpectancyScores();
        repository.updateFinalScores();
    }

    public BigDecimal calculateFinalScore(BacktestResult r) {
        if (r.getCagrScore() == null || r.getMddScore() == null || r.getSharpeScore() == null ||
                r.getProfitFactorScore() == null || r.getExpectancyScore() == null) {
            return BigDecimal.ZERO;
        }

        double finalScore = 0.30 * r.getCagrScore() +
                0.30 * r.getMddScore() +
                0.20 * r.getSharpeScore() +
                0.10 * r.getProfitFactorScore() +
                0.10 * r.getExpectancyScore();

        return BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP);
    }

    private int calculateCagrScore(BigDecimal cagr) {
        if (cagr == null) return 0;
        double val = cagr.doubleValue();
        if (val < 10) return 30;
        if (val < 20) return 60;
        if (val < 30) return 80;
        return 100;
    }

    private int calculateMddScore(BigDecimal mdd) {
        if (mdd == null) return 0;
        double val = mdd.doubleValue();
        if (val > 40) return 20;
        if (val > 30) return 40;
        if (val > 20) return 70;
        if (val > 10) return 85;
        return 100;
    }

    private int calculateSharpeScore(BigDecimal sharpe) {
        if (sharpe == null) return 0;
        double val = sharpe.doubleValue();
        if (val < 1) return 40;
        if (val < 1.5) return 70;
        if (val < 2) return 85;
        return 100;
    }

    private int calculateProfitFactorScore(BigDecimal pf) {
        if (pf == null) return 0;
        double val = pf.doubleValue();
        if (val < 1.3) return 30;
        if (val < 1.5) return 50;
        if (val < 2) return 70;
        if (val < 3) return 85;
        return 100;
    }
}
