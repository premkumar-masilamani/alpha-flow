package com.prem.ta;

import com.prem.ta.services.BinanceService;
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
    CommandLineRunner runner(BinanceService binanceService) {
        return args -> {
            binanceService.downloadData();
        };
    }
}
