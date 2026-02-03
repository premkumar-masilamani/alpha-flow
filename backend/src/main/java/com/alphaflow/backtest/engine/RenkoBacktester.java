package com.alphaflow.backtest.engine;

import com.alphaflow.backtest.configs.BacktestConfig;
import com.alphaflow.backtest.repositories.BacktestEquityRepository;
import com.alphaflow.backtest.repositories.BacktestResultRepository;
import com.alphaflow.backtest.repositories.BacktestSignalRepository;
import com.alphaflow.backtest.repositories.BacktestTradeRepository;
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
                gridSearchStrategies(strategies, backtestConfig),
                transactionTemplate
        );
    }

    private static List<RenkoStrategy> gridSearchStrategies(List<RenkoStrategy> strategies, BacktestConfig config) {

        if (!config.isGridSearchEnabled()) {
            return strategies;
        }

        log.info("Grid Searching Renko strategies. Base count: {}", strategies.size());
        List<RenkoStrategy> all = new ArrayList<>();

        for (RenkoStrategy strategy : strategies) {
            if (!(strategy instanceof RenkoTSMStrategy)) {
                all.add(strategy);
            }
        }

        for (RenkoPriceSource priceSource : RenkoPriceSource.values()) {
            for (MarketDataMetricType momentumMetric : List.of(MarketDataMetricType.OBV, MarketDataMetricType.CCF)) {
                for (TransformationType maType : List.of(TransformationType.SMA, TransformationType.EMA)) {
                    for (int p = 3; p <= 21; p++) {
                        all.add(new RenkoTSMStrategy(priceSource, maType, WindowPeriod.fromDays(p), momentumMetric));
                    }
                }
            }
        }
        log.info("Total Renko strategies after expansion: {}", all.size());
        return all;
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

