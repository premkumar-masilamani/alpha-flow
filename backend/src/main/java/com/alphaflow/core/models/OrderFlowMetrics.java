package com.alphaflow.core.models;

import java.math.BigDecimal;

public record OrderFlowMetrics(
        BigDecimal buyerVolumeShare,
        BigDecimal buyerCapitalShare
) {
}
