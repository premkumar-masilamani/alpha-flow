package com.alphaflow.backtest.strategies;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.infrastructure.entities.CandleData;
import com.alphaflow.infrastructure.entities.RenkoData;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record StrategyContext(
        CandleData candleBar,
        Map<String, BigDecimal> indicators,
        PositionType currentPosition,
        List<RenkoData> renkoBricks,
        Map<String, Object> state
) {
}
