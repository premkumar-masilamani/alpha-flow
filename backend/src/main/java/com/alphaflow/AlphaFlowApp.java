package com.alphaflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AlphaFlowApp {

    public static void main(String[] args) {
        SpringApplication.run(AlphaFlowApp.class, args);
    }

}
