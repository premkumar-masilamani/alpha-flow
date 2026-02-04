package com.alphaflow.engine.metrics;

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
