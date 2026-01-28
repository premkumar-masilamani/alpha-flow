package com.alphaflow.core;

import com.alphaflow.domain.enums.PositionType;
import com.alphaflow.domain.enums.TradeAction;
import com.alphaflow.domain.enums.TradeSignal;
import com.alphaflow.domain.strategy.BacktestStrategy;
import com.alphaflow.domain.strategy.RenkoBacktestStrategy;
import com.alphaflow.infrastructure.persistence.entities.*;
import com.alphaflow.infrastructure.persistence.repositories.BacktestResultRepository;
import com.alphaflow.infrastructure.persistence.repositories.MarketDataRepository;
import com.alphaflow.infrastructure.persistence.repositories.MarketStateRepository;
import com.alphaflow.infrastructure.persistence.repositories.TickerRepository;
import com.alphaflow.infrastructure.util.RenkoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BacktestComputer {

    private static final Logger log = LoggerFactory.getLogger(BacktestComputer.class);
    private static final BigDecimal INITIAL_EQUITY = new BigDecimal("100000.00000000");

    private final TickerRepository tickerRepository;
    private final MarketDataRepository marketDataRepository;
    private final MarketStateRepository marketStateRepository;
    private final BacktestResultRepository backtestResultRepository;
    private final List<BacktestStrategy> strategies;
    private final List<RenkoBacktestStrategy> renkoStrategies;
    private final TransactionTemplate transactionTemplate;

    public BacktestComputer(
            TickerRepository tickerRepository,
            MarketDataRepository marketDataRepository,
            MarketStateRepository marketStateRepository,
            BacktestResultRepository backtestResultRepository,
            List<BacktestStrategy> strategies,
            List<RenkoBacktestStrategy> renkoStrategies,
            TransactionTemplate transactionTemplate
    ) {
        this.tickerRepository = tickerRepository;
        this.marketDataRepository = marketDataRepository;
        this.marketStateRepository = marketStateRepository;
        this.backtestResultRepository = backtestResultRepository;
        this.strategies = strategies;
        this.renkoStrategies = renkoStrategies;
        this.transactionTemplate = transactionTemplate;
    }

    public void compute() {
        log.info("Starting Backtest Computation for all active tickers");

        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            log.info("Running backtests for {}", ticker.getTickerSymbol());

            List<MarketData> marketDataList = marketDataRepository.findByTickerOrderByMarketDataDateAsc(ticker);
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

            // Standard Strategies
            for (BacktestStrategy strategy : strategies) {
                log.info("Running strategy: {} for {}", strategy.getName(), ticker.getTickerSymbol());
                List<BacktestResult> results = runBacktest(ticker, strategy, marketDataList, indicatorMap);
                saveResults(ticker, strategy.getName(), results);
            }

            // Renko Strategies
            for (RenkoBacktestStrategy strategy : renkoStrategies) {
                log.info("Running renko strategy: {} for {}", strategy.getName(), ticker.getTickerSymbol());
                List<BacktestResult> results = runRenkoBacktest(ticker, strategy, marketDataList, indicatorMap);
                saveResults(ticker, strategy.getName(), results);
            }
        });

        log.info("Backtest Computation completed.");
    }

    private void saveResults(Ticker ticker, String strategyName, List<BacktestResult> results) {
        transactionTemplate.execute(status -> {
            backtestResultRepository.deleteByTickerIdAndStrategyName(ticker.getTickerId(), strategyName);
            backtestResultRepository.saveAllAndFlush(results);
            return status;
        });
    }

    private List<BacktestResult> runBacktest(
            Ticker ticker,
            BacktestStrategy strategy,
            List<MarketData> marketDataList,
            Map<LocalDate, Map<String, BigDecimal>> indicatorMap
    ) {

        BigDecimal currentCash = INITIAL_EQUITY;
        PositionType position = PositionType.NONE;
        BigDecimal shares = BigDecimal.ZERO;
        BigDecimal entryPrice = BigDecimal.ZERO; // for shorts

        TradeSignal pendingSignal =
                new TradeSignal(TradeAction.NO_SIGNAL, PositionType.NONE);

        List<BacktestResult> results = new ArrayList<>();

        for (MarketData currentDay : marketDataList) {

            LocalDate date = currentDay.getMarketDataDate();
            BigDecimal priceOpen = currentDay.getPriceOpen();
            BigDecimal priceClose = currentDay.getPriceClose();

            // 1. Execute pending signal at today's open
            PositionUpdate update = executeSignal(pendingSignal, position, shares, currentCash, entryPrice, priceOpen);
            position = update.position();
            shares = update.shares();
            currentCash = update.currentCash();
            entryPrice = update.entryPrice();

            // 2. Equity at today's close
            BigDecimal dailyEquity = calculateEquity(position, shares, currentCash, entryPrice, priceClose);

            // 3. Generate next signal
            Map<String, BigDecimal> indicators =
                    indicatorMap.getOrDefault(date, Collections.emptyMap());

            TradeSignal nextSignal =
                    strategy.generateSignal(currentDay, indicators, position);

            // 4. Record result
            results.add(recordResult(ticker, strategy.getName(), date, dailyEquity, position, priceClose, nextSignal));

            pendingSignal = nextSignal;
        }

        return results;
    }

    private List<BacktestResult> runRenkoBacktest(
            Ticker ticker,
            RenkoBacktestStrategy strategy,
            List<MarketData> marketDataList,
            Map<LocalDate, Map<String, BigDecimal>> indicatorMap
    ) {

        BigDecimal currentCash = INITIAL_EQUITY;
        PositionType position = PositionType.NONE;
        BigDecimal shares = BigDecimal.ZERO;
        BigDecimal entryPrice = BigDecimal.ZERO;

        TradeSignal pendingSignal =
                new TradeSignal(TradeAction.NO_SIGNAL, PositionType.NONE);

        List<BacktestResult> results = new ArrayList<>();
        Map<String, Object> strategyState = new HashMap<>();

        for (int i = 0; i < marketDataList.size(); i++) {
            MarketData currentDay = marketDataList.get(i);
            LocalDate date = currentDay.getMarketDataDate();
            BigDecimal priceOpen = currentDay.getPriceOpen();
            BigDecimal priceClose = currentDay.getPriceClose();

            // 1. Execute pending signal at today's open
            PositionUpdate update = executeSignal(pendingSignal, position, shares, currentCash, entryPrice, priceOpen);
            position = update.position();
            shares = update.shares();
            currentCash = update.currentCash();
            entryPrice = update.entryPrice();

            // 2. Equity at today's close
            BigDecimal dailyEquity = calculateEquity(position, shares, currentCash, entryPrice, priceClose);

            // 3. Generate next signal
            // Recompute Renko bricks based on data up to today (Compute Intensive)
            List<MarketData> subSeries = marketDataList.subList(0, i + 1);
            BigDecimal brickSize = RenkoUtil.calculateBrickSize(subSeries);
            List<RenkoData> bricks = RenkoUtil.generateRenkoBricks(ticker, subSeries, brickSize);

            Map<String, BigDecimal> indicators =
                    indicatorMap.getOrDefault(date, Collections.emptyMap());

            TradeSignal nextSignal =
                    strategy.generateSignal(currentDay, bricks, indicators, position, strategyState);

            // 4. Record result
            results.add(recordResult(ticker, strategy.getName(), date, dailyEquity, position, priceClose, nextSignal));

            pendingSignal = nextSignal;
        }

        return results;
    }

    private PositionUpdate executeSignal(TradeSignal pendingSignal, PositionType position, BigDecimal shares, BigDecimal currentCash, BigDecimal entryPrice, BigDecimal priceOpen) {
        PositionType newPosition = position;
        BigDecimal newShares = shares;
        BigDecimal newCash = currentCash;
        BigDecimal newEntryPrice = entryPrice;

        if (pendingSignal.action() != TradeAction.NO_SIGNAL &&
                pendingSignal.action() != TradeAction.HOLD) {

            BigDecimal totalEquityAtOpen =
                    switch (position) {
                        case SHORT_25, SHORT_50, SHORT_100 -> currentCash.add(
                                shares.multiply(entryPrice.subtract(priceOpen))
                        );
                        case LONG_25, LONG_50, LONG_100 -> currentCash.add(shares.multiply(priceOpen));
                        default -> currentCash;
                    };

            switch (pendingSignal.action()) {
                case ENTER_LONG, REDUCE -> {
                    PositionType target = pendingSignal.targetPosition();
                    BigDecimal allocation = positionFraction(target);
                    newShares = totalEquityAtOpen
                            .multiply(allocation)
                            .divide(priceOpen, 8, RoundingMode.HALF_UP);
                    newCash = totalEquityAtOpen.subtract(newShares.multiply(priceOpen));
                    newPosition = target;
                }
                case ENTER_SHORT -> {
                    PositionType target = pendingSignal.targetPosition();
                    BigDecimal allocation = positionFraction(target);
                    newShares = totalEquityAtOpen
                            .multiply(allocation)
                            .divide(priceOpen, 8, RoundingMode.HALF_UP);
                    newCash = totalEquityAtOpen;
                    newEntryPrice = priceOpen;
                    newPosition = target;
                }
                case EXIT -> {
                    newCash = totalEquityAtOpen;
                    newShares = BigDecimal.ZERO;
                    newPosition = PositionType.NONE;
                }
                default -> {}
            }
        }
        return new PositionUpdate(newPosition, newShares, newCash, newEntryPrice);
    }

    private BigDecimal calculateEquity(PositionType position, BigDecimal shares, BigDecimal currentCash, BigDecimal entryPrice, BigDecimal priceClose) {
        return switch (position) {
            case LONG_25, LONG_50, LONG_100 -> currentCash.add(shares.multiply(priceClose));
            case SHORT_25, SHORT_50, SHORT_100 -> currentCash.add(
                    shares.multiply(entryPrice.subtract(priceClose))
            );
            default -> currentCash;
        };
    }

    private BacktestResult recordResult(Ticker ticker, String strategyName, LocalDate date, BigDecimal dailyEquity, PositionType position, BigDecimal priceClose, TradeSignal nextSignal) {
        return BacktestResult.builder()
                .ticker(ticker)
                .date(date)
                .strategyName(strategyName)
                .equity(dailyEquity.setScale(8, RoundingMode.HALF_UP))
                .position(position.name())
                .price(priceClose)
                .signal(nextSignal.action().name())
                .build();
    }

    private BigDecimal positionFraction(PositionType positionType) {
        return switch (positionType) {
            case LONG_25, SHORT_25 -> new BigDecimal("0.25");
            case LONG_50, SHORT_50 -> new BigDecimal("0.50");
            case LONG_100, SHORT_100 -> BigDecimal.ONE;
            default -> BigDecimal.ZERO;
        };
    }

    private record PositionUpdate(PositionType position, BigDecimal shares, BigDecimal currentCash, BigDecimal entryPrice) {}
}
