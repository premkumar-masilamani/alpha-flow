package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.SupportResistanceDTO;
import com.alphaflow.api.services.SupportResistanceService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import com.alphaflow.engine.calculators.SupportResistanceCalculator;

@RestController
@RequestMapping("/api/tickers")
@Slf4j
public class SupportResistanceController {

  private final SupportResistanceService srService;
  private final SupportResistanceCalculator calculator;

  public SupportResistanceController(SupportResistanceService srService, SupportResistanceCalculator calculator) {
    this.srService = srService;
    this.calculator = calculator;
  }

  @GetMapping("/calculate-sr")
  public ResponseEntity<String> calculateAll() {
    calculator.computeAll();
    return ResponseEntity.ok("Calculated");
  }

  @GetMapping("/{symbol}/sr")
  public List<SupportResistanceDTO> getSupportResistance(
      @PathVariable String symbol,
      @RequestParam(name = "timeframe", defaultValue = "daily") String timeframe) {
    log.info("Request to get SR lines for ticker: {}, timeframe: {}", symbol, timeframe);
    if ("weekly".equalsIgnoreCase(timeframe)) {
      return srService.getWeeklySr(symbol);
    }
    return srService.getDailySr(symbol);
  }
}
