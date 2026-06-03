package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.api.services.DailyPriceService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DailyPriceController {

  private static final Logger log = LoggerFactory.getLogger(DailyPriceController.class);

  private final DailyPriceService dailyPriceService;

  public DailyPriceController(DailyPriceService dailyPriceService) {
    this.dailyPriceService = dailyPriceService;
  }

  @GetMapping("/tickers/{symbol}/data")
  public List<OhlcvDTO> getCandleDataForTicker(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(required = false) Integer size) {
    log.info("Request to get daily data for ticker: {}, page: {}, size: {}", symbol, page, size);
    return dailyPriceService.getDailyPriceByTickerName(symbol, page, size);
  }
}
