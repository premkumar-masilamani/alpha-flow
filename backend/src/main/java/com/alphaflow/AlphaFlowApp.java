package com.alphaflow;

import com.alphaflow.core.services.MarketDataComputationService;
import com.alphaflow.core.services.MarketStateComputationService;
import com.alphaflow.core.services.TickDataDownloadService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AlphaFlowApp {

    public static void main(String[] args) {
        SpringApplication.run(AlphaFlowApp.class, args);
    }

    @Bean
    CommandLineRunner runner(
            TickDataDownloadService tickDataDownloadService,
            MarketDataComputationService marketDataComputationService,
            MarketStateComputationService marketStateComputationService
    ) {
        return args -> {
            tickDataDownloadService.download();
            marketDataComputationService.compute();
            marketStateComputationService.compute();
        };
    }
}
