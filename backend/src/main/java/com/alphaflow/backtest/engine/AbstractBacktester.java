package com.alphaflow.backtest.engine;

import com.alphaflow.backtest.entities.BacktestEquity;
import com.alphaflow.backtest.entities.BacktestResult;
import com.alphaflow.backtest.entities.BacktestSignal;
import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.entities.BacktestTrade;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.indicators.IndicatorKey;
import com.alphaflow.backtest.repositories.*;
import com.alphaflow.backtest.strategies.Strategy;
import com.alphaflow.backtest.strategies.StrategyContext;
import com.alphaflow.backtest.strategies.StrategyState;
import com.alphaflow.infrastructure.entities.MarketData;
import com.alphaflow.infrastructure.entities.MarketState;
import com.alphaflow.infrastructure.entities.RenkoData;
import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.repositories.MarketDataRepository;
import com.alphaflow.infrastructure.repositories.MarketStateRepository;
import com.alphaflow.infrastructure.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.alphaflow.infrastructure.constants.AppConstants.DB_MATH_CONTEXT;

public abstract class AbstractBacktester {

    protected static final BigDecimal INITIAL_EQUITY = new BigDecimal("100000");
    protected static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    protected static final double YEAR_IN_DAYS = 365.25;

    private static final Logger log = LoggerFactory.getLogger(AbstractBacktester.class);

    protected final TickerRepository tickerRepository;
    protected final MarketDataRepository marketDataRepository;
    protected final MarketStateRepository marketStateRepository;
    protected final BacktestEquityRepository backtestEquityRepository;
    protected final BacktestSignalRepository backtestSignalRepository;
    protected final BacktestTradeRepository backtestTradeRepository;
    protected final BacktestResultRepository backtestResultRepository;
    protected final BacktestStrategyRepository backtestStrategyRepository;
    protected final TransactionTemplate transactionTemplate;
    protected List<Strategy<? extends StrategyState>> strategies;

    protected AbstractBacktester(
            TickerRepository tickerRepository,
            MarketDataRepository marketDataRepository,
            MarketStateRepository marketStateRepository,
            BacktestEquityRepository backtestEquityRepository,
            BacktestSignalRepository backtestSignalRepository,
            BacktestTradeRepository backtestTradeRepository,
            BacktestResultRepository backtestResultRepository,
            BacktestStrategyRepository backtestStrategyRepository,
            List<Strategy<? extends StrategyState>> strategies,
            TransactionTemplate transactionTemplate
    ) {
        this.tickerRepository = tickerRepository;
        this.marketDataRepository = marketDataRepository;
        this.marketStateRepository = marketStateRepository;
        this.backtestEquityRepository = backtestEquityRepository;
        this.backtestSignalRepository = backtestSignalRepository;
        this.backtestTradeRepository = backtestTradeRepository;
        this.backtestResultRepository = backtestResultRepository;
        this.backtestStrategyRepository = backtestStrategyRepository;
        this.strategies = strategies;
        this.transactionTemplate = transactionTemplate;
    }

    protected List<RenkoData> buildRenkoBricks(Ticker ticker, List<MarketData> allData, int index, Strategy<? extends StrategyState> strategy) {
        // Only the RenkoBacktester will implement the logic
        return null;
    }

