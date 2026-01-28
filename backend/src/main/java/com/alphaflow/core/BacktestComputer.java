package com.alphaflow.core;

import com.alphaflow.domain.enums.*;
import com.alphaflow.domain.strategy.BacktestStrategy;
import com.alphaflow.infrastructure.persistence.entities.*;
import com.alphaflow.infrastructure.persistence.repositories.BacktestEquityDailyRepository;
import com.alphaflow.infrastructure.persistence.repositories.BacktestSignalIntentRepository;
import com.alphaflow.infrastructure.persistence.repositories.BacktestTradeRepository;
import com.alphaflow.infrastructure.persistence.repositories.MarketDataRepository;
import com.alphaflow.infrastructure.persistence.repositories.MarketStateRepository;
import com.alphaflow.infrastructure.persistence.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alphaflow.infrastructure.config.Constants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BacktestComputer {

    private static final Logger log = LoggerFactory.getLogger(BacktestComputer.class);
    private static final BigDecimal INITIAL_EQUITY = new BigDecimal("100000.00000000");
    private static final int SCALE = Constants.SCALE;
    private static final java.math.RoundingMode ROUNDING_MODE = Constants.ROUNDING_MODE;

    private final TickerRepository tickerRepository;
    private final MarketDataRepository marketDataRepository;
    private final MarketStateRepository marketStateRepository;
    private final BacktestEquityDailyRepository equityRepository;
    private final BacktestSignalIntentRepository signalRepository;
    private final BacktestTradeRepository tradeRepository;
    private final List<BacktestStrategy> strategies;
    private final TransactionTemplate transactionTemplate;

    public BacktestComputer(
            TickerRepository tickerRepository,
            MarketDataRepository marketDataRepository,
            MarketStateRepository marketStateRepository,
            BacktestEquityDailyRepository equityRepository,
            BacktestSignalIntentRepository signalRepository,
            BacktestTradeRepository tradeRepository,
            List<BacktestStrategy> strategies,
            TransactionTemplate transactionTemplate
    ) {
        this.tickerRepository = tickerRepository;
        this.marketDataRepository = marketDataRepository;
        this.marketStateRepository = marketStateRepository;
        this.equityRepository = equityRepository;
        this.signalRepository = signalRepository;
        this.tradeRepository = tradeRepository;
        this.strategies = strategies;
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

            for (BacktestStrategy strategy : strategies) {
                log.info("Running strategy: {} for {}", strategy.getName(), ticker.getTickerSymbol());
                BacktestRunResult result = runBacktest(ticker, strategy, marketDataList, indicatorMap);
                // Delete and Save in a single transaction
                transactionTemplate.execute(status -> {
                    equityRepository.deleteByTickerIdAndStrategyName(ticker.getTickerId(), strategy.getName());
                    signalRepository.deleteByTickerIdAndStrategyName(ticker.getTickerId(), strategy.getName());
                    tradeRepository.deleteByTickerIdAndStrategyName(ticker.getTickerId(), strategy.getName());

                    equityRepository.saveAll(result.equities());
                    signalRepository.saveAll(result.signals());
                    tradeRepository.saveAll(result.trades());
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

        TradeSignal pendingSignal =
                new TradeSignal(TradeAction.NO_SIGNAL, PositionType.NONE);

        List<BacktestEquityDaily> equities = new ArrayList<>();
        List<BacktestSignalIntent> signals = new ArrayList<>();
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
                        PositionType target = pendingSignal.targetPosition();
                        shares = totalEquityAtOpen
                                .divide(priceOpen, SCALE, ROUNDING_MODE);

                        currentCash =
                                totalEquityAtOpen.subtract(shares.multiply(priceOpen));

                        position = target;

                        // Start trade record
                        activeTrade = BacktestTrade.builder()
                                .tradeId(UUID.randomUUID())
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
                        PositionType target = pendingSignal.targetPosition();
                        shares = totalEquityAtOpen
                                .divide(priceOpen, SCALE, ROUNDING_MODE);

                        currentCash = totalEquityAtOpen;
                        entryPrice = priceOpen;
                        position = target;

                        // Start trade record
                        activeTrade = BacktestTrade.builder()
                                .tradeId(UUID.randomUUID())
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
                            activeTrade.setExitDate(date);
                            activeTrade.setExitPrice(priceOpen);
                            BigDecimal pnl = activeTrade.getSide() == TradeSide.LONG
                                    ? activeTrade.getQuantity().multiply(priceOpen.subtract(activeTrade.getEntryPrice()))
                                    : activeTrade.getQuantity().multiply(activeTrade.getEntryPrice().subtract(priceOpen));
                            activeTrade.setPnl(pnl.setScale(SCALE, ROUNDING_MODE));

                            BigDecimal pnlPct = activeTrade.getSide() == TradeSide.LONG
                                    ? priceOpen.subtract(activeTrade.getEntryPrice()).divide(activeTrade.getEntryPrice(), SCALE, ROUNDING_MODE)
                                    : activeTrade.getEntryPrice().subtract(priceOpen).divide(activeTrade.getEntryPrice(), SCALE, ROUNDING_MODE);
                            activeTrade.setPnlPct(pnlPct);

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
            // 3. Generate next signal
            // ─────────────────────────────
            Map<String, BigDecimal> indicators =
                    indicatorMap.getOrDefault(date, Collections.emptyMap());

            TradeSignal nextSignal =
                    strategy.generateSignal(currentDay, indicators, position);

            // ─────────────────────────────
            // 4. Record Daily Equity
            // ─────────────────────────────
            equities.add(
                    BacktestEquityDaily.builder()
                            .ticker(ticker)
                            .strategyName(strategy.getName())
                            .date(date)
                            .equity(dailyEquity.setScale(SCALE, ROUNDING_MODE))
                            .position(position)
                            .priceClose(priceClose)
                            .build()
            );

            // ─────────────────────────────
            // 5. Record Signal Intent
            // ─────────────────────────────
            LocalDate nextDate = (i + 1 < marketDataList.size()) ? marketDataList.get(i + 1).getMarketDataDate() : null;

            signals.add(
                    BacktestSignalIntent.builder()
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
            activeTrade.setPnl(pnl.setScale(SCALE, ROUNDING_MODE));

            BigDecimal pnlPct = activeTrade.getSide() == TradeSide.LONG
                    ? lastClose.subtract(activeTrade.getEntryPrice()).divide(activeTrade.getEntryPrice(), SCALE, ROUNDING_MODE)
                    : activeTrade.getEntryPrice().subtract(lastClose).divide(activeTrade.getEntryPrice(), SCALE, ROUNDING_MODE);
            activeTrade.setPnlPct(pnlPct);

            completedTrades.add(activeTrade);
        }

        return new BacktestRunResult(equities, signals, completedTrades);
    }

    private record BacktestRunResult(
            List<BacktestEquityDaily> equities,
            List<BacktestSignalIntent> signals,
            List<BacktestTrade> trades
    ) {}

}
