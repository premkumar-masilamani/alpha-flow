package com.alphaflow.backtest.engine;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.enums.StrategyCategory;
import com.alphaflow.backtest.enums.StrategyType;
import com.alphaflow.backtest.repositories.*;
import com.alphaflow.backtest.strategies.candlestick.CandlestickStrategy;
import com.alphaflow.backtest.strategies.StrategyState;
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

    private List<CandlestickStrategy<? extends StrategyState>> loadStrategiesFromDb() {
        List<BacktestStrategy> entities = backtestStrategyRepository.findAll().stream()
                .filter(entity -> StrategyType.fromDb(entity.getStrategyType()).getCategory() == StrategyCategory.CANDLESTICK)
                .toList();

        log.info("Loaded {} Candlestick strategies from database", entities.size());

        return entities.stream()
                .map(entity -> StrategyType.fromDb(entity.getStrategyType()).create(entity))
                .map(strategy -> (CandlestickStrategy<? extends StrategyState>) strategy)
                .toList();
    }
}
