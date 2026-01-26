package com.alphaflow.domain.strategy;

import com.alphaflow.domain.enums.PositionType;
import com.alphaflow.domain.enums.TradeSignal;
import com.alphaflow.infrastructure.persistence.entities.MarketData;

import java.math.BigDecimal;
import java.util.Map;

public interface BacktestStrategy {

    String getName();

    /**
     * Generates a signal based on market data and computed currentDayIndicators.
     *
     * @param currentDayMarketData The market data for the day.
     * @param currentDayIndicators Map of currentDayIndicators (key: "metricCode_maType_period").
     * @param currentDayPosition   The current position of the backtest.
     * @return The signal for action on the next day's open.
     */
    TradeSignal generateSignal(MarketData currentDayMarketData, Map<String, BigDecimal> currentDayIndicators, PositionType currentDayPosition);
}
