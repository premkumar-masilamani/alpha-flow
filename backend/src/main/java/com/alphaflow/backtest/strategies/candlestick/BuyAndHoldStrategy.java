package com.alphaflow.backtest.strategies.candlestick;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.strategies.StrategyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

public class BuyAndHoldStrategy implements CandlestickStrategy {

    private static final Logger log = LoggerFactory.getLogger(BuyAndHoldStrategy.class);

    private BacktestStrategy entity;

    public BuyAndHoldStrategy() {
    }

    public BuyAndHoldStrategy(BacktestStrategy entity) {
        this.entity = entity;
    }

    @Override
    public String getName() {
        return entity != null ? entity.getName() : "Buy & Hold";
    }

    @Override
    public BacktestStrategy getEntity() {
        return entity;
    }

    @Override
    public TradeAction generateSignal(StrategyContext context) {

        if (context.currentPosition() == PositionType.NONE) {
            log.debug("Strategy {} generating initial ENTER_LONG signal", getName());
            return new TradeAction(TradeSignal.ENTER_LONG, PositionType.LONG);
        }

        return new TradeAction(TradeSignal.HOLD, context.currentPosition());
    }
}
