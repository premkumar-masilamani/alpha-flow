package com.alphaflow.backtest.computers;

import com.alphaflow.backtest.entities.BacktestEquity;
import com.alphaflow.backtest.entities.BacktestResult;
import com.alphaflow.backtest.entities.BacktestSignal;
import com.alphaflow.backtest.entities.BacktestTrade;
import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.repositories.BacktestEquityRepository;
import com.alphaflow.backtest.repositories.BacktestResultRepository;
import com.alphaflow.backtest.repositories.BacktestSignalRepository;
import com.alphaflow.backtest.repositories.BacktestTradeRepository;
import com.alphaflow.backtest.strategies.Strategy;
import com.alphaflow.backtest.strategies.StrategyContext;
import com.alphaflow.infrastructure.entities.MarketData;
import com.alphaflow.infrastructure.entities.MarketState;
import com.alphaflow.infrastructure.entities.RenkoData;
import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.repositories.MarketDataRepository;
import com.alphaflow.infrastructure.repositories.MarketStateRepository;
import com.alphaflow.infrastructure.repositories.TickerRepository;
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

    protected final TickerRepository tickerRepository;
    protected final MarketDataRepository marketDataRepository;
    protected final MarketStateRepository marketStateRepository;
    protected final BacktestEquityRepository backtestEquityRepository;
    protected final BacktestSignalRepository backtestSignalRepository;
    protected final BacktestTradeRepository backtestTradeRepository;
    protected final BacktestResultRepository backtestResultRepository;
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

    protected List<RenkoData> buildRenkoBricks(Ticker ticker, List<MarketData> allData, int index) {
        // Only the RenkoBacktester will implement the logic
        return null;
    }

    public void compute() {
        tickerRepository.findByIsActiveTrue().forEach(ticker -> {
            List<MarketData> marketData = marketDataRepository.findByTickerOrderByMarketDataDateAsc(ticker);
            if (marketData.isEmpty()) return;

            Map<LocalDate, Map<String, BigDecimal>> indicators = buildIndicatorMap(ticker);

            for (Strategy strategy : strategies) {
                BacktestRunResult runResult = runBacktest(ticker, strategy, marketData, indicators);
                transactionTemplate.execute(status -> {
                    backtestEquityRepository.deleteByTickerAndStrategyName(ticker, strategy.getName());
                    backtestSignalRepository.deleteByTickerAndStrategyName(ticker, strategy.getName());
                    backtestTradeRepository.deleteByTickerAndStrategyName(ticker, strategy.getName());
                    backtestResultRepository.deleteByTickerAndStrategyName(ticker, strategy.getName());

                    backtestEquityRepository.saveAll(runResult.backtestEquities());
                    backtestSignalRepository.saveAll(runResult.backtestSignals());
                    backtestTradeRepository.saveAll(runResult.backtestTrades());
                    backtestResultRepository.save(runResult.backtestResult());
                    return status;
                });
            }
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
                        if (activeTrade != null) {
                            closeActiveTrade(activeTrade, currentDate, priceOpen, trades);
                        }

                        shares = equityAtOpen.divide(priceOpen, DB_MATH_CONTEXT);
                        cash = BigDecimal.ZERO;
                        currentPosition = LONG;

                        activeTrade = BacktestTrade.builder()
                                .ticker(ticker)
                                .strategyName(strategy.getName())
                                .side(LONG)
                                .entryDate(currentDate)
                                .entryPrice(priceOpen)
                                .quantity(shares)
                                .holdingBars(0)
                                .build();
                    }

                    case ENTER_SHORT -> {
                        if (activeTrade != null) {
                            closeActiveTrade(activeTrade, currentDate, priceOpen, trades);
                        }

                        shares = equityAtOpen.divide(priceOpen, DB_MATH_CONTEXT);
                        entryPrice = priceOpen;
                        cash = equityAtOpen;
                        currentPosition = SHORT;

                        activeTrade = BacktestTrade.builder()
                                .ticker(ticker)
                                .strategyName(strategy.getName())
                                .side(SHORT)
                                .entryDate(currentDate)
                                .entryPrice(priceOpen)
                                .quantity(shares)
                                .holdingBars(0)
                                .build();
                    }

                    case EXIT -> {
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
                    .strategyName(strategy.getName())
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
                    buildRenkoBricks(ticker, marketData, i), // used only for renko based strategies
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
                    .strategyName(strategy.getName())
                    .signalDate(currentDate)
                    .executeDate(executeDate)
                    .action(nextAction.tradeSignal())
                    .fromPosition(currentPosition)
                    .toPosition(nextAction.positionType())
                    .build());

            pendingAction = nextAction;
        }

        // Force close final open trade
        if (activeTrade != null) {
            MarketData lastBar = marketData.getLast();
            closeActiveTrade(activeTrade, lastBar.getMarketDataDate(), lastBar.getPriceClose(), trades);
        }

        BacktestResult result = buildResult(ticker, strategy, equities, trades);

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
        double cagr = (Math.pow(growthFactor, 1.0 / years) - 1) * 100;

        // Win Rate Calculation
        long totalTrades = trades.size();
        long winningTrades = trades.stream()
                .filter(trade -> trade.getPnl() != null &&
                        trade.getPnl().compareTo(BigDecimal.ZERO) > 0)
                .count();
        BigDecimal winRate = BigDecimal.ZERO;
        if (totalTrades > 0) {
            BigDecimal wins = BigDecimal.valueOf(winningTrades);
            BigDecimal total = BigDecimal.valueOf(totalTrades);
            winRate = wins.divide(total, DB_MATH_CONTEXT).multiply(HUNDRED);
        }

        return BacktestResult.builder()
                .ticker(ticker)
                .strategyName(strategy.getName())
                .initialEquity(INITIAL_EQUITY)
                .finalEquity(last.getEquity())
                .startDate(first.getEquityDate())
                .endDate(last.getEquityDate())
                .years(BigDecimal.valueOf(years))
                .cagr(BigDecimal.valueOf(cagr))
                .winRate(winRate)
                .build();
    }

    protected record BacktestRunResult(
            List<BacktestEquity> backtestEquities,
            List<BacktestSignal> backtestSignals,
            List<BacktestTrade> backtestTrades,
            BacktestResult backtestResult
    ) {
    }
}
