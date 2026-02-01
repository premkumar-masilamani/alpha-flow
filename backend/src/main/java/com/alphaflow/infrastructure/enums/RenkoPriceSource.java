package com.alphaflow.infrastructure.enums;

import com.alphaflow.infrastructure.entities.MarketData;
import java.math.BigDecimal;

public enum RenkoPriceSource {
    PRICE_CLOSE,
    VWAP,
    VWAP_OHLC4,
    VWAP_HLC3;

    public BigDecimal getPrice(MarketData marketData) {
        return switch (this) {
            case PRICE_CLOSE -> marketData.getPriceClose();
            case VWAP -> marketData.getVwap();
            case VWAP_OHLC4 -> marketData.getVwapOHLC4();
            case VWAP_HLC3 -> marketData.getVwapHLC3();
        };
    }

    public String getMetricCode() {
        return switch (this) {
            case PRICE_CLOSE -> "P_CLOSE";
            case VWAP -> "VWAP";
            case VWAP_OHLC4 -> "VWAP_OHLC4";
            case VWAP_HLC3 -> "VWAP_HLC3";
        };
    }
}
