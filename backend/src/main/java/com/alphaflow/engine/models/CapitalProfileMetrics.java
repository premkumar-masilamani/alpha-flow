package com.alphaflow.engine.models;

import java.math.BigDecimal;

public record CapitalProfileMetrics(
        BigDecimal pointOfControl,
        BigDecimal valueAreaHigh,
        BigDecimal valueAreaLow
) {
}
