package com.prem.ta;

import com.prem.ta.services.DataIngestionService;
import com.prem.ta.services.DataProcessingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TechnicalAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechnicalAnalysisApplication.class, args);
    }

    @Bean
    @ConditionalOnProperty(
        name = "app.run-runner",
        havingValue = "true",
        matchIfMissing = true
    )
    CommandLineRunner runner(
        DataIngestionService dataIngestionService,
        DataProcessingService dataProcessingService
    ) {
        return args -> {
            dataIngestionService.downloadData();
            dataProcessingService.processData();
        };
    }
}
