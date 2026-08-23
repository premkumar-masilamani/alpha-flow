package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.CandlestickPatternDto;
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

@RestController
@RequestMapping("/api")
@Slf4j
public class CandlestickPatternController {

  private final CandlestickPatternService candlestickPatternService;
  private final TickerRepository tickerRepository;

  public CandlestickPatternController(
      CandlestickPatternService candlestickPatternService, TickerRepository tickerRepository) {
    this.candlestickPatternService = candlestickPatternService;
    this.tickerRepository = tickerRepository;
  }

  @GetMapping("/tickers/{symbol}/candlestick-patterns")
  public List<CandlestickPatternDto> getCandlestickPatterns(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "daily") Timeframe timeframe,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    int finalSize = Math.min(size, 250);
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

    return candlestickPatternService.getPatterns(ticker, timeframe, page, finalSize);
  }
}
