package com.alphaflow.domain.strategy;

import com.alphaflow.domain.enums.PositionType;
import com.alphaflow.domain.enums.TradeAction;
import com.alphaflow.domain.enums.TradeSignal;
import com.alphaflow.infrastructure.persistence.entities.MarketData;
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
    public TradeSignal generateSignal(MarketData currentDayMarketData, Map<String, BigDecimal> currentDayIndicators, PositionType currentDayPosition) {
        // Enter once, at full size
        if (currentDayPosition == PositionType.NONE) {
            return new TradeSignal(TradeAction.ENTER_LONG, PositionType.LONG_100);
        }
        // Hold forever
        return new TradeSignal(TradeAction.HOLD, currentDayPosition);
    }

}
