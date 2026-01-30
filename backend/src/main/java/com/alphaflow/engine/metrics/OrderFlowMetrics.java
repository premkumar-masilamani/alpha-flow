package com.alphaflow.engine.metrics;

import java.math.BigDecimal;

public record OrderFlowMetrics(
        BigDecimal buyerCapital,
        BigDecimal totalCapital
) {
}
