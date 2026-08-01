package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.CandlestickPatternDTO;
import com.alphaflow.api.services.CandlestickPatternService;
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

/** REST controller for retrieving candlestick patterns. */
@RestController
@RequestMapping("/api")
@Slf4j
public class PatternController {

  private final CandlestickPatternService patternService;
  private final TickerRepository tickerRepository;

  public PatternController(
      CandlestickPatternService patternService, TickerRepository tickerRepository) {
    this.patternService = patternService;
    this.tickerRepository = tickerRepository;
  }

  /** Endpoint to retrieve candlestick patterns for a specific ticker and timeframe. */
  @GetMapping("/tickers/{symbol}/patterns")
  public List<CandlestickPatternDTO> getPatterns(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "daily") Timeframe timeframe,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "250") int size) {

    int finalSize = Math.clamp(size, size, 1000);
    log.info(
        "Request to get candlestick patterns for ticker: {}, timeframe: {}, page: {}, size: {}",
        symbol,
        timeframe,
        page,
        finalSize);

    Ticker ticker =
        tickerRepository
            .findByTickerSymbolIgnoreCase(symbol)
            .orElseThrow(() -> new ResourceNotFoundException("Ticker not found: " + symbol));

    return patternService.getPatterns(ticker, timeframe, page, finalSize);
  }
}
