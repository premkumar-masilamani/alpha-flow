package com.alphaflow.backtest.engine;

import com.alphaflow.backtest.entities.BacktestEquities;
import com.alphaflow.backtest.entities.BacktestResult;
import com.alphaflow.backtest.entities.BacktestSignals;
import com.alphaflow.backtest.entities.BacktestTrades;
import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.repositories.*;
import com.alphaflow.backtest.strategies.Strategy;
import com.alphaflow.backtest.strategies.StrategyContext;
import com.alphaflow.infrastructure.entities.Candle;
import com.alphaflow.infrastructure.entities.Indicator;
import com.alphaflow.infrastructure.entities.Renko;
import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.repositories.CandleRepository;
import com.alphaflow.infrastructure.repositories.IndicatorRepository;
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
    protected final CandleRepository candleRepository;
    protected final IndicatorRepository indicatorRepository;
    protected final BacktestEquitiesRepository backtestEquitiesRepository;
    protected final BacktestSignalsRepository backtestSignalsRepository;
    protected final BacktestTradesRepository backtestTradesRepository;
    protected final BacktestResultRepository backtestResultRepository;
    protected final BacktestStrategyRepository backtestStrategyRepository;
    protected final TransactionTemplate transactionTemplate;
    protected List<? extends Strategy> strategies;

    protected AbstractBacktester(
            TickerRepository tickerRepository,
            CandleRepository candleRepository,
            IndicatorRepository indicatorRepository,
            BacktestEquitiesRepository backtestEquitiesRepository,
            BacktestSignalsRepository backtestSignalsRepository,
            BacktestTradesRepository backtestTradesRepository,
            BacktestResultRepository backtestResultRepository,
            BacktestStrategyRepository backtestStrategyRepository,
            List<? extends Strategy> strategies,
            TransactionTemplate transactionTemplate
    ) {
        this.tickerRepository = tickerRepository;
        this.candleRepository = candleRepository;
        this.indicatorRepository = indicatorRepository;
        this.backtestEquitiesRepository = backtestEquitiesRepository;
        this.backtestSignalsRepository = backtestSignalsRepository;
        this.backtestTradesRepository = backtestTradesRepository;
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

    protected List<Renko> buildRenkoBricks(Ticker ticker, List<Candle> allData, int index, Strategy strategy) {
        // Only the RenkoBacktester will implement the logic
        return null;
    }

    public void compute() {
        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            log.info("Starting backtests for ticker: {}", ticker.getTickerSymbol());
            List<Candle> marketData = candleRepository.findByTickerOrderByCandleDateAsc(ticker);
            if (marketData.isEmpty()) {
                log.warn("No market data found for ticker: {}", ticker.getTickerSymbol());
                return;
            }

            Map<LocalDate, Map<String, BigDecimal>> indicators = buildIndicatorMap(ticker);

            for (Strategy strategy : strategies) {
                log.debug("Running backtest for ticker: {}, strategy: {}", ticker.getTickerSymbol(), strategy.getName());
                BacktestRunResult runResult = runBacktest(ticker, strategy, marketData, indicators);
                transactionTemplate.execute(status -> {
                    backtestEquitiesRepository.deleteByTickerAndStrategy(ticker, strategy.getEntity());
                    backtestSignalsRepository.deleteByTickerAndStrategy(ticker, strategy.getEntity());
                    backtestTradesRepository.deleteByTickerAndStrategy(ticker, strategy.getEntity());
                    backtestResultRepository.deleteByTickerAndStrategy(ticker, strategy.getEntity());

                    backtestEquitiesRepository.saveAll(runResult.backtestEquities());
                    backtestSignalsRepository.saveAll(runResult.backtestSignals());
                    backtestTradesRepository.saveAll(runResult.backtestTrades());
                    backtestResultRepository.save(runResult.backtestResult());
                    return status;
                });
            }
            log.info("Completed all backtests for ticker: {}", ticker.getTickerSymbol());
        });
    }

    //TODO: Flatten the structure as candles in the future
    private Map<LocalDate, Map<String, BigDecimal>> buildIndicatorMap(Ticker ticker) {
        return indicatorRepository.findByTickerOrderByIndicatorDateAsc(ticker)
                .stream()
                .collect(Collectors.groupingBy(
                        Indicator::getIndicatorDate,
                        Collectors.toMap(
                                ms -> ms.getMetric() + "_" + ms.getMaType() + "_" + ms.getPeriod(),
                                Indicator::getValue,
                                (a, b) -> a
                        )
                ));
    }

    private BacktestRunResult runBacktest(
            Ticker ticker,
            Strategy strategy,
            List<Candle> marketData,
            Map<LocalDate, Map<String, BigDecimal>> indicatorMap
    ) {

        BigDecimal cash = INITIAL_EQUITY;
        BigDecimal shares = BigDecimal.ZERO;
        BigDecimal entryPrice = BigDecimal.ZERO;
        PositionType currentPosition = PositionType.NONE;

        TradeAction pendingAction = new TradeAction(TradeSignal.NO_SIGNAL, PositionType.NONE);
        BacktestTrades activeTrade = null;

        List<BacktestEquities> equities = new ArrayList<>();
        List<BacktestSignals> signals = new ArrayList<>();
        List<BacktestTrades> trades = new ArrayList<>();

        Map<String, Object> strategyState = new HashMap<>();

        for (int i = 0; i < marketData.size(); i++) {

            Candle data = marketData.get(i);
            LocalDate currentDate = data.getCandleDate();
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

                        activeTrade = BacktestTrades.builder()
                                .ticker(ticker)
                                .strategy(strategy.getEntity())
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

                        activeTrade = BacktestTrades.builder()
                                .ticker(ticker)
                                .strategy(strategy.getEntity())
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
            equities.add(BacktestEquities.builder()
                    .ticker(ticker)
                    .strategy(strategy.getEntity())
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
                executeDate = marketData.get(i + 1).getCandleDate();
            }

            signals.add(BacktestSignals.builder()
                    .ticker(ticker)
                    .strategy(strategy.getEntity())
                    .signalDate(currentDate)
                    .executeDate(executeDate)
                    .action(nextAction.tradeSignal())
                    .signalData(nextAction.signalData().toString())
                    .build());

            pendingAction = nextAction;
        }

        // Force close final open trade
        if (activeTrade != null) {
            Candle lastBar = marketData.getLast();
            closeActiveTrade(activeTrade, lastBar.getCandleDate(), lastBar.getPriceClose(), trades);
        }

        BacktestResult result = buildResult(ticker, strategy, equities, trades);

        return new BacktestRunResult(equities, signals, trades, result);
    }

    private void closeActiveTrade(
            BacktestTrades activeTrade,
            LocalDate currentDate,
            BigDecimal priceOpen,
            List<BacktestTrades> trades
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
            Strategy strategy,
            List<BacktestEquities> equities,
            List<BacktestTrades> trades
    ) {
        if (equities.isEmpty()) return null;

        BacktestEquities first = equities.getFirst();
        BacktestEquities last = equities.getLast();

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
        List<BacktestTrades> winningTradesList = trades.stream()
                .filter(trade -> trade.getPnl() != null && trade.getPnl().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        List<BacktestTrades> losingTradesList = trades.stream()
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
                            .map(BacktestTrades::getPnlPct)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(winningTradesList.size()), DB_MATH_CONTEXT);

            avgLoss = losingTradesList.isEmpty() ? BigDecimal.ZERO :
                    losingTradesList.stream()
                            .map(BacktestTrades::getPnlPct)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(losingTradesList.size()), DB_MATH_CONTEXT);

            BigDecimal grossProfit = winningTradesList.stream()
                    .map(BacktestTrades::getPnl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal grossLoss = losingTradesList.stream()
                    .map(BacktestTrades::getPnl)
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

    private BigDecimal calculateMaxDrawdown(List<BacktestEquities> equities) {
        if (equities.isEmpty()) return BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        BigDecimal peak = equities.getFirst().getEquity();
        for (BacktestEquities equity : equities) {
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

    private BigDecimal calculateSharpeRatio(List<BacktestEquities> equities) {
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
            List<BacktestEquities> backtestEquities,
            List<BacktestSignals> backtestSignals,
            List<BacktestTrades> backtestTrades,
            BacktestResult backtestResult
    ) {
    }
}
