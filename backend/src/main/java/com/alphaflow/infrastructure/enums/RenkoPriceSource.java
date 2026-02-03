package com.alphaflow.infrastructure.enums;

import com.alphaflow.infrastructure.entities.MarketData;
import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RenkoPriceSource {
    PRICE_CLOSE("P_CLOSE"),
    VWAP("VWAP"),
    VWAP_OHLC4("VWAP_OHLC4"),
    VWAP_HLC3("VWAP_HLC3");

    private final String code;

    public String code() {
        return code;
    }

    public BigDecimal getPrice(MarketData marketData) {
        return switch (this) {
            case PRICE_CLOSE -> marketData.getPriceClose();
            case VWAP -> marketData.getVwap();
            case VWAP_OHLC4 -> marketData.getVwapOHLC4();
            case VWAP_HLC3 -> marketData.getVwapHLC3();
        };
    }
}
