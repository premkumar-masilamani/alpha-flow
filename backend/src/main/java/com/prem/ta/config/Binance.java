package com.prem.ta.config;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class Binance {

    /**
     * Map of ticker → start date.
     */
    private Map<String, LocalDate> tickers = new HashMap<>();

    public Map<String, LocalDate> getTickers() {
        return tickers;
    }

    public void setTickers(Map<String, LocalDate> tickers) {
        this.tickers = tickers;
    }
}
