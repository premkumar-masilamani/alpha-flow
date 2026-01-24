package com.alphaflow.domain.model;

import com.alphaflow.infrastructure.persistence.entities.MarketData;

import java.math.BigDecimal;
import java.util.function.Function;

public enum MarketDataMetricType {

    VOLUME("VOL", MarketData::getVolume),

    VWAP("VWAP", MarketData::getVwap),

    CAPITAL_POC("C_POC", MarketData::getCapitalPOC),

    CAPITAL_VALUE_RANGE("C_VR", md -> md.getCapitalVAH().subtract(md.getCapitalVAL())),

    TOTAL_CAPITAL("T_CAP", MarketData::getTotalCapital),

    BUYER_CAPITAL("B_CAP", MarketData::getBuyerCapital);

    private final String code;
    private final Function<MarketData, BigDecimal> extractor;

    MarketDataMetricType(String code, Function<MarketData, BigDecimal> extractor) {
        this.code = code;
        this.extractor = extractor;
    }

    public BigDecimal extract(MarketData marketData) {
        return extractor.apply(marketData);
    }

    public String code() {
        return code;
    }
}
