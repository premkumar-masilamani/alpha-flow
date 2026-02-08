package com.alphaflow.backtest.engine;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.entities.BacktestTrade;
import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.infrastructure.entities.Ticker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TradeExecutor {

    private final Portfolio portfolio;
    private final Ticker ticker;
    private final BacktestStrategy strategy;

    private BacktestTrade activeTrade;

    public TradeExecutor(Portfolio portfolio, Ticker ticker, BacktestStrategy strategy) {
        this.portfolio = portfolio;
        this.ticker = ticker;
        this.strategy = strategy;
    }

    public void execute(TradeAction action, LocalDate currentDate, BigDecimal priceOpen, List<BacktestTrade> trades) {
        if (action == null || action.tradeSignal() == TradeSignal.NO_SIGNAL || action.tradeSignal() == TradeSignal.HOLD) {
            return;
        }

        BigDecimal equityAtOpen = portfolio.equityAtPrice(priceOpen);

        switch (action.tradeSignal()) {
            case ENTER_LONG -> enterLong(currentDate, priceOpen, equityAtOpen, trades);
            case ENTER_SHORT -> enterShort(currentDate, priceOpen, equityAtOpen, trades);
            case EXIT -> exit(currentDate, priceOpen, equityAtOpen, trades);
            default -> {
            }
        }
    }

    public void incrementHoldingBars() {
        if (activeTrade != null) {
            activeTrade.setHoldingBars(activeTrade.getHoldingBars() + 1);
        }
    }

    public boolean hasActiveTrade() {
        return activeTrade != null;
    }

    public void closeOpenTrade(LocalDate date, BigDecimal price, List<BacktestTrade> trades) {
        if (activeTrade == null) {
            return;
        }
        closeActiveTrade(date, price, trades);
        portfolio.exitToCash(portfolio.equityAtPrice(price));
        activeTrade = null;
    }

    private void enterLong(LocalDate date, BigDecimal priceOpen, BigDecimal equityAtOpen, List<BacktestTrade> trades) {
        if (portfolio.getPositionType() == PositionType.LONG) {
            throw new IllegalStateException("Duplicate ENTER_LONG signal while already long");
        }
        if (activeTrade != null) {
            closeActiveTrade(date, priceOpen, trades);
        }
        portfolio.enterLong(priceOpen, equityAtOpen);
        activeTrade = BacktestTrade.builder()
                .ticker(ticker)
                .strategy(strategy)
                .side(PositionType.LONG)
                .entryDate(date)
                .entryPrice(priceOpen)
                .quantity(portfolio.getQuantity())
                .holdingBars(0)
                .build();
    }

    private void enterShort(LocalDate date, BigDecimal priceOpen, BigDecimal equityAtOpen, List<BacktestTrade> trades) {
        if (portfolio.getPositionType() == PositionType.SHORT) {
            throw new IllegalStateException("Duplicate ENTER_SHORT signal while already short");
        }
        if (activeTrade != null) {
            closeActiveTrade(date, priceOpen, trades);
        }
        portfolio.enterShort(priceOpen, equityAtOpen);
        activeTrade = BacktestTrade.builder()
                .ticker(ticker)
                .strategy(strategy)
                .side(PositionType.SHORT)
                .entryDate(date)
                .entryPrice(priceOpen)
                .quantity(portfolio.getQuantity())
                .holdingBars(0)
                .build();
    }

    private void exit(LocalDate date, BigDecimal priceOpen, BigDecimal equityAtOpen, List<BacktestTrade> trades) {
        if (activeTrade == null || portfolio.getPositionType() == PositionType.NONE) {
            throw new IllegalStateException("EXIT signal received without an open position");
        }
        closeActiveTrade(date, priceOpen, trades);
        portfolio.exitToCash(equityAtOpen);
        activeTrade = null;
    }

    private void closeActiveTrade(LocalDate date, BigDecimal priceOpen, List<BacktestTrade> trades) {
        Portfolio.TradePnl pnl = portfolio.calculateTradePnl(
                activeTrade.getSide(),
                activeTrade.getEntryPrice(),
                priceOpen,
                activeTrade.getQuantity()
        );

        activeTrade.setExitDate(date);
        activeTrade.setExitPrice(priceOpen);
        activeTrade.setPnl(pnl.pnl());
        activeTrade.setPnlPct(pnl.pnlPct());

        trades.add(activeTrade);
    }
}
