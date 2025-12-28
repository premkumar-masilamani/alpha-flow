package com.prem.ta.models;

public record OHLCVMetrics(
        double open,
        double high,
        double low,
        double close,
        double volume,
        double vwap
) {
}
