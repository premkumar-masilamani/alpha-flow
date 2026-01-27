package com.alphaflow.domain.model;

import java.math.BigDecimal;

public record OHLCVMetrics(
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume,
        BigDecimal vwap,
        BigDecimal vwapOHLC4,
        BigDecimal vwapHLC3
) {
}
