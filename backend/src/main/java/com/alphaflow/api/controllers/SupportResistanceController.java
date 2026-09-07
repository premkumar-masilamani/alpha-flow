package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.SupportResistanceDto;
import com.alphaflow.api.services.SupportResistanceService;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class SupportResistanceController {

  private final SupportResistanceService supportResistanceService;
  private final TickerRepository tickerRepository;

  public SupportResistanceController(
      SupportResistanceService supportResistanceService, TickerRepository tickerRepository) {
    this.supportResistanceService = supportResistanceService;
    this.tickerRepository = tickerRepository;
  }

  @GetMapping("/tickers/{symbol}/support-resistances")
  public List<SupportResistanceDto> getSupportResistances(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "daily") Timeframe timeframe,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

    log.info(
        "Request to get support resistances for ticker: {}, timeframe: {}, date: {}",
        symbol,
        timeframe,
        date);

    Ticker ticker =
        tickerRepository
            .findByTickerSymbolIgnoreCase(symbol)
            .orElseThrow(() -> new ResourceNotFoundException("Ticker not found: " + symbol));

    return supportResistanceService.getSupportResistances(ticker, timeframe, date);
  }
}
