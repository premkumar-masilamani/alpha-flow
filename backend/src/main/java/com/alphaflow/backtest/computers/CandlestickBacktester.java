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
import com.alphaflow.backtest.strategies.BacktestStrategy;
import com.alphaflow.infrastructure.entities.MarketData;
import com.alphaflow.infrastructure.entities.MarketState;
import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.repositories.MarketDataRepository;
import com.alphaflow.infrastructure.repositories.MarketStateRepository;
import com.alphaflow.infrastructure.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.alphaflow.infrastructure.constants.AppConstants.DB_MATH_CONTEXT;

@Service
public class CandlestickBacktester {

    public static final double YEAR_IN_DAYS = 365.25;
    private static final Logger log = LoggerFactory.getLogger(CandlestickBacktester.class);
    private static final BigDecimal INITIAL_EQUITY = new BigDecimal("100000.00000000");

    private final TickerRepository tickerRepository;
    private final MarketDataRepository marketDataRepository;
    private final MarketStateRepository marketStateRepository;
    private final BacktestEquityRepository backtestEquityRepository;
    private final BacktestSignalRepository backtestSignalRepository;
    private final BacktestTradeRepository backtestTradeRepository;
    private final BacktestResultRepository backtestResultRepository;
    private final List<BacktestStrategy> backtestStrategies;
    private final TransactionTemplate transactionTemplate;

    public CandlestickBacktester(
            TickerRepository tickerRepository,
            MarketDataRepository marketDataRepository,
            MarketStateRepository marketStateRepository,
            BacktestEquityRepository backtestEquityRepository,
            BacktestSignalRepository backtestSignalRepository,
            BacktestTradeRepository backtestTradeRepository,
            BacktestResultRepository backtestResultRepository,
            List<BacktestStrategy> backtestStrategies,
            TransactionTemplate transactionTemplate
    ) {
        this.tickerRepository = tickerRepository;
        this.marketDataRepository = marketDataRepository;
        this.marketStateRepository = marketStateRepository;
        this.backtestEquityRepository = backtestEquityRepository;
        this.backtestSignalRepository = backtestSignalRepository;
        this.backtestTradeRepository = backtestTradeRepository;
        this.backtestResultRepository = backtestResultRepository;
        this.backtestStrategies = backtestStrategies;
        this.transactionTemplate = transactionTemplate;
    }

