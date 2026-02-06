package com.alphaflow.backtest.engine;

import com.alphaflow.backtest.configs.BacktestConfig;
import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.entities.BacktestStrategyIndicator;
import com.alphaflow.backtest.repositories.*;
import com.alphaflow.backtest.strategies.Strategy;
import com.alphaflow.backtest.strategies.renko.*;
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
                new ArrayList<>(),
                transactionTemplate
        );
        this.strategies = loadStrategiesFromDb(strategies, backtestConfig);
    }

    private List<RenkoStrategy> loadStrategiesFromDb(List<RenkoStrategy> baseStrategies, BacktestConfig config) {
        syncStrategiesToDb(baseStrategies, config);

        List<BacktestStrategy> entities = backtestStrategyRepository.findAll().stream()
                .filter(s -> s.getStrategyType().startsWith("RENKO"))
                .toList();

        log.info("Loaded {} Renko strategies from database", entities.size());

        return entities.stream()
                .map(this::instantiateStrategy)
                .toList();
    }

    private void syncStrategiesToDb(List<RenkoStrategy> baseStrategies, BacktestConfig config) {
        log.info("Syncing strategies to database...");
        for (RenkoStrategy s : baseStrategies) {
            if (s instanceof RenkoTSMStrategy) {
                if (config.isGridSearchEnabled()) {
                    gridSearchRenkoTSMStrategyEntities().forEach(this::ensureStrategyInDb);
                } else {
                    ensureStrategyInDb(toEntity((RenkoTSMStrategy) s));
                }
            } else if (s instanceof RenkoPPStrategy pp) {
                ensureStrategyInDb(toEntity(pp));
            } else if (s instanceof RenkoTSMV2Strategy v2) {
                ensureStrategyInDb(toEntity(v2));
            }
        }
    }

    private void ensureStrategyInDb(BacktestStrategy entity) {
        if (backtestStrategyRepository.findByName(entity.getName()).isEmpty()) {
            log.debug("Persisting strategy to DB: {}", entity.getName());
            backtestStrategyRepository.save(entity);
        }
    }

    private List<BacktestStrategy> gridSearchRenkoTSMStrategyEntities() {
        List<BacktestStrategy> combinations = new ArrayList<>();
        for (RenkoPriceSource priceSource : RenkoPriceSource.values()) {
            for (MarketDataMetricType momentumMetric : List.of(MarketDataMetricType.OBV, MarketDataMetricType.CCF)) {
                for (TransformationType maType : List.of(TransformationType.SMA, TransformationType.EMA)) {
                    for (int p = 3; p <= 21; p++) {
                        combinations.add(toEntity(new RenkoTSMStrategy(priceSource, maType, WindowPeriod.fromDays(p), momentumMetric)));
                    }
                }
            }
        }
        return combinations;
    }

    private BacktestStrategy toEntity(RenkoTSMStrategy s) {
        BacktestStrategy strategy = BacktestStrategy.builder()
                .name(s.getName())
                .strategyType("RENKO_TSM")
                .priceSource(s.getPriceSource().name())
                .build();

        strategy.getIndicators().add(BacktestStrategyIndicator.builder()
                .backtestStrategy(strategy)
                .indicatorRole("MA")
                .metric(s.getPriceSource().code())
                .transformation(s.getMaType().name())
                .period(s.getMaPeriod().days())
                .build());

        strategy.getIndicators().add(BacktestStrategyIndicator.builder()
                .backtestStrategy(strategy)
                .indicatorRole("MOMENTUM")
                .metric(s.getMomentumMetric().name())
                .transformation(s.getMomentumMetric().name())
                .period(0)
                .build());

        return strategy;
    }

    private BacktestStrategy toEntity(RenkoPPStrategy s) {
        return BacktestStrategy.builder()
                .name(s.getName())
                .strategyType("RENKO_PP")
                .priceSource(RenkoPriceSource.PRICE_CLOSE.name())
                .build();
    }

    private BacktestStrategy toEntity(RenkoTSMV2Strategy s) {
        return BacktestStrategy.builder()
                .name(s.getName())
                .strategyType("RENKO_TSM_V2")
                .priceSource(RenkoPriceSource.PRICE_CLOSE.name())
                .build();
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

