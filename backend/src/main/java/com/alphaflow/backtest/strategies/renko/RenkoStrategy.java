package com.alphaflow.backtest.strategies.renko;

import com.alphaflow.backtest.strategies.Strategy;
import com.alphaflow.infrastructure.enums.RenkoPriceSource;

public interface RenkoStrategy extends Strategy {
    RenkoPriceSource getPriceSource();
}