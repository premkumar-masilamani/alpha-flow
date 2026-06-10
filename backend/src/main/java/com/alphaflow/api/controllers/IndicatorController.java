package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.IndicatorConfigDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.api.services.IndicatorService;
import com.alphaflow.api.utils.APIUtil;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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

  @GetMapping("/indicators")
  public List<IndicatorConfigDTO> getConfiguredIndicators() {
    log.info("Request to get configured indicators");
    return indicatorService.getConfiguredIndicators();
  }

  @GetMapping("/tickers/{symbol}/indicators")
  public List<IndicatorSeriesDTO> getIndicatorSeries(
      @PathVariable String symbol,
      @RequestParam String timeframe,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "250") int size) {

    int finalSize = Math.clamp(size, size, 1000);
    log.info(
        "Request to get {} indicators for ticker: {}, page: {}, size: {}",
        timeframe,
        symbol,
        page,
        finalSize);

    Ticker ticker =
        tickerRepository
            .findByTickerSymbolIgnoreCase(symbol)
            .orElseThrow(() -> new ResourceNotFoundException("Ticker not found: " + symbol));

    return indicatorService.getIndicatorSeries(
        ticker, APIUtil.parseTimeframe(timeframe), page, finalSize);
  }
}
