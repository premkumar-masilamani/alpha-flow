package com.alphaflow.backtest.services;

import com.alphaflow.backtest.repositories.BacktestResultRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerformanceScorer {

    private static final Logger log = LoggerFactory.getLogger(PerformanceScorer.class);

    private final BacktestResultRepository repository;

    @Transactional
    public void score() {
        log.info("Starting performance scoring of strategies...");
        repository.updateFixedScoresAndFilters();
        repository.updateExpectancyScores();
        repository.updateFinalScores();
        log.info("Performance scoring completed.");
    }
}