package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.ChartPatternDto;
import com.alphaflow.api.services.ChartPatternService;
import com.alphaflow.api.services.TickerService;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.ChartPatternStatus;
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
public class ChartPatternController {

  private static final int MAX_PAGE_SIZE = 250;

  private final ChartPatternService chartPatternService;
  private final TickerService tickerService;

  public ChartPatternController(
      ChartPatternService chartPatternService, TickerService tickerService) {
    this.chartPatternService = chartPatternService;
    this.tickerService = tickerService;
  }

  @GetMapping("/tickers/{symbol}/chart-patterns")
  public List<ChartPatternDto> getChartPatterns(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "daily") Timeframe timeframe,
      @RequestParam(required = false) ChartPatternStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    int finalSize = Math.min(size, MAX_PAGE_SIZE);
    log.info(
        "Request to get chart patterns for ticker: {}, timeframe: {}, status: {}, page: {}, size: {}",
        symbol,
        timeframe,
        status,
        page,
        finalSize);

    Ticker ticker = tickerService.getTicker(symbol);

    return chartPatternService.getPatterns(ticker, timeframe, status, page, finalSize);
  }
}
