package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.SupportResistanceDTO;
import com.alphaflow.api.services.SupportResistanceService;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.calculators.SupportResistanceCalculator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
  public List<SupportResistanceDTO> getSupportResistanceLines(
      @PathVariable String symbol,
      @RequestParam(name = "timeframe", defaultValue = "DAILY") Timeframe timeframe) {
    log.info("Request to get SR lines for ticker: {}, timeframe: {}", symbol, timeframe);

    if (timeframe == Timeframe.MONTHLY) {
      throw new UnsupportedOperationException("Monthly timeframe not yet supported");
    }

    if (timeframe == Timeframe.WEEKLY) {
      return srService.getWeeklySr(symbol);
    }
    return srService.getDailySr(symbol);
  }
}
