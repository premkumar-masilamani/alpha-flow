package com.alphaflow.core;

import com.alphaflow.domain.enums.BacktestSignal;
import com.alphaflow.domain.enums.PositionType;
import com.alphaflow.domain.strategy.BacktestStrategy;
import com.alphaflow.infrastructure.persistence.entities.BacktestResult;
import com.alphaflow.infrastructure.persistence.entities.MarketData;
import com.alphaflow.infrastructure.persistence.entities.MarketState;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
import com.alphaflow.infrastructure.persistence.repositories.BacktestResultRepository;
import com.alphaflow.infrastructure.persistence.repositories.MarketDataRepository;
import com.alphaflow.infrastructure.persistence.repositories.MarketStateRepository;
import com.alphaflow.infrastructure.persistence.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BacktestComputer {

    private static final Logger log = LoggerFactory.getLogger(BacktestComputer.class);
    private static final BigDecimal INITIAL_EQUITY = new BigDecimal("100000.0000");

    private final TickerRepository tickerRepository;
    private final MarketDataRepository marketDataRepository;
    private final MarketStateRepository marketStateRepository;
    private final BacktestResultRepository backtestResultRepository;
    private final List<BacktestStrategy> strategies;

    public BacktestComputer(
            TickerRepository tickerRepository,
            MarketDataRepository marketDataRepository,
            MarketStateRepository marketStateRepository,
            BacktestResultRepository backtestResultRepository,
            List<BacktestStrategy> strategies
    ) {
        this.tickerRepository = tickerRepository;
        this.marketDataRepository = marketDataRepository;
        this.marketStateRepository = marketStateRepository;
        this.backtestResultRepository = backtestResultRepository;
        this.strategies = strategies;
    }

    @Transactional
    public void compute() {
        log.info("Starting Backtest Computation for all active tickers");

        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            log.info("Running backtests for {}", ticker.getTickerSymbol());

            List<MarketData> marketDataList = marketDataRepository.findByTickerAndMarketDataDateGreaterThanEqualOrderByMarketDataDateAsc(ticker, LocalDate.of(2000, 1, 1));
            List<MarketState> marketStateList = marketStateRepository.findByTickerOrderByMarketStateDateAsc(ticker);

            if (marketDataList.isEmpty()) {
                log.warn("No market data found for {}, skipping.", ticker.getTickerSymbol());
                return;
            }

            // Group market state by date and key (metric_maType_period)
            Map<LocalDate, Map<String, BigDecimal>> indicatorMap = marketStateList.stream()
                    .collect(Collectors.groupingBy(
                            MarketState::getMarketStateDate,
                            Collectors.toMap(
                                    ms -> ms.getMetric() + "_" + ms.getMaType() + "_" + ms.getPeriod(),
                                    MarketState::getValue,
                                    (existing, replacement) -> existing
                            )
                    ));

            for (BacktestStrategy strategy : strategies) {
                log.info("Running strategy: {} for {}", strategy.getName(), ticker.getTickerSymbol());
                runBacktest(ticker, strategy, marketDataList, indicatorMap);
            }
        });

        log.info("Backtest Computation completed.");
    }

    private void runBacktest(Ticker ticker, BacktestStrategy strategy, List<MarketData> marketDataList, Map<LocalDate, Map<String, BigDecimal>> indicatorMap) {
        BigDecimal currentCash = INITIAL_EQUITY;
        PositionType position = PositionType.NONE;
        BigDecimal shares = BigDecimal.ZERO;
        BigDecimal entryPrice = BigDecimal.ZERO;
        BacktestSignal pendingSignal = BacktestSignal.NONE;

        List<BacktestResult> results = new ArrayList<>();

        for (int i = 0; i < marketDataList.size(); i++) {
            MarketData currentDay = marketDataList.get(i);
            LocalDate date = currentDay.getMarketDataDate();
            BigDecimal priceOpen = currentDay.getPriceOpen();
            BigDecimal priceClose = currentDay.getPriceClose();

            // 1. Execute pending signal from previous day at today's open
            if (pendingSignal != BacktestSignal.NONE && pendingSignal != BacktestSignal.HOLD) {
                if (pendingSignal == BacktestSignal.LONG_ENTRY && position == PositionType.NONE) {
                    shares = currentCash.divide(priceOpen, 8, RoundingMode.HALF_UP);
                    entryPrice = priceOpen;
                    currentCash = BigDecimal.ZERO;
                    position = PositionType.LONG;
                } else if (pendingSignal == BacktestSignal.SHORT_ENTRY && position == PositionType.NONE) {
                    shares = currentCash.divide(priceOpen, 8, RoundingMode.HALF_UP);
                    entryPrice = priceOpen;
                    // For shorting, currentCash remains the same (it's the margin/collateral),
                    // and we track profit/loss relative to it.
                    position = PositionType.SHORT;
                } else if (pendingSignal == BacktestSignal.EXIT_LONG && position == PositionType.LONG) {
                    currentCash = shares.multiply(priceOpen);
                    shares = BigDecimal.ZERO;
                    position = PositionType.NONE;
                } else if (pendingSignal == BacktestSignal.EXIT_SHORT && position == PositionType.SHORT) {
                    currentCash = currentCash.add(shares.multiply(entryPrice.subtract(priceOpen)));
                    shares = BigDecimal.ZERO;
                    position = PositionType.NONE;
                }
            }

            // 2. Calculate equity at today's close
            BigDecimal dailyEquity;
            if (position == PositionType.LONG) {
                dailyEquity = shares.multiply(priceClose);
            } else if (position == PositionType.SHORT) {
                dailyEquity = currentCash.add(shares.multiply(entryPrice.subtract(priceClose)));
            } else {
                dailyEquity = currentCash;
            }

            // 3. Generate signal for tomorrow's open based on today's close data
            Map<String, BigDecimal> indicators = indicatorMap.getOrDefault(date, Collections.emptyMap());
            BacktestSignal nextSignal = strategy.generateSignal(currentDay, indicators, position);

            // Record result for today
            results.add(BacktestResult.builder()
                    .ticker(ticker)
                    .date(date)
                    .strategyName(strategy.getName())
                    .equity(dailyEquity.setScale(4, RoundingMode.HALF_UP))
                    .position(position.name())
                    .price(priceClose)
                    .signal(nextSignal.name())
                    .build());

            pendingSignal = nextSignal;
        }

        backtestResultRepository.saveAll(results);
    }
}
