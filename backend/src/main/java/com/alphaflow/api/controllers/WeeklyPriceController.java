package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.api.services.WeeklyPriceService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class WeeklyPriceController {

  private static final Logger logger = LoggerFactory.getLogger(WeeklyPriceController.class);

  private final WeeklyPriceService weeklyPriceService;

  public WeeklyPriceController(WeeklyPriceService weeklyPriceService) {
    this.weeklyPriceService = weeklyPriceService;
  }

  @GetMapping("/tickers/{symbol}/weekly-data")
  public List<OhlcvDTO> getWeeklyDataForTicker(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "250") int size) {

    int finalSize = Math.max(1, Math.min(size, 1000));
    logger.info(
        "Request to get weekly data for ticker: {}, page: {}, size: {}", symbol, page, finalSize);
    return weeklyPriceService.getWeeklyPriceByTickerName(symbol, page, finalSize);
  }
}
