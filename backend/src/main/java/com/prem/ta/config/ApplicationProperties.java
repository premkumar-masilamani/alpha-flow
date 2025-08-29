package com.prem.ta.config;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Holds app.* properties from application.properties
 */
@Component
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    /**
     * Directory where Binance files will be stored.
     */
    private String downloadDir;

    /**
     * Map of ticker → start date.
     */
    private Map<String, LocalDate> tickers = new HashMap<>();

    public String getDownloadDir() {
        return downloadDir;
    }

    public void setDownloadDir(String downloadDir) {
        this.downloadDir = downloadDir;
    }

    public Map<String, LocalDate> getTickers() {
        return tickers;
    }

    public void setTickers(Map<String, LocalDate> tickers) {
        this.tickers = tickers;
    }
}