    public void compute() {
        log.info("Starting Backtest Computation for all active tickers");

        // Clear all previous results to ensure a clean slate
        transactionTemplate.execute(status -> {
            backtestEquityRepository.deleteAllInBatch();
            backtestSignalRepository.deleteAllInBatch();
            backtestTradeRepository.deleteAllInBatch();
            backtestResultRepository.deleteAllInBatch();
            return status;
        });

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

            for (BacktestStrategy strategy : backtestStrategies) {
                log.info("Running strategy: {} for {}", strategy.getName(), ticker.getTickerSymbol());
                BacktestRunResult result = runBacktest(ticker, strategy, marketDataList, indicatorMap);
                // Save in a single transaction
                transactionTemplate.execute(status -> {
                    backtestEquityRepository.saveAll(result.backtestEquities());
                    backtestSignalRepository.saveAll(result.backtestSignals());
                    backtestTradeRepository.saveAll(result.backtestTrades());
                    backtestResultRepository.save(result.backtestResult());
                    return status;
                });
            }
        });

        log.info("Backtest Computation completed.");
    }

    private BacktestRunResult runBacktest(
            Ticker ticker,
            BacktestStrategy strategy,
            List<MarketData> marketDataList,
            Map<LocalDate, Map<String, BigDecimal>> indicatorMap
    ) {

        BigDecimal currentCash = INITIAL_EQUITY;
        PositionType position = PositionType.NONE;
        BigDecimal shares = BigDecimal.ZERO;
        BigDecimal entryPrice = BigDecimal.ZERO; // for shorts

        TradeAction pendingAction = new TradeAction(TradeSignal.NO_SIGNAL, PositionType.NONE);

        List<BacktestEquity> equities = new ArrayList<>();
        List<BacktestSignal> signals = new ArrayList<>();
        List<BacktestTrade> completedTrades = new ArrayList<>();

        BacktestTrade activeTrade = null;

        for (int i = 0; i < marketDataList.size(); i++) {
            MarketData currentDay = marketDataList.get(i);
            LocalDate date = currentDay.getMarketDataDate();
            BigDecimal priceOpen = currentDay.getPriceOpen();
            BigDecimal priceClose = currentDay.getPriceClose();

            // ─────────────────────────────────────────
            // 1. Execute pending tradeSignal at today's open
            // ─────────────────────────────────────────
            if (pendingAction.tradeSignal() != TradeSignal.NO_SIGNAL &&
                    pendingAction.tradeSignal() != TradeSignal.HOLD) {

                BigDecimal totalEquityAtOpen =
                        switch (position) {
                            case SHORT -> currentCash.add(
                                    shares.multiply(entryPrice.subtract(priceOpen))
                            );
                            case LONG -> currentCash.add(shares.multiply(priceOpen));
                            default -> currentCash;
                        };

                switch (pendingAction.tradeSignal()) {

                    case ENTER_LONG -> {
                        PositionType target = pendingAction.positionType();
                        shares = totalEquityAtOpen
                                .divide(priceOpen, DB_MATH_CONTEXT);

                        currentCash =
                                totalEquityAtOpen.subtract(shares.multiply(priceOpen));

                        position = target;

                        // Start trade record
                        activeTrade = BacktestTrade.builder()
                                .ticker(ticker)
                                .strategyName(strategy.getName())
                                .side(PositionType.LONG)
                                .entryDate(date)
                                .entryPrice(priceOpen)
                                .quantity(shares)
                                .holdingBars(0)
                                .build();
                    }

                    case ENTER_SHORT -> {
                        PositionType target = pendingAction.positionType();
                        shares = totalEquityAtOpen
                                .divide(priceOpen, DB_MATH_CONTEXT);

                        currentCash = totalEquityAtOpen;
                        entryPrice = priceOpen;
                        position = target;

                        // Start trade record
                        activeTrade = BacktestTrade.builder()
                                .ticker(ticker)
                                .strategyName(strategy.getName())
                                .side(PositionType.SHORT)
                                .entryDate(date)
                                .entryPrice(priceOpen)
                                .quantity(shares)
                                .holdingBars(0)
                                .build();
                    }

                    case EXIT -> {
                        if (activeTrade != null) {
                            activeTrade.setExitDate(date);
                            activeTrade.setExitPrice(priceOpen);
                            BigDecimal pnl = activeTrade.getSide() == PositionType.LONG
                                    ? activeTrade.getQuantity().multiply(priceOpen.subtract(activeTrade.getEntryPrice()))
                                    : activeTrade.getQuantity().multiply(activeTrade.getEntryPrice().subtract(priceOpen));
                            activeTrade.setPnl(pnl);

                            BigDecimal pnlPct = activeTrade.getSide() == PositionType.LONG
                                    ? priceOpen.subtract(activeTrade.getEntryPrice()).divide(activeTrade.getEntryPrice(), DB_MATH_CONTEXT)
                                    : activeTrade.getEntryPrice().subtract(priceOpen).divide(activeTrade.getEntryPrice(), DB_MATH_CONTEXT);
                            activeTrade.setPnlPct(pnlPct.multiply(BigDecimal.valueOf(100)));

                            completedTrades.add(activeTrade);
                            activeTrade = null;
                        }

                        currentCash = totalEquityAtOpen;
                        shares = BigDecimal.ZERO;
                        position = PositionType.NONE;
                    }

                    default -> {
                        // HOLD / NO_SIGNAL → no-op
                    }
                }
            }

            if (activeTrade != null) {
                activeTrade.setHoldingBars(activeTrade.getHoldingBars() + 1);
            }

            // ─────────────────────────────
            // 2. Equity at today's close
            // ─────────────────────────────
            BigDecimal dailyEquity =
                    switch (position) {
                        case LONG -> currentCash.add(shares.multiply(priceClose));

                        case SHORT -> currentCash.add(
                                shares.multiply(entryPrice.subtract(priceClose))
                        );

                        default -> currentCash;
                    };

            // ─────────────────────────────
            // 3. Generate next tradeSignal
            // ─────────────────────────────
            Map<String, BigDecimal> indicators = indicatorMap.getOrDefault(date, Collections.emptyMap());
            TradeAction nextSignal = strategy.generateSignal(currentDay, indicators, position);

            // ─────────────────────────────
            // 4. Record Daily Equity
            // ─────────────────────────────
            equities.add(BacktestEquity.builder()
                    .ticker(ticker)
                    .strategyName(strategy.getName())
                    .equityDate(date)
                    .equity(dailyEquity)
                    .position(position)
                    .priceClose(priceClose)
                    .build()
            );

            // ─────────────────────────────
            // 5. Record Signal Intent
            // ─────────────────────────────
            LocalDate nextDate = (i + 1 < marketDataList.size()) ? marketDataList.get(i + 1).getMarketDataDate() : null;

            signals.add(BacktestSignal.builder()
                    .ticker(ticker)
                    .strategyName(strategy.getName())
                    .signalDate(date)
                    .executeDate(nextDate)
                    .action(nextSignal.tradeSignal())
                    .fromPosition(position)
                    .toPosition(nextSignal.positionType())
                    .build()
            );

            pendingAction = nextSignal;
        }

        // Force-close any open trade on the final bar
        if (activeTrade != null) {
            MarketData lastDay = marketDataList.getLast();
            BigDecimal lastClose = lastDay.getPriceClose();

            activeTrade.setExitDate(lastDay.getMarketDataDate());
            activeTrade.setExitPrice(lastClose);

            BigDecimal pnl = activeTrade.getSide() == PositionType.LONG
                    ? activeTrade.getQuantity().multiply(lastClose.subtract(activeTrade.getEntryPrice()))
                    : activeTrade.getQuantity().multiply(activeTrade.getEntryPrice().subtract(lastClose));
            activeTrade.setPnl(pnl);

            BigDecimal pnlPct = activeTrade.getSide() == PositionType.LONG
                    ? lastClose.subtract(activeTrade.getEntryPrice()).divide(activeTrade.getEntryPrice(), DB_MATH_CONTEXT)
                    : activeTrade.getEntryPrice().subtract(lastClose).divide(activeTrade.getEntryPrice(), DB_MATH_CONTEXT);
            activeTrade.setPnlPct(pnlPct.multiply(BigDecimal.valueOf(100)));

            completedTrades.add(activeTrade);
        }

        // Calculate Backtest Results
        BacktestResult resultEntity = null;
        if (!equities.isEmpty()) {
            BacktestEquity first = equities.getFirst();
            BacktestEquity last = equities.getLast();

            LocalDate startDate = first.getEquityDate();
            LocalDate endDate = last.getEquityDate();

            long days = ChronoUnit.DAYS.between(startDate, endDate);
            if (days > 0) {
                double years = days / YEAR_IN_DAYS;
                double initialValue = INITIAL_EQUITY.doubleValue();
                double finalValue = last.getEquity().doubleValue();

                double cagrValue = (Math.pow(finalValue / initialValue, 1.0 / years) - 1.0) * 100.0;

                long totalTrades = completedTrades.size();
                long successfulTrades = completedTrades.stream()
                        .filter(t -> t.getPnl().compareTo(BigDecimal.ZERO) > 0)
                        .count();
                BigDecimal winRate = totalTrades > 0
                        ? BigDecimal.valueOf(successfulTrades)
                        .divide(BigDecimal.valueOf(totalTrades), DB_MATH_CONTEXT)
                        .multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;

                resultEntity = BacktestResult.builder()
                        .ticker(ticker)
                        .strategyName(strategy.getName())
                        .initialEquity(INITIAL_EQUITY)
                        .finalEquity(last.getEquity())
                        .startDate(startDate)
                        .endDate(endDate)
                        .years(new BigDecimal(years, DB_MATH_CONTEXT))
                        .cagr(new BigDecimal(cagrValue, DB_MATH_CONTEXT))
                        .winRate(winRate)
                        .build();
            }
        }

        return new BacktestRunResult(equities, signals, completedTrades, resultEntity);
    }

    private record BacktestRunResult(
            List<BacktestEquity> backtestEquities,
            List<BacktestSignal> backtestSignals,
            List<BacktestTrade> backtestTrades,
            BacktestResult backtestResult
    ) {
    }

}
