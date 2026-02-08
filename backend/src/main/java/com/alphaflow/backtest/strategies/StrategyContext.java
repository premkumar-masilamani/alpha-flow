package com.alphaflow.backtest.strategies;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.infrastructure.entities.Candle;
import com.alphaflow.infrastructure.entities.Renko;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record StrategyContext(
        Candle marketData,
        Map<String, BigDecimal> indicators,
        PositionType currentPosition,
        List<Renko> renkoBricks,
        Map<String, Object> state
) {
}
