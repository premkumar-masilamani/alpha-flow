package com.alphaflow.infrastructure.enums;

import com.alphaflow.infrastructure.entities.MarketData;
import java.math.BigDecimal;

public enum RenkoPriceSource {
    PRICE_CLOSE,
    VWAP,
    VWAP_OHLC4,
    VWAP_HLC3;

    public BigDecimal getPrice(MarketData data) {
        return switch (this) {
            case PRICE_CLOSE -> data.getPriceClose();
            case VWAP -> data.getVwap();
            case VWAP_OHLC4 -> data.getVwapOHLC4();
            case VWAP_HLC3 -> data.getVwapHLC3();
        };
    }
}
