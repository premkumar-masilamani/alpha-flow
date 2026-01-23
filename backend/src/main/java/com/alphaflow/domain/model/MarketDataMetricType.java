package com.alphaflow.domain.model;

import com.alphaflow.infrastructure.persistence.entities.MarketData;

import java.math.BigDecimal;
import java.util.function.Function;

public enum MarketDataMetricType {

    VOLUME("VOL", MarketData::getVolume),

    VWAP("VWAP", MarketData::getVwap),

    VOLUME_PROFILE_POC("VP_POC", MarketData::getVolumeProfilePOC),

    VOLUME_PROFILE_VALUE_RANGE("VP_VR", md -> md.getVolumeProfileVAH().subtract(md.getVolumeProfileVAL())),

    BUYER_CAPITAL_SHARE("B_CAP", MarketData::getBuyerCapitalShare),

    BUYER_VOLUME_SHARE("B_VOL", MarketData::getBuyerVolumeShare);

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
