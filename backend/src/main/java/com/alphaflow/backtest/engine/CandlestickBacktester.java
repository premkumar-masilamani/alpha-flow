package com.alphaflow.backtest.engine;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.repositories.*;
import com.alphaflow.backtest.strategies.candlestick.BuyAndHoldRiskOverlayStrategy;
import com.alphaflow.backtest.strategies.candlestick.BuyAndHoldStrategy;
import com.alphaflow.backtest.strategies.candlestick.CandlestickStrategy;
import com.alphaflow.infrastructure.repositories.CandleRepository;
import com.alphaflow.infrastructure.repositories.IndicatorRepository;
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
            CandleRepository candleRepository,
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
                candleRepository,
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

    private List<CandlestickStrategy> loadStrategiesFromDb() {
        List<BacktestStrategy> entities = backtestStrategyRepository.findAll().stream()
                .filter(s -> !s.getStrategyType().startsWith("RENKO"))
                .toList();

        log.info("Loaded {} Candlestick strategies from database", entities.size());

        return entities.stream()
                .map(this::instantiateStrategy)
                .toList();
    }

    private CandlestickStrategy instantiateStrategy(BacktestStrategy entity) {
        return switch (entity.getStrategyType()) {
            case "BUY_AND_HOLD" -> new BuyAndHoldStrategy(entity);
            case "BUY_AND_HOLD_RISK_OVERLAY" -> new BuyAndHoldRiskOverlayStrategy(entity);
            default -> throw new IllegalArgumentException("Unknown Candlestick strategy type: " + entity.getStrategyType());
        };
    }
}
