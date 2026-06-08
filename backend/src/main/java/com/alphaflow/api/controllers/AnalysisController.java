package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.AnalysisResponseDTO;
import com.alphaflow.api.services.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnalysisController {

  private static final Logger logger = LoggerFactory.getLogger(AnalysisController.class);
  private final AnalysisService analysisService;

  public AnalysisController(AnalysisService analysisService) {
    this.analysisService = analysisService;
  }

  @GetMapping("/tickers/{symbol}/analysis")
  public AnalysisResponseDTO getTechnicalAnalysisForTicker(@PathVariable String symbol) {
    logger.info("Request to get technical analysis for ticker: {}", symbol);
    return analysisService.getAnalysis(symbol);
  }
}
