package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.AnalysisResponseDTO;
import com.alphaflow.engine.strategies.ASTAStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class AnalysisController {

  private final ASTAStrategy astaStrategy;

  public AnalysisController(ASTAStrategy astaStrategy) {
    this.astaStrategy = astaStrategy;
  }

  @GetMapping("/tickers/{symbol}/analysis")
  public AnalysisResponseDTO getTechnicalAnalysisForTicker(@PathVariable String symbol) {
    log.info("Request to get technical analysis for ticker: {}", symbol);
    return astaStrategy.getAnalysis(symbol);
  }
}