    public void compute() {
        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            log.info("Starting backtests for ticker: {}", ticker.getTickerSymbol());
            List<MarketData> marketData = marketDataRepository.findByTickerOrderByMarketDataDateAsc(ticker);
            if (marketData.isEmpty()) {
                log.warn("No market data found for ticker: {}", ticker.getTickerSymbol());
                return;
            }

            Map<LocalDate, Map<IndicatorKey, BigDecimal>> indicators = buildIndicatorMap(ticker);

            for (Strategy<? extends StrategyState> strategy : strategies) {
                BacktestStrategy strategyEntity = requireStrategyEntity(strategy);
                log.debug("Running backtest for ticker: {}, strategy: {}", ticker.getTickerSymbol(), strategy.getName());
                BacktestRunResult runResult = runBacktest(ticker, strategy, marketData, indicators);
                transactionTemplate.execute(status -> {
                    backtestEquityRepository.deleteByTickerAndStrategy(ticker, strategyEntity);
                    backtestSignalRepository.deleteByTickerAndStrategy(ticker, strategyEntity);
                    backtestTradeRepository.deleteByTickerAndStrategy(ticker, strategyEntity);
                    backtestResultRepository.deleteByTickerAndStrategy(ticker, strategyEntity);

                    backtestEquityRepository.saveAll(runResult.backtestEquities());
                    backtestSignalRepository.saveAll(runResult.backtestSignals());
                    backtestTradeRepository.saveAll(runResult.backtestTrades());
                    backtestResultRepository.save(runResult.backtestResult());
                    return status;
                });
            }
            log.info("Completed all backtests for ticker: {}", ticker.getTickerSymbol());
        });
    }

    private BacktestStrategy requireStrategyEntity(Strategy<? extends StrategyState> strategy) {
        BacktestStrategy entity = strategy.getEntity();
        if (entity == null) {
            throw new IllegalStateException("Backtest strategy entity is not initialized for " + strategy.getClass().getSimpleName());
        }
        return entity;
    }

    //TODO: Flatten the structure as market_data in the future
    private Map<LocalDate, Map<IndicatorKey, BigDecimal>> buildIndicatorMap(Ticker ticker) {
        Map<LocalDate, Map<IndicatorKey, BigDecimal>> byDate = new HashMap<>();
        List<MarketState> states = marketStateRepository.findByTickerOrderByMarketStateDateAsc(ticker);
        for (MarketState state : states) {
            LocalDate date = state.getMarketStateDate();
            Map<IndicatorKey, BigDecimal> indicatorMap = byDate.computeIfAbsent(date, ignored -> new HashMap<>());
            IndicatorKey key = new IndicatorKey(state.getMetric(), state.getMaType(), state.getPeriod());
            if (indicatorMap.putIfAbsent(key, state.getValue()) != null) {
                throw new IllegalStateException("Duplicate indicator for " + ticker.getTickerSymbol() + " on " + date + ": " + key);
            }
        }
        return byDate;
    }

    private BacktestRunResult runBacktest(
            Ticker ticker,
            Strategy<? extends StrategyState> strategy,
            List<MarketData> marketData,
            Map<LocalDate, Map<IndicatorKey, BigDecimal>> indicatorMap
    ) {
        return runBacktestInternal(ticker, strategy, marketData, indicatorMap);
    }

    private <S extends StrategyState> BacktestRunResult runBacktestInternal(
            Ticker ticker,
            Strategy<S> strategy,
            List<MarketData> marketData,
            Map<LocalDate, Map<IndicatorKey, BigDecimal>> indicatorMap
    ) {

        Portfolio portfolio = new Portfolio(INITIAL_EQUITY);
        TradeExecutor executor = new TradeExecutor(portfolio, ticker, strategy.getEntity());
        S strategyState = strategy.initialState();

        List<BacktestEquity> equities = new ArrayList<>();
        List<BacktestSignal> signals = new ArrayList<>();
        List<BacktestTrade> trades = new ArrayList<>();

        ScheduledTrade scheduledTrade = null;

        for (int i = 0; i < marketData.size(); i++) {

            MarketData data = marketData.get(i);
            LocalDate currentDate = data.getMarketDataDate();
            BigDecimal priceOpen = data.getPriceOpen();
            BigDecimal priceClose = data.getPriceClose();

            if (scheduledTrade != null && currentDate.equals(scheduledTrade.executeDate())) {
                log.debug("Executing {} for {} at {} price {}", scheduledTrade.action().tradeSignal(), strategy.getName(), currentDate, priceOpen);
                executor.execute(scheduledTrade.action(), currentDate, priceOpen, trades);
                scheduledTrade = null;
            }

            executor.incrementHoldingBars();

            BigDecimal equityAtClose = portfolio.equityAtPrice(priceOpen);
            equities.add(BacktestEquity.builder()
                    .ticker(ticker)
                    .strategy(strategy.getEntity())
                    .equityDate(currentDate)
                    .equity(equityAtClose)
                    .position(portfolio.getPositionType())
                    .priceClose(priceClose)
                    .build());

            StrategyContext<S> context = new StrategyContext<>(
                    data,
                    indicatorMap.getOrDefault(currentDate, Collections.emptyMap()),
                    portfolio.getPositionType(),
                    buildRenkoBricks(ticker, marketData, i, strategy),
                    strategyState
            );
            TradeAction nextAction = strategy.generateSignal(context);

            LocalDate executeDate = null;
            if (marketData.size() > i + 1) {
                executeDate = marketData.get(i + 1).getMarketDataDate();
            }

            signals.add(BacktestSignal.builder()
                    .ticker(ticker)
                    .strategy(strategy.getEntity())
                    .signalDate(currentDate)
                    .executeDate(executeDate)
                    .action(nextAction.tradeSignal())
                    .signalData(nextAction.signalData().toString())
                    .build());

            if (executeDate != null) {
                scheduledTrade = new ScheduledTrade(nextAction, executeDate);
            }
        }

        if (executor.hasActiveTrade()) {
            MarketData lastBar = marketData.getLast();
            executor.closeOpenTrade(lastBar.getMarketDataDate(), lastBar.getPriceClose(), trades);
        }

        BacktestResult result = buildResult(ticker, strategy, equities, trades);

        return new BacktestRunResult(equities, signals, trades, result);
    }


    private BacktestResult buildResult(
            Ticker ticker,
            Strategy<? extends StrategyState> strategy,
            List<BacktestEquity> equities,
            List<BacktestTrade> trades
    ) {
        if (equities.isEmpty()) return null;

        BacktestEquity first = equities.getFirst();
        BacktestEquity last = equities.getLast();

        long days = ChronoUnit.DAYS.between(first.getEquityDate(), last.getEquityDate());
        if (days <= 0) return null;

        // CAGR Calculation
        double startValue = INITIAL_EQUITY.doubleValue();
        double endValue = last.getEquity().doubleValue();
        double growthFactor = endValue / startValue;
        double years = days / YEAR_IN_DAYS;
        double cagr;
        if (growthFactor > 0) {
            cagr = (Math.pow(growthFactor, 1.0 / years) - 1) * 100;
        } else {
            cagr = -100.0;
        }

        // Total Return Calculation
        BigDecimal totalReturnPct = last.getEquity().subtract(INITIAL_EQUITY)
                .divide(INITIAL_EQUITY, DB_MATH_CONTEXT).multiply(HUNDRED);

        // Win Rate Calculation
        int totalTrades = trades.size();
        List<BacktestTrade> winningTradesList = trades.stream()
                .filter(trade -> trade.getPnl() != null && trade.getPnl().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        List<BacktestTrade> losingTradesList = trades.stream()
                .filter(trade -> trade.getPnl() != null && trade.getPnl().compareTo(BigDecimal.ZERO) < 0)
                .toList();

        BigDecimal winRate = BigDecimal.ZERO;
        BigDecimal avgWin = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;
        BigDecimal profitFactor = BigDecimal.ZERO;
        BigDecimal expectancy = BigDecimal.ZERO;

        if (totalTrades > 0) {
            BigDecimal wins = BigDecimal.valueOf(winningTradesList.size());
            BigDecimal total = BigDecimal.valueOf(totalTrades);
            winRate = wins.divide(total, DB_MATH_CONTEXT).multiply(HUNDRED);

            avgWin = winningTradesList.isEmpty() ? BigDecimal.ZERO :
                    winningTradesList.stream()
                            .map(BacktestTrade::getPnlPct)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(winningTradesList.size()), DB_MATH_CONTEXT);

            avgLoss = losingTradesList.isEmpty() ? BigDecimal.ZERO :
                    losingTradesList.stream()
                            .map(BacktestTrade::getPnlPct)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(losingTradesList.size()), DB_MATH_CONTEXT);

            BigDecimal grossProfit = winningTradesList.stream()
                    .map(BacktestTrade::getPnl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal grossLoss = losingTradesList.stream()
                    .map(BacktestTrade::getPnl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).abs();

            if (grossLoss.compareTo(BigDecimal.ZERO) > 0) {
                profitFactor = grossProfit.divide(grossLoss, DB_MATH_CONTEXT);
            } else if (grossProfit.compareTo(BigDecimal.ZERO) > 0) {
                profitFactor = BigDecimal.valueOf(99.99);
            }

            BigDecimal winRateDecimal = winRate.divide(HUNDRED, DB_MATH_CONTEXT);
            BigDecimal lossRateDecimal = BigDecimal.ONE.subtract(winRateDecimal);
            expectancy = winRateDecimal.multiply(avgWin).add(lossRateDecimal.multiply(avgLoss));
        }

        return BacktestResult.builder()
                .ticker(ticker)
                .strategy(strategy.getEntity())
                .initialEquity(INITIAL_EQUITY)
                .finalEquity(last.getEquity())
                .startDate(first.getEquityDate())
                .endDate(last.getEquityDate())
                .years(BigDecimal.valueOf(Double.isNaN(years) || Double.isInfinite(years) ? 0.0 : years))
                .cagr(BigDecimal.valueOf(Double.isNaN(cagr) || Double.isInfinite(cagr) ? 0.0 : cagr))
                .winRate(winRate)
                .totalReturnPct(totalReturnPct)
                .maxDrawdownPct(calculateMaxDrawdown(equities))
                .sharpeRatio(calculateSharpeRatio(equities))
                .totalTrades(totalTrades)
                .avgWin(avgWin)
                .avgLoss(avgLoss)
                .profitFactor(profitFactor)
                .expectancy(expectancy)
                .build();
    }

    private BigDecimal calculateMaxDrawdown(List<BacktestEquity> equities) {
        if (equities.isEmpty()) return BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        BigDecimal peak = equities.getFirst().getEquity();
        for (BacktestEquity equity : equities) {
            if (equity.getEquity().compareTo(peak) > 0) {
                peak = equity.getEquity();
            }
            BigDecimal drawdown = peak.subtract(equity.getEquity()).divide(peak, DB_MATH_CONTEXT).multiply(HUNDRED);
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }
        return maxDrawdown;
    }

    private BigDecimal calculateSharpeRatio(List<BacktestEquity> equities) {
        if (equities.size() < 2) return BigDecimal.ZERO;
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < equities.size(); i++) {
            double prev = equities.get(i - 1).getEquity().doubleValue();
            double curr = equities.get(i).getEquity().doubleValue();
            if (prev > 0) {
                returns.add((curr / prev) - 1.0);
            }
        }
        if (returns.isEmpty()) return BigDecimal.ZERO;
        double mean = returns.stream().mapToDouble(d -> d).average().orElse(0.0);
        double variance = returns.stream().mapToDouble(d -> Math.pow(d - mean, 2)).sum() / returns.size();
        double stdDev = Math.sqrt(variance);
        if (stdDev == 0) return BigDecimal.ZERO;
        double sharpe = (mean / stdDev) * Math.sqrt(252);
        return BigDecimal.valueOf(Double.isNaN(sharpe) || Double.isInfinite(sharpe) ? 0.0 : sharpe);
    }

    protected record BacktestRunResult(
            List<BacktestEquity> backtestEquities,
            List<BacktestSignal> backtestSignals,
            List<BacktestTrade> backtestTrades,
            BacktestResult backtestResult
    ) {
    }
}
