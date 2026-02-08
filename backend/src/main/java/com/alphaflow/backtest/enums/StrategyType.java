package com.alphaflow.backtest.enums;

import com.alphaflow.backtest.entities.BacktestStrategy;
import com.alphaflow.backtest.strategies.Strategy;
import com.alphaflow.backtest.strategies.candlestick.BuyAndHoldRiskOverlayStrategy;
import com.alphaflow.backtest.strategies.candlestick.BuyAndHoldStrategy;
import com.alphaflow.backtest.strategies.renko.RenkoPPStrategy;
import com.alphaflow.backtest.strategies.renko.RenkoTSMStrategy;
import com.alphaflow.backtest.strategies.renko.RenkoTSMV2Strategy;

import java.util.Locale;
import java.util.function.Function;

public enum StrategyType {
    BUY_AND_HOLD(StrategyCategory.CANDLESTICK, BuyAndHoldStrategy::new),
    BUY_AND_HOLD_RISK_OVERLAY(StrategyCategory.CANDLESTICK, BuyAndHoldRiskOverlayStrategy::new),
    RENKO_TSM(StrategyCategory.RENKO, RenkoTSMStrategy::new),
    RENKO_PP(StrategyCategory.RENKO, RenkoPPStrategy::new),
    RENKO_TSM_V2(StrategyCategory.RENKO, RenkoTSMV2Strategy::new);

    private final StrategyCategory category;
    private final Function<BacktestStrategy, Strategy> factory;

    StrategyType(StrategyCategory category, Function<BacktestStrategy, Strategy> factory) {
        this.category = category;
        this.factory = factory;
    }

    public StrategyCategory getCategory() {
        return category;
    }

    public Strategy create(BacktestStrategy entity) {
        return factory.apply(entity);
    }

    public static StrategyType fromDb(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            throw new IllegalArgumentException("Strategy type is missing for backtest strategy");
        }
        try {
            return StrategyType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown strategy type: " + rawType, ex);
        }
    }
}
