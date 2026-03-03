package com.alphaflow.backtest.engine;

import com.alphaflow.backtest.entities.BacktestEquity;
import com.alphaflow.backtest.entities.BacktestResult;
import com.alphaflow.backtest.entities.BacktestSignal;
import com.alphaflow.backtest.entities.BacktestTrade;
import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.repositories.*;
import com.alphaflow.backtest.strategies.Strategy;
import com.alphaflow.backtest.strategies.StrategyContext;
import com.alphaflow.infrastructure.entities.CandleData;
import com.alphaflow.infrastructure.entities.Indicator;
import com.alphaflow.infrastructure.entities.RenkoData;
import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.repositories.CandleDataRepository;
import com.alphaflow.infrastructure.repositories.IndicatorRepository;
import com.alphaflow.infrastructure.repositories.TickerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    protected final TickerRepository tickerRepository;
    protected final CandleDataRepository candleDataRepository;
    protected final IndicatorRepository indicatorRepository;
    protected final BacktestEquityRepository backtestEquityRepository;
    protected final BacktestSignalRepository backtestSignalRepository;
    protected final BacktestTradeRepository backtestTradeRepository;
    protected final BacktestResultRepository backtestResultRepository;
    protected final BacktestStrategyRepository backtestStrategyRepository;
    protected final TransactionTemplate transactionTemplate;
    protected List<? extends Strategy> strategies;

    protected AbstractBacktester(
            TickerRepository tickerRepository,
            CandleDataRepository candleDataRepository,
            IndicatorRepository indicatorRepository,
            BacktestEquityRepository backtestEquityRepository,
            BacktestSignalRepository backtestSignalRepository,
            BacktestTradeRepository backtestTradeRepository,
            BacktestResultRepository backtestResultRepository,
            BacktestStrategyRepository backtestStrategyRepository,
            List<? extends Strategy> strategies,
            TransactionTemplate transactionTemplate
    ) {
        this.tickerRepository = tickerRepository;
        this.candleDataRepository = candleDataRepository;
        this.indicatorRepository = indicatorRepository;
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

    private static boolean isInvalidEntryPrice(BigDecimal price) {
        return price == null || price.compareTo(BigDecimal.ZERO) <= 0;
    }

    protected List<RenkoData> buildRenkoData(Ticker ticker, List<CandleData> allData, int index, Strategy strategy) {
        // Only the RenkoBacktester will implement the logic
        return null;
    }

    public void compute() {
        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            log.info("Starting backtests for ticker: {}", ticker.getTickerSymbol());
            List<CandleData> candleData = candleDataRepository.findByTickerOrderByCandleDataDateAsc(ticker);
            if (candleData.isEmpty()) {
                log.warn("No market data found for ticker: {}", ticker.getTickerSymbol());
                return;
            }

            Map<LocalDate, Map<String, BigDecimal>> indicators = buildIndicatorMap(ticker);

            for (Strategy strategy : strategies) {
                log.debug("Running backtest for ticker: {}, strategy: {}", ticker.getTickerSymbol(), strategy.getName());

                Optional<BacktestSignal> lastSignalOpt = backtestSignalRepository.findTopByTickerAndStrategyOrderBySignalDateDesc(ticker, strategy.getEntity());

                if (lastSignalOpt.isPresent()) {
                    LocalDate lastSignalDate = lastSignalOpt.get().getSignalDate();
                    if (lastSignalDate.equals(candleData.getLast().getCandleDataDate())) {
                        log.debug("Backtest for {} / {} is already up to date.", ticker.getTickerSymbol(), strategy.getName());
                        continue;
                    }

                    BacktestRunResult runResult = runBacktest(ticker, strategy, candleData, indicators, lastSignalOpt.get());
                    transactionTemplate.execute(status -> {
                        backtestEquityRepository.saveAll(runResult.backtestEquity());
                        backtestSignalRepository.saveAll(runResult.backtestSignal());
                        backtestTradeRepository.saveAll(runResult.backtestTrades());
                        backtestResultRepository.save(runResult.backtestResult());
                        return status;
                    });
                } else {
                    BacktestRunResult runResult = runBacktest(ticker, strategy, candleData, indicators, null);
                    transactionTemplate.execute(status -> {
                        backtestEquityRepository.saveAll(runResult.backtestEquity());
                        backtestSignalRepository.saveAll(runResult.backtestSignal());
                        backtestTradeRepository.saveAll(runResult.backtestTrades());
                        backtestResultRepository.save(runResult.backtestResult());
                        return status;
                    });
                }
            }
            log.info("Completed all backtests for ticker: {}", ticker.getTickerSymbol());
        });
    }

    private String serializeState(Map<String, Object> state) {
        try {
            return OBJECT_MAPPER.writeValueAsString(state);
        } catch (Exception e) {
            log.error("Failed to serialize strategy state", e);
            return null;
        }
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
            List<CandleData> candleData,
            Map<LocalDate, Map<String, BigDecimal>> indicatorMap,
            BacktestSignal lastSignal
    ) {

        BigDecimal cash = INITIAL_EQUITY;
        BigDecimal shares = BigDecimal.ZERO;
        BigDecimal entryPrice = BigDecimal.ZERO;
        PositionType currentPosition = PositionType.NONE;

        TradeAction pendingAction = new TradeAction(TradeSignal.NO_SIGNAL, PositionType.NONE);
        BacktestTrade activeTrade = null;
        Map<String, Object> strategyState = new HashMap<>();

        int startIndex = 0;

        List<BacktestEquity> historicalEquities = new ArrayList<>();
        List<BacktestTrade> historicalTrades = new ArrayList<>();

        if (lastSignal != null) {
            LocalDate lastDate = lastSignal.getSignalDate();
            for (int i = 0; i < candleData.size(); i++) {
                if (candleData.get(i).getCandleDataDate().equals(lastDate)) {
                    startIndex = i + 1;
                    break;
                }
            }

            BacktestEquity lastEquity = backtestEquityRepository.findTopByTickerAndStrategyOrderByEquityDateDesc(ticker, strategy.getEntity())
                    .orElseThrow(() -> new IllegalStateException("Last signal exists but last equity not found"));

            final PositionType pos = lastEquity.getPosition();
            currentPosition = pos;
            if (currentPosition != PositionType.NONE) {
                activeTrade = backtestTradeRepository.findTopByTickerAndStrategyOrderByEntryDateDesc(ticker, strategy.getEntity())
                        .orElseThrow(() -> new IllegalStateException("Position is " + pos + " but no trade found"));

                shares = activeTrade.getQuantity();
                entryPrice = activeTrade.getEntryPrice();
                if (currentPosition == PositionType.LONG) {
                    cash = BigDecimal.ZERO;
                } else {
                    cash = activeTrade.getQuantity().multiply(activeTrade.getEntryPrice());
                }
            } else {
                cash = lastEquity.getEquity();
                shares = BigDecimal.ZERO;
            }

            pendingAction = new TradeAction(lastSignal.getAction(), currentPosition);

            if (lastSignal.getStrategyState() != null) {
                try {
                    strategyState = OBJECT_MAPPER.readValue(lastSignal.getStrategyState(), new TypeReference<Map<String, Object>>() {
                    });
                } catch (Exception e) {
                    log.error("Failed to deserialize strategy state for {} / {}", ticker.getTickerSymbol(), strategy.getName(), e);
                }
            }

            historicalEquities = backtestEquityRepository.findByTickerAndStrategyOrderByEquityDateAsc(ticker, strategy.getEntity());
            historicalTrades = backtestTradeRepository.findByTickerAndStrategyOrderByEntryDateAsc(ticker, strategy.getEntity());
            if (activeTrade != null && !historicalTrades.isEmpty()) {
                historicalTrades.removeLast();
            }
        }

        List<BacktestEquity> equities = new ArrayList<>();
        List<BacktestSignal> signals = new ArrayList<>();
        List<BacktestTrade> trades = new ArrayList<>();

        for (int i = startIndex; i < candleData.size(); i++) {

            CandleData data = candleData.get(i);
            LocalDate currentDate = data.getCandleDataDate();
            BigDecimal priceOpen = data.getPriceOpen();
            BigDecimal priceClose = data.getPriceClose();

            // ───── Execute pendingAction signal ─────
            if (pendingAction.tradeSignal() != TradeSignal.NO_SIGNAL &&
                    pendingAction.tradeSignal() != TradeSignal.HOLD) {

                BigDecimal equityAtOpen = calculateEquity(currentPosition, cash, shares, priceOpen, entryPrice);

                switch (pendingAction.tradeSignal()) {

                    case ENTER_LONG -> {
                        log.debug("Executing ENTER_LONG for {} at {} price {}", strategy.getName(), currentDate, priceOpen);
                        // TODO: Verify this logic
                        if (isInvalidEntryPrice(priceOpen)) {
                            log.warn("Skipping ENTER_LONG execution for {} on {} due invalid entry price: {}", strategy.getName(), currentDate, priceOpen);
                            break;
                        }
                        if (activeTrade != null) {
                            closeActiveTrade(activeTrade, currentDate, priceOpen, trades);
                        }

                        shares = equityAtOpen.divide(priceOpen, DB_MATH_CONTEXT);
                        cash = BigDecimal.ZERO;
                        currentPosition = LONG;

                        activeTrade = BacktestTrade.builder()
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
                        if (isInvalidEntryPrice(priceOpen)) {
                            log.warn("Skipping ENTER_SHORT execution for {} on {} due invalid entry price: {}", strategy.getName(), currentDate, priceOpen);
                            break;
                        }
                        if (activeTrade != null) {
                            closeActiveTrade(activeTrade, currentDate, priceOpen, trades);
                        }

                        shares = equityAtOpen.divide(priceOpen, DB_MATH_CONTEXT);
                        entryPrice = priceOpen;
                        cash = equityAtOpen;
                        currentPosition = SHORT;

                        activeTrade = BacktestTrade.builder()
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
            equities.add(BacktestEquity.builder()
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
                    buildRenkoData(ticker, candleData, i, strategy), // used only for renko based strategies
                    strategyState // Placeholder to extra data
            );
            TradeAction nextAction = strategy.generateSignal(context);

            // Fetch the next trading date from list
            LocalDate executeDate = null;
            if (candleData.size() > i + 1) {
                executeDate = candleData.get(i + 1).getCandleDataDate();
            }

            signals.add(BacktestSignal.builder()
                    .ticker(ticker)
                    .strategy(strategy.getEntity())
                    .signalDate(currentDate)
                    .executeDate(executeDate)
                    .action(nextAction.tradeSignal())
                    .signalData(nextAction.signalData().toString())
                    .strategyState(serializeState(strategyState))
                    .build());

            pendingAction = nextAction;
        }

        // Force close final open trade
        if (activeTrade != null) {
            CandleData lastBar = candleData.getLast();
            closeActiveTrade(activeTrade, lastBar.getCandleDataDate(), lastBar.getPriceClose(), trades);
        }

        List<BacktestEquity> allEquitiesForMetrics = new ArrayList<>(historicalEquities);
        allEquitiesForMetrics.addAll(equities);

        List<BacktestTrade> allTradesForMetrics = new ArrayList<>(historicalTrades);
        allTradesForMetrics.addAll(trades);

        BacktestResult result = buildResult(ticker, strategy, allEquitiesForMetrics, allTradesForMetrics);

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
        // TODO: Review this logic
        BigDecimal pnlPct = BigDecimal.ZERO;
        if (entryValue.compareTo(BigDecimal.ZERO) != 0) {
            pnlPct = pnl.divide(entryValue, DB_MATH_CONTEXT).multiply(HUNDRED);
        } else {
            log.warn("Skipping pnlPct calculation for trade on {} due zero entry value. entryPrice={}, quantity={}", currentDate, activeTrade.getEntryPrice(), activeTrade.getQuantity());
        }

        activeTrade.setExitDate(currentDate);
        activeTrade.setExitPrice(priceOpen);
        activeTrade.setPnl(pnl);
        activeTrade.setPnlPct(pnlPct);

        trades.add(activeTrade);
    }


    private BacktestResult buildResult(
            Ticker ticker,
            Strategy strategy,
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

        final BigDecimal finalWinRate = winRate;
        final BigDecimal finalAvgWin = avgWin;
        final BigDecimal finalAvgLoss = avgLoss;
        final BigDecimal finalProfitFactor = profitFactor;
        final BigDecimal finalExpectancy = expectancy;
        return backtestResultRepository.findByTickerAndStrategy(ticker, strategy.getEntity())
                .map(result -> {
                    result.setInitialEquity(INITIAL_EQUITY);
                    result.setFinalEquity(last.getEquity());
                    result.setStartDate(first.getEquityDate());
                    result.setEndDate(last.getEquityDate());
                    result.setYears(BigDecimal.valueOf(Double.isNaN(years) || Double.isInfinite(years) ? 0.0 : years));
                    result.setCagr(BigDecimal.valueOf(Double.isNaN(cagr) || Double.isInfinite(cagr) ? 0.0 : cagr));
                    result.setWinRate(finalWinRate);
                    result.setTotalReturnPct(totalReturnPct);
                    result.setMaxDrawdownPct(calculateMaxDrawdown(equities));
                    result.setSharpeRatio(calculateSharpeRatio(equities));
                    result.setTotalTrades(totalTrades);
                    result.setAvgWin(finalAvgWin);
                    result.setAvgLoss(finalAvgLoss);
                    result.setProfitFactor(finalProfitFactor);
                    result.setExpectancy(finalExpectancy);
                    return result;
                })
                .orElse(BacktestResult.builder()
                        .ticker(ticker)
                        .strategy(strategy.getEntity())
                        .initialEquity(INITIAL_EQUITY)
                        .finalEquity(last.getEquity())
                        .startDate(first.getEquityDate())
                        .endDate(last.getEquityDate())
                        .years(BigDecimal.valueOf(Double.isNaN(years) || Double.isInfinite(years) ? 0.0 : years))
                        .cagr(BigDecimal.valueOf(Double.isNaN(cagr) || Double.isInfinite(cagr) ? 0.0 : cagr))
                        .winRate(finalWinRate)
                        .totalReturnPct(totalReturnPct)
                        .maxDrawdownPct(calculateMaxDrawdown(equities))
                        .sharpeRatio(calculateSharpeRatio(equities))
                        .totalTrades(totalTrades)
                        .avgWin(finalAvgWin)
                        .avgLoss(finalAvgLoss)
                        .profitFactor(finalProfitFactor)
                        .expectancy(finalExpectancy)
                        .build());
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
            List<BacktestEquity> backtestEquity,
            List<BacktestSignal> backtestSignal,
            List<BacktestTrade> backtestTrades,
            BacktestResult backtestResult
    ) {
    }
}
