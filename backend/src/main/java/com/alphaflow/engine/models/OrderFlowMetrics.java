package com.alphaflow.engine.models;

import java.math.BigDecimal;

public record OrderFlowMetrics(
        BigDecimal buyerCapital,
        BigDecimal totalCapital
) {
}
