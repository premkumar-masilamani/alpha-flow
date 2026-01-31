package com.alphaflow.backtest.services;

import com.alphaflow.backtest.repositories.BacktestResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerformanceScorer {

    private final BacktestResultRepository repository;

    @Transactional
    public void score() {
        repository.updateFixedScoresAndFilters();
        repository.updateExpectancyScores();
        repository.updateFinalScores();
    }
}