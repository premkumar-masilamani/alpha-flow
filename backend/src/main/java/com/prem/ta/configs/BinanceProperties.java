package com.prem.ta.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.binance")
public class BinanceProperties {

    /**
     * Template URL for downloading Binance data.
     * Example: https://data.binance.vision/data/spot/daily/trades/{ticker}/{filename}
     */
    private String downloadUrl;

    /**
     * Local directory to store downloaded files.
     */
    private String downloadDir;

    // Getters and Setters
    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getDownloadDir() {
        return downloadDir;
    }

    public void setDownloadDir(String downloadDir) {
        this.downloadDir = downloadDir;
    }
}
