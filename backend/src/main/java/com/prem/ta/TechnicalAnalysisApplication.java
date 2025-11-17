package com.prem.ta;

import com.prem.ta.services.DataDownloadService;
import com.prem.ta.services.DataProcessingService;
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
            DataDownloadService dataDownloadService,
            DataProcessingService dataProcessingService
    ) {
        return args -> {
            dataDownloadService.download();
            dataProcessingService.process();
        };
    }
}
