package com.alphaflow.backtest.strategies.renko;

import com.alphaflow.backtest.strategies.Strategy;
import com.alphaflow.backtest.strategies.StrategyState;
import com.alphaflow.infrastructure.enums.RenkoPriceSource;

public interface RenkoStrategy<S extends StrategyState> extends Strategy<S> {
    RenkoPriceSource getPriceSource();
}
