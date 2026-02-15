package com.alphaflow.engine.enums;

import com.alphaflow.infrastructure.entities.CandleData;
import com.alphaflow.infrastructure.enums.DataSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.alphaflow.engine.enums.TransformationType.EMA;
import static com.alphaflow.engine.enums.TransformationType.SMA;
import static com.alphaflow.engine.enums.WindowPeriod.*;
import static com.alphaflow.infrastructure.constants.AppConstants.DB_MATH_CONTEXT;

public enum CandleDataMetricType {

    VWAP(
            "VWAP",
            CandleData::getVwap
    ),

    CAPITAL_POC(
            "C_POC",
            CandleData::getCapitalPOC
    ),

    CAPITAL_VALUE_RANGE("C_VR",
            md -> md.getCapitalVAH().subtract(md.getCapitalVAL())
    ),


    TOTAL_CAPITAL(
            "T_CAP",
            CandleData::getTotalCapital
    ),

    CAPITAL_MOMENTUM(
            "CAP_MOM",
            null
    ),

    BUYER_CAPITAL_RATIO(
            "B_CAP_RATIO",
            md -> md.getBuyerCapital().divide(md.getTotalCapital(), DB_MATH_CONTEXT)
    ),

    OBV(
            "OBV",
            CandleData::getVolume,
            new MetricTransformSpec(
                    EnumSet.of(TransformationType.OBV),
                    EnumSet.of(ZERO_DAYS)
            )
    ),

    CCF(
            "CCF",
            md -> md.getBuyerCapital().multiply(BigDecimal.valueOf(2)).subtract(md.getTotalCapital(), DB_MATH_CONTEXT)
    ),

    PRICE_CLOSE(
            "P_CLOSE",
            CandleData::getPriceClose,
            new MetricTransformSpec(
                    EnumSet.of(SMA),
                    EnumSet.of(TEN_DAYS,
                            TWO_HUNDRED_DAYS)
            ),
            new MetricTransformSpec(
                    EnumSet.of(EMA),
                    EnumSet.of(THREE_DAYS,
                            FIVE_DAYS,
                            EIGHT_DAYS,
                            TEN_DAYS,
                            TWELVE_DAYS,
                            FIFTEEN_DAYS,
                            THIRTY_DAYS,
                            THIRTY_FIVE_DAYS,
                            FORTY_DAYS,
                            FORTY_FIVE_DAYS,
                            FIFTY_DAYS,
                            SIXTY_DAYS)
            )
    );

    private final String code;
    private final Function<CandleData, BigDecimal> extractor;
    private final List<MetricTransformSpec> transformSpecs;

    CandleDataMetricType(
            String code,
            Function<CandleData, BigDecimal> extractor,
            MetricTransformSpec... transformSpecs
    ) {
        this.code = code;
        this.extractor = extractor;
        this.transformSpecs = Arrays.asList(transformSpecs);
    }

    public BigDecimal extract(CandleData candle) {
        return extractor.apply(candle);
    }

    public String code() {
        return code;
    }

    public List<MetricTransformSpec> transformSpecs() {
        return transformSpecs;
    }

    public boolean isEligibleFor(DataSource source) {
        if (source == DataSource.YAHOO_FINANCE) {
            return switch (this) {
                case VWAP, CAPITAL_POC, CAPITAL_VALUE_RANGE, TOTAL_CAPITAL, BUYER_CAPITAL_RATIO, CCF, CAPITAL_MOMENTUM -> false;
                default -> true;
            };
        }
        return true;
    }

    public record MetricTransformSpec(
            Set<TransformationType> transformations,
            Set<WindowPeriod> periods
    ) {
    }
}
