package com.alphaflow.core;

import com.alphaflow.domain.enums.PositionType;
import com.alphaflow.domain.enums.TradeAction;
import com.alphaflow.domain.enums.TradeSide;
import com.alphaflow.domain.enums.TradeSignal;
import com.alphaflow.domain.strategy.BacktestStrategy;
import com.alphaflow.domain.strategy.RenkoBacktestStrategy;
import com.alphaflow.infrastructure.config.Constants;
import com.alphaflow.infrastructure.persistence.entities.*;
import com.alphaflow.infrastructure.persistence.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import com.alphaflow.infrastructure.util.RenkoUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BacktestComputer {

    public static final double YEAR_IN_DAYS = 365.25;
    private static final Logger log = LoggerFactory.getLogger(BacktestComputer.class);
    private static final BigDecimal INITIAL_EQUITY = new BigDecimal("100000.00000000");
    private final TickerRepository tickerRepository;
    private final MarketDataRepository marketDataRepository;
    private final MarketStateRepository marketStateRepository;
    private final BacktestEquityDailyRepository equityRepository;
    private final BacktestSignalIntentRepository signalRepository;
    private final BacktestTradeRepository tradeRepository;
    private final BacktestCagrRepository cagrRepository;
    private final List<BacktestStrategy> strategies;
    private final List<RenkoBacktestStrategy> renkoStrategies;
    private final TransactionTemplate transactionTemplate;

    public BacktestComputer(
            TickerRepository tickerRepository,
            MarketDataRepository marketDataRepository,
            MarketStateRepository marketStateRepository,
            BacktestEquityDailyRepository equityRepository,
            BacktestSignalIntentRepository signalRepository,
            BacktestTradeRepository tradeRepository,
            BacktestCagrRepository cagrRepository,
            List<BacktestStrategy> strategies,
            List<RenkoBacktestStrategy> renkoStrategies,
            TransactionTemplate transactionTemplate
    ) {
        this.tickerRepository = tickerRepository;
        this.marketDataRepository = marketDataRepository;
        this.marketStateRepository = marketStateRepository;
        this.equityRepository = equityRepository;
        this.signalRepository = signalRepository;
        this.tradeRepository = tradeRepository;
        this.cagrRepository = cagrRepository;
        this.strategies = strategies;
        this.renkoStrategies = renkoStrategies;
        this.transactionTemplate = transactionTemplate;
    }

    public void compute() {
        log.info("Starting Backtest Computation for all active tickers");

        // Clear all previous results to ensure a clean slate
        transactionTemplate.execute(status -> {
            equityRepository.deleteAllInBatch();
            signalRepository.deleteAllInBatch();
            tradeRepository.deleteAllInBatch();
            cagrRepository.deleteAllInBatch();
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

            for (BacktestStrategy strategy : strategies) {
                log.info("Running strategy: {} for {}", strategy.getName(), ticker.getTickerSymbol());
                BacktestRunResult result = runBacktest(ticker, strategy, marketDataList, indicatorMap);
                // Save in a single transaction
                transactionTemplate.execute(status -> {
                    equityRepository.saveAll(result.equities());
                    signalRepository.saveAll(result.signals());
                    tradeRepository.saveAll(result.trades());
                    cagrRepository.save(result.cagr());
                    return status;
                });
            }

            for (RenkoBacktestStrategy strategy : renkoStrategies) {
                log.info("Running Renko strategy: {} for {}", strategy.getName(), ticker.getTickerSymbol());
                BacktestRunResult result = runRenkoBacktest(ticker, strategy, marketDataList, indicatorMap);
                // Save in a single transaction
                transactionTemplate.execute(status -> {
                    equityRepository.saveAll(result.equities());
                    signalRepository.saveAll(result.signals());
                    tradeRepository.saveAll(result.trades());
                    cagrRepository.save(result.cagr());
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

        TradeSignal pendingSignal = new TradeSignal(TradeAction.NO_SIGNAL, PositionType.NONE);

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
            // 1. Execute pending signal at today's open
            // ─────────────────────────────────────────
            if (pendingSignal.action() != TradeAction.NO_SIGNAL &&
                    pendingSignal.action() != TradeAction.HOLD) {

                BigDecimal totalEquityAtOpen =
                        switch (position) {
                            case SHORT -> currentCash.add(
                                    shares.multiply(entryPrice.subtract(priceOpen))
                            );
                            case LONG -> currentCash.add(shares.multiply(priceOpen));
                            default -> currentCash;
                        };

                switch (pendingSignal.action()) {

                    case ENTER_LONG -> {
                        if (activeTrade != null) {
                            closeActiveTrade(activeTrade, date, priceOpen, completedTrades);
                        }

                        PositionType target = pendingSignal.targetPosition();
                        shares = totalEquityAtOpen
                                .divide(priceOpen, Constants.DB_MATH_CONTEXT);

                        currentCash =
                                totalEquityAtOpen.subtract(shares.multiply(priceOpen));

                        position = target;

                        // Start trade record
                        activeTrade = BacktestTrade.builder()
                                .ticker(ticker)
                                .strategyName(strategy.getName())
                                .side(TradeSide.LONG)
                                .entryDate(date)
                                .entryPrice(priceOpen)
                                .quantity(shares)
                                .holdingBars(0)
                                .build();
                    }

                    case ENTER_SHORT -> {
                        if (activeTrade != null) {
                            closeActiveTrade(activeTrade, date, priceOpen, completedTrades);
                        }

                        PositionType target = pendingSignal.targetPosition();
                        shares = totalEquityAtOpen
                                .divide(priceOpen, Constants.DB_MATH_CONTEXT);

                        currentCash = totalEquityAtOpen;
                        entryPrice = priceOpen;
                        position = target;

                        // Start trade record
                        activeTrade = BacktestTrade.builder()
                                .ticker(ticker)
                                .strategyName(strategy.getName())
                                .side(TradeSide.SHORT)
                                .entryDate(date)
                                .entryPrice(priceOpen)
                                .quantity(shares)
                                .holdingBars(0)
                                .build();
                    }

                    case EXIT -> {
                        if (activeTrade != null) {
                            closeActiveTrade(activeTrade, date, priceOpen, completedTrades);
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
            // 3. Generate next signal
            // ─────────────────────────────
            Map<String, BigDecimal> indicators = indicatorMap.getOrDefault(date, Collections.emptyMap());
            TradeSignal nextSignal = strategy.generateSignal(currentDay, indicators, position);

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
                    .action(nextSignal.action())
                    .fromPosition(position)
                    .toPosition(nextSignal.targetPosition())
                    .build()
            );

            pendingSignal = nextSignal;
        }

        // Force-close any open trade on the final bar
        if (activeTrade != null) {
            MarketData lastDay = marketDataList.getLast();
            BigDecimal lastClose = lastDay.getPriceClose();

            activeTrade.setExitDate(lastDay.getMarketDataDate());
            activeTrade.setExitPrice(lastClose);

            BigDecimal pnl = activeTrade.getSide() == TradeSide.LONG
                    ? activeTrade.getQuantity().multiply(lastClose.subtract(activeTrade.getEntryPrice()))
                    : activeTrade.getQuantity().multiply(activeTrade.getEntryPrice().subtract(lastClose));
            activeTrade.setPnl(pnl);

            BigDecimal pnlPct = activeTrade.getSide() == TradeSide.LONG
                    ? lastClose.subtract(activeTrade.getEntryPrice()).divide(activeTrade.getEntryPrice(), Constants.DB_MATH_CONTEXT)
                    : activeTrade.getEntryPrice().subtract(lastClose).divide(activeTrade.getEntryPrice(), Constants.DB_MATH_CONTEXT);
            activeTrade.setPnlPct(pnlPct.multiply(BigDecimal.valueOf(100)));

            completedTrades.add(activeTrade);
        }

        // Calculate CAGR
        BacktestCagr cagrEntity = null;
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

                cagrEntity = BacktestCagr.builder()
                        .ticker(ticker)
                        .strategyName(strategy.getName())
                        .initialEquity(INITIAL_EQUITY)
                        .finalEquity(last.getEquity())
                        .startDate(startDate)
                        .endDate(endDate)
                        .years(new BigDecimal(years, Constants.DB_MATH_CONTEXT))
                        .cagr(new BigDecimal(cagrValue, Constants.DB_MATH_CONTEXT))
                        .build();
            }
        }

        return new BacktestRunResult(equities, signals, completedTrades, cagrEntity);
    }

    private BacktestRunResult runRenkoBacktest(
            Ticker ticker,
            RenkoBacktestStrategy strategy,
            List<MarketData> marketDataList,
            Map<LocalDate, Map<String, BigDecimal>> indicatorMap
    ) {

        BigDecimal currentCash = INITIAL_EQUITY;
        PositionType position = PositionType.NONE;
        BigDecimal shares = BigDecimal.ZERO;
        BigDecimal entryPrice = BigDecimal.ZERO; // for shorts

        TradeSignal pendingSignal = new TradeSignal(TradeAction.NO_SIGNAL, PositionType.NONE);

        List<BacktestEquity> equities = new ArrayList<>();
        List<BacktestSignal> signals = new ArrayList<>();
        List<BacktestTrade> completedTrades = new ArrayList<>();

        BacktestTrade activeTrade = null;
        Map<String, Object> strategyState = new HashMap<>();
        Function<MarketData, BigDecimal> priceExtractor = getPriceExtractor(strategy.getPriceSource());

        for (int i = 0; i < marketDataList.size(); i++) {
            MarketData currentDay = marketDataList.get(i);
            LocalDate date = currentDay.getMarketDataDate();
            BigDecimal priceOpen = currentDay.getPriceOpen();
            BigDecimal priceClose = currentDay.getPriceClose();

            // ─────────────────────────────────────────
            // 1. Execute pending signal at today's open
            // ─────────────────────────────────────────
            if (pendingSignal.action() != TradeAction.NO_SIGNAL &&
                    pendingSignal.action() != TradeAction.HOLD) {

                BigDecimal totalEquityAtOpen =
                        switch (position) {
                            case SHORT -> currentCash.add(
                                    shares.multiply(entryPrice.subtract(priceOpen))
                            );
                            case LONG -> currentCash.add(shares.multiply(priceOpen));
                            default -> currentCash;
                        };

                switch (pendingSignal.action()) {

                    case ENTER_LONG -> {
                        if (activeTrade != null) {
                            closeActiveTrade(activeTrade, date, priceOpen, completedTrades);
                        }

                        PositionType target = pendingSignal.targetPosition();
                        shares = totalEquityAtOpen
                                .divide(priceOpen, Constants.DB_MATH_CONTEXT);

                        currentCash =
                                totalEquityAtOpen.subtract(shares.multiply(priceOpen));

                        position = target;

                        // Start trade record
                        activeTrade = BacktestTrade.builder()
                                .ticker(ticker)
                                .strategyName(strategy.getName())
                                .side(TradeSide.LONG)
                                .entryDate(date)
                                .entryPrice(priceOpen)
                                .quantity(shares)
                                .holdingBars(0)
                                .build();
                    }

                    case ENTER_SHORT -> {
                        if (activeTrade != null) {
                            closeActiveTrade(activeTrade, date, priceOpen, completedTrades);
                        }

                        PositionType target = pendingSignal.targetPosition();
                        shares = totalEquityAtOpen
                                .divide(priceOpen, Constants.DB_MATH_CONTEXT);

                        currentCash = totalEquityAtOpen;
                        entryPrice = priceOpen;
                        position = target;

                        // Start trade record
                        activeTrade = BacktestTrade.builder()
                                .ticker(ticker)
                                .strategyName(strategy.getName())
                                .side(TradeSide.SHORT)
                                .entryDate(date)
                                .entryPrice(priceOpen)
                                .quantity(shares)
                                .holdingBars(0)
                                .build();
                    }

                    case EXIT -> {
                        if (activeTrade != null) {
                            closeActiveTrade(activeTrade, date, priceOpen, completedTrades);
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
            // 3. Generate next signal
            // ─────────────────────────────
            Map<String, BigDecimal> indicators = indicatorMap.getOrDefault(date, Collections.emptyMap());

            // Renko regeneration logic
            List<MarketData> subList = marketDataList.subList(0, i + 1);
            List<RenkoData> renkoBricks = RenkoUtil.generateRenkoBricks(ticker, subList, priceExtractor, strategy.getPriceSource());

            TradeSignal nextSignal = strategy.generateSignal(renkoBricks, currentDay, indicators, position, strategyState);

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
                    .action(nextSignal.action())
                    .fromPosition(position)
                    .toPosition(nextSignal.targetPosition())
                    .build()
            );

            pendingSignal = nextSignal;
        }

        // Force-close any open trade on the final bar
        if (activeTrade != null) {
            MarketData lastDay = marketDataList.getLast();
            BigDecimal lastClose = lastDay.getPriceClose();

            activeTrade.setExitDate(lastDay.getMarketDataDate());
            activeTrade.setExitPrice(lastClose);

            BigDecimal pnl = activeTrade.getSide() == TradeSide.LONG
                    ? activeTrade.getQuantity().multiply(lastClose.subtract(activeTrade.getEntryPrice()))
                    : activeTrade.getQuantity().multiply(activeTrade.getEntryPrice().subtract(lastClose));
            activeTrade.setPnl(pnl);

            BigDecimal pnlPct = activeTrade.getSide() == TradeSide.LONG
                    ? lastClose.subtract(activeTrade.getEntryPrice()).divide(activeTrade.getEntryPrice(), Constants.DB_MATH_CONTEXT)
                    : activeTrade.getEntryPrice().subtract(lastClose).divide(activeTrade.getEntryPrice(), Constants.DB_MATH_CONTEXT);
            activeTrade.setPnlPct(pnlPct.multiply(BigDecimal.valueOf(100)));

            completedTrades.add(activeTrade);
        }

        // Calculate CAGR
        BacktestCagr cagrEntity = null;
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

                cagrEntity = BacktestCagr.builder()
                        .ticker(ticker)
                        .strategyName(strategy.getName())
                        .initialEquity(INITIAL_EQUITY)
                        .finalEquity(last.getEquity())
                        .startDate(startDate)
                        .endDate(endDate)
                        .years(new BigDecimal(years, Constants.DB_MATH_CONTEXT))
                        .cagr(new BigDecimal(cagrValue, Constants.DB_MATH_CONTEXT))
                        .build();
            }
        }

        return new BacktestRunResult(equities, signals, completedTrades, cagrEntity);
    }

    private void closeActiveTrade(BacktestTrade activeTrade, LocalDate exitDate, BigDecimal exitPrice, List<BacktestTrade> completedTrades) {
        activeTrade.setExitDate(exitDate);
        activeTrade.setExitPrice(exitPrice);
        BigDecimal pnl = activeTrade.getSide() == TradeSide.LONG
                ? activeTrade.getQuantity().multiply(exitPrice.subtract(activeTrade.getEntryPrice()))
                : activeTrade.getQuantity().multiply(activeTrade.getEntryPrice().subtract(exitPrice));
        activeTrade.setPnl(pnl);

        BigDecimal pnlPct = activeTrade.getSide() == TradeSide.LONG
                ? exitPrice.subtract(activeTrade.getEntryPrice()).divide(activeTrade.getEntryPrice(), Constants.DB_MATH_CONTEXT)
                : activeTrade.getEntryPrice().subtract(exitPrice).divide(activeTrade.getEntryPrice(), Constants.DB_MATH_CONTEXT);
        activeTrade.setPnlPct(pnlPct.multiply(BigDecimal.valueOf(100)));

        completedTrades.add(activeTrade);
    }

    private Function<MarketData, BigDecimal> getPriceExtractor(String source) {
        if ("vwap".equalsIgnoreCase(source)) return MarketData::getVwap;
        if ("price_close".equalsIgnoreCase(source)) return MarketData::getPriceClose;
        return MarketData::getVwap;
    }

    private record BacktestRunResult(
            List<BacktestEquity> equities,
            List<BacktestSignal> signals,
            List<BacktestTrade> trades,
            BacktestCagr cagr
    ) {
    }

}
