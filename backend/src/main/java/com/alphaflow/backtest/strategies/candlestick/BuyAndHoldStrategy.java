package com.alphaflow.backtest.strategies.candlestick;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.strategies.StrategyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class BuyAndHoldStrategy implements CandlestickStrategy {

    private static final Logger log = LoggerFactory.getLogger(BuyAndHoldStrategy.class);

    @Override
    public String getName() {
        return "Buy & Hold";
    }

    @Override
    public BacktestStrategy getEntity() {
        return BacktestStrategy.builder()
                .name(getName())
                .strategyType("CANDLESTICK_B&H")
                .indicators(Collections.emptyList())
                .build();
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
