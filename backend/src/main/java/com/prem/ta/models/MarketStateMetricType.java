package com.prem.ta.models;

import com.prem.ta.entities.MarketData;

import java.math.BigDecimal;
import java.util.function.Function;

public enum MarketStateMetricType {

    VOLUME("VOLUME", MarketData::getVolume),

    VWAP("VWAP", MarketData::getVwap),

    VOLUME_PROFILE_POC("VP_POC", MarketData::getVolumeProfilePOC),

    VOLUME_PROFILE_VALUE_RANGE(
            "VP_VALUE_RANGE",
            md -> md.getVolumeProfileVAH()
                    .subtract(md.getVolumeProfileVAL())
    ),

    BUYER_CAPITAL_SHARE("BUYER_CAPITAL_SHARE", MarketData::getBuyerCapitalShare),

    BUYER_VOLUME_SHARE("BUYER_VOLUME_SHARE", MarketData::getBuyerVolumeShare);

    private final String code;
    private final Function<MarketData, BigDecimal> extractor;

    MarketStateMetricType(String code, Function<MarketData, BigDecimal> extractor) {
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
