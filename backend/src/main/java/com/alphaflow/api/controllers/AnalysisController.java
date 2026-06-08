package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.AnalysisResponseDTO;
import com.alphaflow.api.services.AnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class AnalysisController {

  private final AnalysisService analysisService;

  public AnalysisController(AnalysisService analysisService) {
    this.analysisService = analysisService;
  }

  @GetMapping("/tickers/{symbol}/analysis")
  public AnalysisResponseDTO getTechnicalAnalysisForTicker(@PathVariable String symbol) {
    log.info("Request to get technical analysis for ticker: {}", symbol);
    return analysisService.getAnalysis(symbol);
  }
}
