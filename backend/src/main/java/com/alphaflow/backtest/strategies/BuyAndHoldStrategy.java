package com.alphaflow.backtest.strategies;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import org.springframework.stereotype.Component;

@Component
public class BuyAndHoldStrategy implements CandlestickStrategy {

    @Override
    public String getName() {
        return "Buy & Hold";
    }

    @Override
    public TradeAction generateSignal(StrategyContext context) {

        if (context.currentPosition() == PositionType.NONE) {
            return new TradeAction(TradeSignal.ENTER_LONG, PositionType.LONG);
        }

        return new TradeAction(TradeSignal.HOLD, context.currentPosition());
    }
}
