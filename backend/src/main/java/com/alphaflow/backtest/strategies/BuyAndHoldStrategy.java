package com.alphaflow.backtest.strategies;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.infrastructure.entities.MarketData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class BuyAndHoldStrategy implements BacktestStrategy {

    @Override
    public String getName() {
        return "Buy & Hold";
    }

    @Override
    public TradeAction generateSignal(MarketData currentDayMarketData, Map<String, BigDecimal> currentDayIndicators, PositionType currentDayPosition) {
        // Enter once, at full size
        if (currentDayPosition == PositionType.NONE) {
            return new TradeAction(TradeSignal.ENTER_LONG, PositionType.LONG);
        }
        // Hold forever
        return new TradeAction(TradeSignal.HOLD, currentDayPosition);
    }

}
