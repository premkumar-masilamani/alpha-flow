package com.prem.ta.models;

import com.prem.ta.entities.MarketData;

import java.math.BigDecimal;
import java.util.function.Function;

public enum MarketStateMetricType {

    vol("vol", MarketData::getVolume),

    vwap("vwap", MarketData::getVwap),

    poc("poc", MarketData::getVolumeProfilePOC),

    vpr("vpr", md -> md.getVolumeProfileVAH().subtract(md.getVolumeProfileVAL())),

    vpd("vpd", md -> md.getVwap().subtract(md.getVolumeProfilePOC())),

    bcs("bcs", MarketData::getBuyerCapitalShare),

    bvs("bvs", MarketData::getBuyerVolumeShare);

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
