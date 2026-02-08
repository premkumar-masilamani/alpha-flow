package com.alphaflow.backtest.indicators;

public record IndicatorKey(String metric, String maType, int period) {

    @Override
    public String toString() {
        return String.valueOf(metric) + "_" + String.valueOf(maType) + "_" + period;
    }
}
