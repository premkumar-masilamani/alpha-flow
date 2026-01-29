package com.alphaflow.backtest.strategies;

import com.alphaflow.infrastructure.enums.RenkoPriceSource;

public interface RenkoStrategy extends Strategy {
    default RenkoPriceSource getPriceSource() {
        return RenkoPriceSource.PRICE_CLOSE;
    }
}
