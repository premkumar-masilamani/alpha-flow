package com.alphaflow.backtest.engine;

import com.alphaflow.backtest.entities.*;
import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.repositories.*;
import com.alphaflow.backtest.strategies.Strategy;
import com.alphaflow.backtest.strategies.StrategyContext;
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
import java.util.stream.Collectors;

import static com.alphaflow.backtest.enums.PositionType.LONG;
import static com.alphaflow.backtest.enums.PositionType.SHORT;
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
    protected final List<? extends Strategy> strategies;
    protected final TransactionTemplate transactionTemplate;

    protected AbstractBacktester(
            TickerRepository tickerRepository,
            MarketDataRepository marketDataRepository,
            MarketStateRepository marketStateRepository,
            BacktestEquityRepository backtestEquityRepository,
            BacktestSignalRepository backtestSignalRepository,
            BacktestTradeRepository backtestTradeRepository,
            BacktestResultRepository backtestResultRepository,
            BacktestStrategyRepository backtestStrategyRepository,
            List<? extends Strategy> strategies,
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

    private static BigDecimal calculateEquity(PositionType currentPosition, BigDecimal cash, BigDecimal shares, BigDecimal open, BigDecimal entryPrice) {
        return switch (currentPosition) {
            case LONG -> cash.add(shares.multiply(open));
            case SHORT -> cash.add(shares.multiply(entryPrice.subtract(open)));
            default -> cash;
        };
    }

    protected List<RenkoData> buildRenkoBricks(Ticker ticker, List<MarketData> allData, int index, Strategy strategy) {
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

            Map<LocalDate, Map<String, BigDecimal>> indicators = buildIndicatorMap(ticker);

            for (Strategy strategy : strategies) {
                log.debug("Running backtest for ticker: {}, strategy: {}", ticker.getTickerSymbol(), strategy.getName());

                BacktestStrategy strategyEntity = transactionTemplate.execute(status ->
                        backtestStrategyRepository.findByName(strategy.getName())
                                .orElseGet(() -> backtestStrategyRepository.save(strategy.getEntity()))
                );

                BacktestRunResult runResult = runBacktest(ticker, strategyEntity, strategy, marketData, indicators);
                transactionTemplate.execute(status -> {
                    backtestEquityRepository.deleteByTickerAndBacktestStrategy(ticker, strategyEntity);
                    backtestSignalRepository.deleteByTickerAndBacktestStrategy(ticker, strategyEntity);
                    backtestTradeRepository.deleteByTickerAndBacktestStrategy(ticker, strategyEntity);
                    backtestResultRepository.deleteByTickerAndBacktestStrategy(ticker, strategyEntity);

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

    //TODO: Flatten the structure as market_data in the future
    private Map<LocalDate, Map<String, BigDecimal>> buildIndicatorMap(Ticker ticker) {
        return marketStateRepository.findByTickerOrderByMarketStateDateAsc(ticker)
                .stream()
                .collect(Collectors.groupingBy(
                        MarketState::getMarketStateDate,
                        Collectors.toMap(
                                ms -> ms.getMetric() + "_" + ms.getMaType() + "_" + ms.getPeriod(),
                                MarketState::getValue,
                                (a, b) -> a
                        )
                ));
    }

    private BacktestRunResult runBacktest(
            Ticker ticker,
            BacktestStrategy strategyEntity,
            Strategy strategy,
            List<MarketData> marketData,
            Map<LocalDate, Map<String, BigDecimal>> indicatorMap
    ) {

        BigDecimal cash = INITIAL_EQUITY;
        BigDecimal shares = BigDecimal.ZERO;
        BigDecimal entryPrice = BigDecimal.ZERO;
        PositionType currentPosition = PositionType.NONE;

        TradeAction pendingAction = new TradeAction(TradeSignal.NO_SIGNAL, PositionType.NONE);
        BacktestTrade activeTrade = null;

        List<BacktestEquity> equities = new ArrayList<>();
        List<BacktestSignal> signals = new ArrayList<>();
        List<BacktestTrade> trades = new ArrayList<>();

        Map<String, Object> strategyState = new HashMap<>();

        for (int i = 0; i < marketData.size(); i++) {

            MarketData data = marketData.get(i);
            LocalDate currentDate = data.getMarketDataDate();
            BigDecimal priceOpen = data.getPriceOpen();
            BigDecimal priceClose = data.getPriceClose();

            // ───── Execute pendingAction signal ─────
            if (pendingAction.tradeSignal() != TradeSignal.NO_SIGNAL &&
                    pendingAction.tradeSignal() != TradeSignal.HOLD) {

                BigDecimal equityAtOpen = calculateEquity(currentPosition, cash, shares, priceOpen, entryPrice);

                switch (pendingAction.tradeSignal()) {

                    case ENTER_LONG -> {
                        log.debug("Executing ENTER_LONG for {} at {} price {}", strategy.getName(), currentDate, priceOpen);
                        if (activeTrade != null) {
                            closeActiveTrade(activeTrade, currentDate, priceOpen, trades);
                        }

                        shares = equityAtOpen.divide(priceOpen, DB_MATH_CONTEXT);
                        cash = BigDecimal.ZERO;
                        currentPosition = LONG;

                        activeTrade = BacktestTrade.builder()
                                .ticker(ticker)
                                .backtestStrategy(strategyEntity)
                                .side(LONG)
                                .entryDate(currentDate)
                                .entryPrice(priceOpen)
                                .quantity(shares)
                                .holdingBars(0)
                                .build();
                    }

                    case ENTER_SHORT -> {
                        log.debug("Executing ENTER_SHORT for {} at {} price {}", strategy.getName(), currentDate, priceOpen);
                        if (activeTrade != null) {
                            closeActiveTrade(activeTrade, currentDate, priceOpen, trades);
                        }

                        shares = equityAtOpen.divide(priceOpen, DB_MATH_CONTEXT);
                        entryPrice = priceOpen;
                        cash = equityAtOpen;
                        currentPosition = SHORT;

                        activeTrade = BacktestTrade.builder()
                                .ticker(ticker)
                                .backtestStrategy(strategyEntity)
                                .side(SHORT)
                                .entryDate(currentDate)
                                .entryPrice(priceOpen)
                                .quantity(shares)
                                .holdingBars(0)
                                .build();
                    }

                    case EXIT -> {
                        log.debug("Executing EXIT for {} at {} price {}", strategy.getName(), currentDate, priceOpen);
                        if (activeTrade != null) {
                            closeActiveTrade(activeTrade, currentDate, priceOpen, trades);
                            activeTrade = null;
                        }
                        cash = equityAtOpen;
                        shares = BigDecimal.ZERO;
                        currentPosition = PositionType.NONE;
                    }
                }
            }

            if (activeTrade != null) {
                activeTrade.setHoldingBars(activeTrade.getHoldingBars() + 1);
            }

            // ───── Equity at priceClose ─────
            BigDecimal equityAtClose = calculateEquity(currentPosition, cash, shares, priceOpen, entryPrice);
            equities.add(BacktestEquity.builder()
                    .ticker(ticker)
                    .backtestStrategy(strategyEntity)
                    .equityDate(currentDate)
                    .equity(equityAtClose)
                    .position(currentPosition)
                    .priceClose(priceClose)
                    .build());

            // ───── Build Strategy Context ─────
            StrategyContext context = new StrategyContext(
                    data,
                    indicatorMap.getOrDefault(currentDate, Collections.emptyMap()),
                    currentPosition,
                    buildRenkoBricks(ticker, marketData, i, strategy), // used only for renko based strategies
                    strategyState // Placeholder to extra data
            );
            TradeAction nextAction = strategy.generateSignal(context);

            // Fetch the next trading date from list
            LocalDate executeDate = null;
            if (marketData.size() > i + 1) {
                executeDate = marketData.get(i + 1).getMarketDataDate();
            }

            signals.add(BacktestSignal.builder()
                    .ticker(ticker)
                    .backtestStrategy(strategyEntity)
                    .signalDate(currentDate)
                    .executeDate(executeDate)
                    .action(nextAction.tradeSignal())
                    .signalData(nextAction.signalData().toString())
                    .build());

            pendingAction = nextAction;
        }

        // Force close final open trade
        if (activeTrade != null) {
            MarketData lastBar = marketData.getLast();
            closeActiveTrade(activeTrade, lastBar.getMarketDataDate(), lastBar.getPriceClose(), trades);
        }

        BacktestResult result = buildResult(ticker, strategyEntity, equities, trades);

        return new BacktestRunResult(equities, signals, trades, result);
    }

    private void closeActiveTrade(
            BacktestTrade activeTrade,
            LocalDate currentDate,
            BigDecimal priceOpen,
            List<BacktestTrade> trades
    ) {
        BigDecimal priceDiff = switch (activeTrade.getSide()) {
            case LONG -> priceOpen.subtract(activeTrade.getEntryPrice());
            case SHORT -> activeTrade.getEntryPrice().subtract(priceOpen);
            default -> BigDecimal.ZERO;
        };
        BigDecimal pnl = activeTrade.getQuantity().multiply(priceDiff);

        BigDecimal entryValue = activeTrade.getQuantity().multiply(activeTrade.getEntryPrice());
        BigDecimal pnlPct = pnl.divide(entryValue, DB_MATH_CONTEXT).multiply(HUNDRED);

        activeTrade.setExitDate(currentDate);
        activeTrade.setExitPrice(priceOpen);
        activeTrade.setPnl(pnl);
        activeTrade.setPnlPct(pnlPct);

        trades.add(activeTrade);
    }


    private BacktestResult buildResult(
            Ticker ticker,
            BacktestStrategy strategyEntity,
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
                .backtestStrategy(strategyEntity)
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
