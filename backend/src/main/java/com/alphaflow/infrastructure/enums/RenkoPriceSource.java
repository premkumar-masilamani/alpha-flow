package com.alphaflow.infrastructure.enums;

import com.alphaflow.infrastructure.entities.MarketData;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public enum RenkoPriceSource {
    PRICE_CLOSE("P_CLOSE"),
    VWAP("VWAP");

    private final String code;

    public String code() {
        return code;
    }

    public BigDecimal getPrice(MarketData marketData) {
        return switch (this) {
            case PRICE_CLOSE -> marketData.getPriceClose();
            case VWAP -> marketData.getVwap();
        };
    }
}
