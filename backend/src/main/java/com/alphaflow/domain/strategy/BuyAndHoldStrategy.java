package com.alphaflow.domain.strategy;

import com.alphaflow.domain.enums.BacktestSignal;
import com.alphaflow.domain.enums.PositionType;
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
    public BacktestSignal generateSignal(MarketData marketData, Map<String, BigDecimal> indicators, PositionType currentPosition) {
        if (currentPosition == PositionType.NONE) {
            return BacktestSignal.GO_LONG_100;
        }
        return BacktestSignal.HOLD;
    }
}
