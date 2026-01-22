package com.alphaflow;

import com.alphaflow.core.MarketDataComputer;
import com.alphaflow.core.MarketStateComputer;
import com.alphaflow.core.TickDataDownloader;
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
            TickDataDownloader tickDataDownloader,
            MarketDataComputer marketDataComputer,
            MarketStateComputer marketStateComputer
    ) {
        return args -> {
            tickDataDownloader.download();
            marketDataComputer.compute();
            marketStateComputer.compute();
        };
    }
}
