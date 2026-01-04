package com.prem.ta.models;

import java.math.BigDecimal;

public record OrderFlowMetrics(
        BigDecimal buyerVolumeShare,
        BigDecimal buyerCapitalShare
) {
}
