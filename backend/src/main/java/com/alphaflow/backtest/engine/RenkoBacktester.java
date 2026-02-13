package com.alphaflow.backtest.engine;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.repositories.*;
import com.alphaflow.backtest.strategies.Strategy;
import com.alphaflow.backtest.strategies.renko.RenkoPPStrategy;
import com.alphaflow.backtest.strategies.renko.RenkoStrategy;
import com.alphaflow.backtest.strategies.renko.RenkoTSMStrategy;
import com.alphaflow.backtest.strategies.renko.RenkoTSMV2Strategy;
import com.alphaflow.infrastructure.entities.CandleBar;
import com.alphaflow.infrastructure.entities.RenkoBrick;
import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.enums.RenkoPriceSource;
import com.alphaflow.infrastructure.generators.RenkoBricksGenerator;
import com.alphaflow.infrastructure.repositories.CandleBarRepository;
import com.alphaflow.infrastructure.repositories.IndicatorRepository;
import com.alphaflow.infrastructure.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class RenkoBacktester extends AbstractBacktester {

    private static final Logger log = LoggerFactory.getLogger(RenkoBacktester.class);

    public RenkoBacktester(
            TickerRepository tickerRepository,
            CandleBarRepository candleBarRepository,
            IndicatorRepository indicatorRepository,
            BacktestEquitiesRepository backtestEquitiesRepository,
            BacktestSignalsRepository backtestSignalsRepository,
            BacktestTradesRepository backtestTradesRepository,
            BacktestResultRepository backtestResultRepository,
            BacktestStrategyRepository backtestStrategyRepository,
            TransactionTemplate transactionTemplate
    ) {
        super(
                tickerRepository,
                candleBarRepository,
                indicatorRepository,
                backtestEquitiesRepository,
                backtestSignalsRepository,
                backtestTradesRepository,
                backtestResultRepository,
                backtestStrategyRepository,
                new ArrayList<>(),
                transactionTemplate
        );
        this.strategies = loadStrategiesFromDb();
    }

    private List<RenkoStrategy> loadStrategiesFromDb() {
        List<BacktestStrategy> entities = backtestStrategyRepository.findAll().stream()
                .filter(s -> s.getStrategyType().startsWith("RENKO"))
                .toList();

        log.info("Loaded {} RenkoBrick strategies from database", entities.size());

        return entities.stream()
                .map(this::instantiateStrategy)
                .toList();
    }

    private RenkoStrategy instantiateStrategy(BacktestStrategy entity) {
        return switch (entity.getStrategyType()) {
            case "RENKO_TSM" -> new RenkoTSMStrategy(entity);
            case "RENKO_PP" -> new RenkoPPStrategy(entity);
            case "RENKO_TSM_V2" -> new RenkoTSMV2Strategy(entity);
            default -> throw new IllegalArgumentException("Unknown RenkoBrick strategy type: " + entity.getStrategyType());
        };
    }

    @Override
    protected List<RenkoBrick> buildRenkoBricks(Ticker ticker, List<CandleBar> allData, int index, Strategy strategy) {
        RenkoPriceSource priceSource = null;
        // Strategies can have different price sources for their renko bricks
        if (strategy instanceof RenkoStrategy renkoStrategy) {
            priceSource = renkoStrategy.getPriceSource();
        }
        if (priceSource == null) {
            throw new IllegalStateException("RenkoPriceSource is not defined for strategy: " + strategy.getClass().getSimpleName());
        }

        return RenkoBricksGenerator.generateRenkoBricks(ticker, allData.subList(0, index + 1), priceSource);
    }
}
