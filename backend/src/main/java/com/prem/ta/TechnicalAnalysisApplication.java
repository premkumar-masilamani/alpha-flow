package com.prem.ta;

import com.prem.ta.core.MarketDataComputationService;
import com.prem.ta.core.MarketStateComputationService;
import com.prem.ta.core.TickDataDownloadService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TechnicalAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechnicalAnalysisApplication.class, args);
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
