package com.alphaflow.domain.model;

import java.math.BigDecimal;

public record VolumeProfileMetrics(
        BigDecimal pointOfControl,
        BigDecimal valueAreaHigh,
        BigDecimal valueAreaLow
) {
}
