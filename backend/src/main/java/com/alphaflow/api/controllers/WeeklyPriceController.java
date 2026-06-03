package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.api.services.WeeklyPriceService;
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
public class WeeklyPriceController {

  private static final Logger log = LoggerFactory.getLogger(WeeklyPriceController.class);

  private final WeeklyPriceService weeklyPriceService;

  public WeeklyPriceController(WeeklyPriceService weeklyPriceService) {

    this.weeklyPriceService = weeklyPriceService;
  }

  @GetMapping("/tickers/{symbol}/weekly-data")
  public List<OhlcvDTO> getWeeklyDataForTicker(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(required = false) Integer size) {

    log.info("Request to get weekly data for ticker: {}, page: {}, size: {}", symbol, page, size);

    return weeklyPriceService.getWeeklyPriceByTickerName(symbol, page, size);
  }
}
