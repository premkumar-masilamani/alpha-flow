package com.alphaflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class AlphaFlowApp {

  private static final Logger LOGGER = LoggerFactory.getLogger(AlphaFlowApp.class);

  public static void main(String[] args) {
    LOGGER.info("Starting Alpha Flow Application...");
    SpringApplication.run(AlphaFlowApp.class, args);
    LOGGER.info("Alpha Flow Application started successfully.");
  }
}
