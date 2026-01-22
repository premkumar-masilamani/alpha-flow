package com.alphaflow.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "alphaflow.binance")
@Data
public class AppConfig {

    private String downloadUrl;
    private String downloadDir;

}
