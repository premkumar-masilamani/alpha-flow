package com.prem.ta.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Holds app.* properties from application.properties
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    /**
     * Directory where Binance files will be stored.
     */
    private String downloadDir;

    /**
     * URL template for downloading daily trade data from Binance.
     * Must contain {ticker} and {filename} placeholders.
     */
    private String binanceDownloadUrl;

    public String getDownloadDir() {
        return downloadDir;
    }

    public void setDownloadDir(String downloadDir) {
        this.downloadDir = downloadDir;
    }

    public String getBinanceDownloadUrl() {
        return binanceDownloadUrl;
    }

    public void setBinanceDownloadUrl(String binanceDownloadUrl) {
        this.binanceDownloadUrl = binanceDownloadUrl;
    }
}
