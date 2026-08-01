package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.IndicatorConfigDto;
import com.alphaflow.api.dtos.IndicatorSeriesDto;
import com.alphaflow.api.services.IndicatorService;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class IndicatorController {

  private final IndicatorService indicatorService;
  private final TickerRepository tickerRepository;

  public IndicatorController(IndicatorService indicatorService, TickerRepository tickerRepository) {
    this.indicatorService = indicatorService;
    this.tickerRepository = tickerRepository;
  }

  @GetMapping("/indicator-definitions")
  public List<IndicatorConfigDto> getConfiguredIndicators() {
    log.info("Request to get configured indicators");
    return indicatorService.getConfiguredIndicators();
  }

  @GetMapping("/tickers/{symbol}/indicators")
  public List<IndicatorSeriesDto> getIndicatorSeries(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "daily") Timeframe timeframe,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "250") int size) {

    int finalSize = Math.clamp(size, size, 1000);
    Ticker ticker =
        tickerRepository
            .findByTickerSymbolIgnoreCase(symbol)
            .orElseThrow(() -> new ResourceNotFoundException("Ticker not found: " + symbol));
    return indicatorService.getIndicatorSeries(ticker, timeframe, page, finalSize);
  }
}
