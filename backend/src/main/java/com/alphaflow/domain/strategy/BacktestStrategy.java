package com.alphaflow.domain.strategy;

import com.alphaflow.domain.enums.BacktestSignal;
import com.alphaflow.domain.enums.PositionType;
import com.alphaflow.infrastructure.persistence.entities.MarketData;

import java.math.BigDecimal;
import java.util.Map;

public interface BacktestStrategy {
    String getName();

    /**
     * Generates a signal based on market data and computed indicators.
     * @param marketData The market data for the day.
     * @param indicators Map of indicators (key: "metricCode_maType_period").
     * @param currentPosition The current position of the backtest.
     * @return The signal for action on the next day's open.
     */
    BacktestSignal generateSignal(MarketData marketData, Map<String, BigDecimal> indicators, PositionType currentPosition);
}
