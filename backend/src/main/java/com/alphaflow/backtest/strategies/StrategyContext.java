package com.alphaflow.backtest.strategies;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.infrastructure.entities.CandleBar;
import com.alphaflow.infrastructure.entities.RenkoBrick;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record StrategyContext(
        CandleBar candleBar,
        Map<String, BigDecimal> indicators,
        PositionType currentPosition,
        List<RenkoBrick> renkoBricks,
        Map<String, Object> state
) {
}
