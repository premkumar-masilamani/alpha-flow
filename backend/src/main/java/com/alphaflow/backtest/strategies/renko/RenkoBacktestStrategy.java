package com.alphaflow.backtest.strategies.renko;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.infrastructure.entities.MarketData;
import com.alphaflow.infrastructure.entities.RenkoData;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface RenkoBacktestStrategy {

    String getName();

    /**
     * Generates a signal based on current Renko brick series and other market data.
     *
     * @param renkoBricks          The full series of Renko bricks up to the current day.
     * @param currentDayMarketData The market data for the day.
     * @param currentDayIndicators Map of currentDayIndicators (key: "metricCode_maType_period").
     * @param currentDayPosition   The current position of the backtest.
     * @param strategyState        A map to persist strategy-specific state (e.g., trailing SL).
     * @return The signal for action on the next day's open.
     */
    TradeSignal generateSignal(
            List<RenkoData> renkoBricks,
            MarketData currentDayMarketData,
            Map<String, BigDecimal> currentDayIndicators,
            PositionType currentDayPosition,
            Map<String, Object> strategyState
    );
}
