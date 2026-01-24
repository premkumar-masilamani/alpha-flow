package com.alphaflow.domain.model;

import java.math.BigDecimal;

public record CapitalProfileMetrics(
        BigDecimal pointOfControl,
        BigDecimal valueAreaHigh,
        BigDecimal valueAreaLow
) {
}
