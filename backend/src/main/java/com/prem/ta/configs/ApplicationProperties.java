package com.prem.ta.configs;

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

    private final Binance binance = new Binance();

    public String getDownloadDir() {
        return downloadDir;
    }

    public void setDownloadDir(String downloadDir) {
        this.downloadDir = downloadDir;
    }

    public Binance getBinance() {
        return binance;
    }
}
