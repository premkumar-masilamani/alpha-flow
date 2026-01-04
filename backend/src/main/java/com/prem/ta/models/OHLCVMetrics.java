package com.prem.ta.models;

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
