package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.api.services.IntradayPriceService;
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
public class IntradayPriceController {

  private final IntradayPriceService intradayPriceService;
  private final TickerService tickerService;

  public IntradayPriceController(
      IntradayPriceService intradayPriceService, TickerService tickerService) {
    this.intradayPriceService = intradayPriceService;
    this.tickerService = tickerService;
  }

  @GetMapping("/tickers/{symbol}/intraday")
  public List<OhlcvDto> getIntradayPriceDataForTicker(
      @PathVariable String symbol,
      @RequestParam Timeframe timeframe,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "250") int size,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate) {

    int finalSize = Math.clamp(size, 1, 1000);
    log.info(
        "Request to get {} intraday data for ticker: {}, page: {}, size: {}, endDate: {}",
        timeframe,
        symbol,
        page,
        finalSize,
        endDate);

    Ticker ticker = tickerService.getTicker(symbol);

    if (timeframe == Timeframe._15M) {
      return intradayPriceService.getIntradayPrice(ticker, timeframe, endDate, page, finalSize);
    } else {
      log.error("Unsupported timeframe for intraday price data: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }
}
