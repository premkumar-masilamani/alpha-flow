package com.alphaflow.backtest.engine;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.repositories.*;
import com.alphaflow.backtest.strategies.candlestick.*;
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
public class CandlestickBacktester extends AbstractBacktester {

    private static final Logger log = LoggerFactory.getLogger(CandlestickBacktester.class);

    public CandlestickBacktester(
            TickerRepository tickerRepository,
            MarketDataRepository marketDataRepository,
            MarketStateRepository marketStateRepository,
            BacktestEquityRepository backtestEquityRepository,
            BacktestSignalRepository backtestSignalRepository,
            BacktestTradeRepository backtestTradeRepository,
            BacktestResultRepository backtestResultRepository,
            BacktestStrategyRepository backtestStrategyRepository,
            List<CandlestickStrategy> strategies,
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
        this.strategies = loadStrategiesFromDb(strategies);
    }

    private List<CandlestickStrategy> loadStrategiesFromDb(List<CandlestickStrategy> baseStrategies) {
        syncStrategiesToDb(baseStrategies);

        List<BacktestStrategy> entities = backtestStrategyRepository.findAll().stream()
                .filter(s -> !s.getStrategyType().startsWith("RENKO"))
                .toList();

        log.info("Loaded {} Candlestick strategies from database", entities.size());

        return entities.stream()
                .map(this::instantiateStrategy)
                .toList();
    }

    private void syncStrategiesToDb(List<CandlestickStrategy> baseStrategies) {
        log.info("Syncing Candlestick strategies to database...");
        for (CandlestickStrategy s : baseStrategies) {
            ensureStrategyInDb(toEntity(s));
        }
    }

    private void ensureStrategyInDb(BacktestStrategy entity) {
        if (backtestStrategyRepository.findByName(entity.getName()).isEmpty()) {
            log.debug("Persisting strategy to DB: {}", entity.getName());
            backtestStrategyRepository.save(entity);
        }
    }

    private BacktestStrategy toEntity(CandlestickStrategy s) {
        String type = s instanceof BuyAndHoldRiskOverlayStrategy ? "BUY_AND_HOLD_RISK_OVERLAY" : "BUY_AND_HOLD";
        BacktestStrategy strategy = BacktestStrategy.builder()
                .name(s.getName())
                .strategyType(type)
                .build();

        if (s instanceof BuyAndHoldRiskOverlayStrategy) {
            strategy.getIndicators().add(com.alphaflow.backtest.entities.BacktestStrategyIndicator.builder()
                    .backtestStrategy(strategy)
                    .indicatorRole("FILTER")
                    .metric("P_CLOSE")
                    .transformation("SMA")
                    .period(200)
                    .build());
        }

        return strategy;
    }

    private CandlestickStrategy instantiateStrategy(BacktestStrategy entity) {
        return switch (entity.getStrategyType()) {
            case "BUY_AND_HOLD" -> new BuyAndHoldStrategy(entity);
            case "BUY_AND_HOLD_RISK_OVERLAY" -> new BuyAndHoldRiskOverlayStrategy(entity);
            default -> throw new IllegalArgumentException("Unknown Candlestick strategy type: " + entity.getStrategyType());
        };
    }
}
