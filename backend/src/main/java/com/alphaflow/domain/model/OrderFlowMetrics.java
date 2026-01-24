package com.alphaflow.domain.model;

import java.math.BigDecimal;

public record OrderFlowMetrics(
        BigDecimal buyerCapital,
        BigDecimal totalCapital
) {
}
