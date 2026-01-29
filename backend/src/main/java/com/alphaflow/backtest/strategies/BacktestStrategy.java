package com.alphaflow.backtest.strategies;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.backtest.enums.TradeAction;
import com.alphaflow.infrastructure.entities.MarketData;

import java.math.BigDecimal;
import java.util.Map;

public interface BacktestStrategy {

    String getName();

    /**
     * Generates a tradeSignal based on market data and computed currentDayIndicators.
     *
     * @param currentDayMarketData The market data for the day.
     * @param currentDayIndicators Map of currentDayIndicators (key: "metricCode_maType_period").
     * @param currentDayPosition   The current positionType of the backtest.
     * @return The tradeSignal for tradeSignal on the next day's open.
     */
    TradeAction generateSignal(MarketData currentDayMarketData, Map<String, BigDecimal> currentDayIndicators, PositionType currentDayPosition);
}
