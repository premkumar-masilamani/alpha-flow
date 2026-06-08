package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.api.services.WeeklyPriceService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
public class WeeklyPriceController {

  private final WeeklyPriceService weeklyPriceService;

  public WeeklyPriceController(WeeklyPriceService weeklyPriceService) {
    this.weeklyPriceService = weeklyPriceService;
  }

  @GetMapping("/tickers/{symbol}/weekly-data")
  public List<OhlcvDTO> getWeeklyDataForTicker(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "250") int size) {

    int finalSize = Math.clamp(size, size, 1000);
    log.info(
        "Request to get weekly data for ticker: {}, page: {}, size: {}", symbol, page, finalSize);
    return weeklyPriceService.getWeeklyPriceByTickerName(symbol, page, finalSize);
  }
}
