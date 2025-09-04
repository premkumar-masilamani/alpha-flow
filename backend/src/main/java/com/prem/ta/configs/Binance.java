package com.prem.ta.configs;

public class Binance {

    /**
     * URL template for downloading daily trade data.
     * Must contain {ticker} and {filename} placeholders.
     */
    private String downloadUrl;

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
