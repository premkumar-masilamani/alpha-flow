package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.SupportResistanceDto;
import com.alphaflow.api.services.SupportResistanceService;
import com.alphaflow.api.services.TickerService;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.Ticker;
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
  private final TickerService tickerService;

  public SupportResistanceController(
      SupportResistanceService supportResistanceService, TickerService tickerService) {
    this.supportResistanceService = supportResistanceService;
    this.tickerService = tickerService;
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

    Ticker ticker = tickerService.getTicker(symbol);

    return supportResistanceService.getSupportResistances(ticker, timeframe, date);
  }
}
