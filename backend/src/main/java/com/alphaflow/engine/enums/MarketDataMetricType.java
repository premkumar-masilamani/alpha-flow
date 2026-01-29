package com.alphaflow.engine.enums;

import com.alphaflow.infrastructure.entities.MarketData;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

import static com.alphaflow.engine.enums.TransformationType.EMA;
import static com.alphaflow.engine.enums.TransformationType.SMA;
import static com.alphaflow.engine.enums.WindowPeriod.*;
import static com.alphaflow.infrastructure.constants.AppConstants.DB_MATH_CONTEXT;

public enum MarketDataMetricType {

    VWAP(
            "VWAP",
            MarketData::getVwap,
            new MetricTransformSpec(
                    EnumSet.of(EMA, SMA),
                    EnumSet.of(FIVE_DAYS, TEN_DAYS, TWENTY_ONE_DAYS)
            )
    ),

    VWAP_OHLC4(
            "VWAP_OHLC4",
            MarketData::getVwapOHLC4,
            new MetricTransformSpec(
                    EnumSet.of(SMA),
                    EnumSet.of(TEN_DAYS)
            )
    ),

    VWAP_HLC3(
            "VWAP_HLC3",
            MarketData::getVwapHLC3,
            new MetricTransformSpec(
                    EnumSet.of(SMA),
                    EnumSet.of(TEN_DAYS)
            )
    ),

    CAPITAL_POC(
            "C_POC",
            MarketData::getCapitalPOC,
            new MetricTransformSpec(
                    EnumSet.of(SMA),
                    EnumSet.of(FIVE_DAYS, TEN_DAYS)
            )
    ),

    CAPITAL_VALUE_RANGE("C_VR",
            md -> md.getCapitalVAH().subtract(md.getCapitalVAL()),
            new MetricTransformSpec(
                    EnumSet.of(SMA),
                    EnumSet.of(FIVE_DAYS)
            )),


    TOTAL_CAPITAL(
            "T_CAP",
            MarketData::getTotalCapital,
            new MetricTransformSpec(
                    EnumSet.of(EMA),
                    EnumSet.of(TEN_DAYS, TWENTY_DAYS)
            )
    ),

    CAPITAL_MOMENTUM(
            "CAP_MOM",
            null,
            new MetricTransformSpec(
                    EnumSet.of(TransformationType.CAP_MOM),
                    EnumSet.of(ZERO_DAYS)
            )
    ),

    BUYER_CAPITAL_RATIO(
            "B_CAP_RATIO",
            md -> md.getBuyerCapital().divide(md.getTotalCapital(), DB_MATH_CONTEXT),
            new MetricTransformSpec(
                    EnumSet.of(SMA),
                    EnumSet.of(FIVE_DAYS)
            )
    ),

    OBV(
            "OBV",
            MarketData::getVolume,
            new MetricTransformSpec(
                    EnumSet.of(TransformationType.OBV),
                    EnumSet.of(ZERO_DAYS)
            )
    ),

    PRICE_CLOSE(
            "P_CLOSE",
            MarketData::getPriceClose,
            new MetricTransformSpec(
                    EnumSet.of(SMA),
                    EnumSet.of(TEN_DAYS)
            )
    );

    private final String code;
    private final Function<MarketData, BigDecimal> extractor;
    private final MetricTransformSpec transformSpec;

    MarketDataMetricType(
            String code,
            Function<MarketData, BigDecimal> extractor,
            MetricTransformSpec transformSpec
    ) {
        this.code = code;
        this.extractor = extractor;
        this.transformSpec = transformSpec;
    }

    public BigDecimal extract(MarketData marketData) {
        return extractor.apply(marketData);
    }

    public String code() {
        return code;
    }

    public MetricTransformSpec transformSpec() {
        return transformSpec;
    }

    public record MetricTransformSpec(
            Set<TransformationType> transformations,
            Set<WindowPeriod> periods
    ) {
    }
}
