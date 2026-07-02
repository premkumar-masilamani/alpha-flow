package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.SupportResistanceDTO;
import com.alphaflow.api.services.SupportResistanceService;
import com.alphaflow.engine.calculators.SupportResistanceCalculator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickers")
@Slf4j
public class SupportResistanceController {

  private final SupportResistanceService srService;
  private final SupportResistanceCalculator calculator;

  public SupportResistanceController(
      SupportResistanceService srService, SupportResistanceCalculator calculator) {
    this.srService = srService;
    this.calculator = calculator;
  }

  @GetMapping("/{symbol}/sr")
  public List<SupportResistanceDTO> getSupportResistanceLines(@PathVariable String symbol) {
    log.info("Request to get SR lines for ticker: {}", symbol);

    return srService.getSupportResistance(symbol);
  }
}
