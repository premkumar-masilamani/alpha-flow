package com.alphaflow.backtest.strategies;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.indicators.IndicatorKey;
import com.alphaflow.infrastructure.entities.MarketData;
import com.alphaflow.infrastructure.entities.RenkoData;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record StrategyContext<S extends StrategyState>(
        MarketData marketData,
        Map<IndicatorKey, BigDecimal> indicators,
        PositionType currentPosition,
        List<RenkoData> renkoBricks,
        S state
) {
}
