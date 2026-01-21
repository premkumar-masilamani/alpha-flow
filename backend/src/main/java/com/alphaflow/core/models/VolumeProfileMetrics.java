package com.alphaflow.core.models;

import java.math.BigDecimal;

public record VolumeProfileMetrics(
        BigDecimal pointOfControl,
        BigDecimal valueAreaHigh,
        BigDecimal valueAreaLow
) {
}
