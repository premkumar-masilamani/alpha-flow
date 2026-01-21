package com.alphaflow.core.models;

import java.math.BigDecimal;

public record OHLCVMetrics(
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume,
        BigDecimal vwap
) {
}
