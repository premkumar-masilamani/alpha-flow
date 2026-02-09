package com.alphaflow.backtest.engine;

import com.alphaflow.backtest.configs.BacktestConfig;
import com.alphaflow.backtest.repositories.*;
import com.alphaflow.backtest.strategies.Strategy;
import com.alphaflow.backtest.strategies.renko.RenkoStrategy;
import com.alphaflow.backtest.strategies.renko.RenkoTSMStrategy;
import com.alphaflow.engine.enums.MarketDataMetricType;
import com.alphaflow.engine.enums.TransformationType;
import com.alphaflow.engine.enums.WindowPeriod;
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
            List<RenkoStrategy> strategies,
            TransactionTemplate transactionTemplate,
            BacktestConfig backtestConfig
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
                expandStrategies(strategies, backtestConfig),
                transactionTemplate
        );
    }

    private static List<RenkoStrategy> expandStrategies(List<RenkoStrategy> renkoStrategies, BacktestConfig config) {
        if (!config.isGridSearchEnabled()) {
            return renkoStrategies;
        }

        log.info("Expanding Renko renkoStrategies. Base count: {}", renkoStrategies.size());
        List<RenkoStrategy> all = new ArrayList<>();

        for (RenkoStrategy renkoStrategy : renkoStrategies) {
            if (renkoStrategy instanceof RenkoTSMStrategy) {
                all.addAll(gridSearchRenkoTSMStrategies());
            } else {
                all.add(renkoStrategy);
            }
        }

        log.info("Total Renko renkoStrategies after expansion: {}", all.size());
        return all;
    }

    private static List<RenkoStrategy> gridSearchRenkoTSMStrategies() {
        List<RenkoStrategy> combinations = new ArrayList<>();
        for (RenkoPriceSource priceSource : RenkoPriceSource.values()) {
            for (MarketDataMetricType momentumMetric : List.of(MarketDataMetricType.OBV, MarketDataMetricType.CCF)) {
                for (TransformationType maType : List.of(TransformationType.SMA, TransformationType.EMA)) {
                    for (int p = 3; p <= 21; p++) {
                        combinations.add(new RenkoTSMStrategy(priceSource, maType, WindowPeriod.fromDays(p), momentumMetric));
                    }
                }
            }
        }
        return combinations;
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
