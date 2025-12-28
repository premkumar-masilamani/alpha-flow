package com.prem.ta.models;

import java.math.BigDecimal;

public record VolumeProfileMetrics(
        BigDecimal pointOfControl,
        BigDecimal valueAreaHigh,
        BigDecimal valueAreaLow
) {
}
