package com.alphaflow.engine.enums;

import com.alphaflow.infrastructure.entities.DailyCandleData;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.alphaflow.engine.enums.TransformationType.EMA;
import static com.alphaflow.engine.enums.TransformationType.SMA;
import static com.alphaflow.engine.enums.WindowPeriod.*;

public enum DailyCandleDataMetricType {

    OBV(
            "OBV",
            DailyCandleData::getVolume,
            new MetricTransformSpec(
                    EnumSet.of(TransformationType.OBV),
                    EnumSet.of(ZERO_DAYS)
            )
    ),

    PRICE_CLOSE(
            "P_CLOSE",
            DailyCandleData::getPriceClose,
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
    private final Function<DailyCandleData, BigDecimal> extractor;
    private final List<MetricTransformSpec> transformSpecs;

    DailyCandleDataMetricType(
            String code,
            Function<DailyCandleData, BigDecimal> extractor,
            MetricTransformSpec... transformSpecs
    ) {
        this.code = code;
        this.extractor = extractor;
        this.transformSpecs = Arrays.asList(transformSpecs);
    }

    public BigDecimal extract(DailyCandleData candle) {
        return extractor.apply(candle);
    }

    public String code() {
        return code;
    }

    public List<MetricTransformSpec> transformSpecs() {
        return transformSpecs;
    }

    public record MetricTransformSpec(
            Set<TransformationType> transformations,
            Set<WindowPeriod> periods
    ) {
    }
}
