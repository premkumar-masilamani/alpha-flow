package com.alphaflow.domain.strategy;

import com.alphaflow.domain.enums.PositionType;
import com.alphaflow.domain.enums.TradeSignal;
import com.alphaflow.infrastructure.persistence.entities.MarketData;
import com.alphaflow.infrastructure.persistence.entities.RenkoData;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Interface for strategies that rely on Renko bricks.
 * Because Renko bricks can be recalculated daily, the strategy receives the full series known today.
 */
public interface RenkoBacktestStrategy {

    String getName();

    /**
     * Generates a signal based on market data, Renko bricks, and computed indicators.
     *
     * @param currentDay        The market data for the day.
     * @param renkoBricks       The list of Renko bricks generated up to the current day.
     * @param indicators        Map of indicators for the current day.
     * @param currentPosition   The current position of the backtest.
     * @param strategyState     A map to store strategy-specific state between calls (e.g., Trailing SL).
     * @return The signal for action on the next day's open.
     */
    TradeSignal generateSignal(
            MarketData currentDay,
            List<RenkoData> renkoBricks,
            Map<String, BigDecimal> indicators,
            PositionType currentPosition,
            Map<String, Object> strategyState
    );
}
