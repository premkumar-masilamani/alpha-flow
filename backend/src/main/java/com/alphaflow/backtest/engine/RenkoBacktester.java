package com.alphaflow.backtest.engine;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.repositories.*;
import com.alphaflow.backtest.strategies.Strategy;
import com.alphaflow.backtest.strategies.renko.*;
import com.alphaflow.infrastructure.entities.MarketData;
import com.alphaflow.infrastructure.entities.RenkoData;
import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.enums.RenkoPriceSource;
import com.alphaflow.infrastructure.generators.RenkoBricksGenerator;
import com.alphaflow.infrastructure.repositories.MarketDataRepository;
import com.alphaflow.infrastructure.repositories.MarketStateRepository;
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
            MarketDataRepository marketDataRepository,
            MarketStateRepository marketStateRepository,
            BacktestEquityRepository backtestEquityRepository,
            BacktestSignalRepository backtestSignalRepository,
            BacktestTradeRepository backtestTradeRepository,
            BacktestResultRepository backtestResultRepository,
            BacktestStrategyRepository backtestStrategyRepository,
            TransactionTemplate transactionTemplate
    ) {
        super(
                tickerRepository,
                marketDataRepository,
                marketStateRepository,
                backtestEquityRepository,
                backtestSignalRepository,
                backtestTradeRepository,
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

        log.info("Loaded {} Renko strategies from database", entities.size());

        return entities.stream()
                .map(this::instantiateStrategy)
                .toList();
    }

    private RenkoStrategy instantiateStrategy(BacktestStrategy entity) {
        return switch (entity.getStrategyType()) {
            case "RENKO_TSM" -> new RenkoTSMStrategy(entity);
            case "RENKO_PP" -> new RenkoPPStrategy(entity);
            case "RENKO_TSM_V2" -> new RenkoTSMV2Strategy(entity);
            default -> throw new IllegalArgumentException("Unknown Renko strategy type: " + entity.getStrategyType());
        };
    }

    @Override
    protected List<RenkoData> buildRenkoBricks(Ticker ticker, List<MarketData> allData, int index, Strategy strategy) {
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
