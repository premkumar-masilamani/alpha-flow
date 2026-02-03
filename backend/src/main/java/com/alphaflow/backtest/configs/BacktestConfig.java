package com.alphaflow.backtest.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "alphaflow.backtest")
@Data
public class BacktestConfig {
    private boolean gridSearchEnabled = false;
}
