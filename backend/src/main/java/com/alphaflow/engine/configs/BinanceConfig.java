package com.alphaflow.engine.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "alphaflow.binance")
@Data
public class BinanceConfig {

    private String downloadUrl;
    private String downloadDir;

}
