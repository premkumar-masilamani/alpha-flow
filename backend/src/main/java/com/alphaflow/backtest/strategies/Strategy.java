package com.alphaflow.backtest.strategies;

import com.alphaflow.backtest.enums.TradeAction;

public interface Strategy {

    String getName();

    TradeAction generateSignal(StrategyContext strategyContext);
}
