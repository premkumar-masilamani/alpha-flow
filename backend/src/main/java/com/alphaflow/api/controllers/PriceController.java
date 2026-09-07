package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.api.services.DailyPriceService;
import com.alphaflow.api.services.TickerService;
import com.alphaflow.api.services.WeeklyPriceService;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.Ticker;
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
public class PriceController {

  private final DailyPriceService dailyPriceService;
  private final WeeklyPriceService weeklyPriceService;
  private final TickerService tickerService;

  public PriceController(
      DailyPriceService dailyPriceService,
      WeeklyPriceService weeklyPriceService,
      TickerService tickerService) {
    this.dailyPriceService = dailyPriceService;
    this.weeklyPriceService = weeklyPriceService;
    this.tickerService = tickerService;
  }

  @GetMapping("/tickers/{symbol}/data")
  public List<OhlcvDto> getPriceDataForTicker(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "daily") Timeframe timeframe,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "250") int size) {

    int finalSize = Math.clamp(size, size, 1000);
    log.info(
        "Request to get {} data for ticker: {}, page: {}, size: {}",
        timeframe,
        symbol,
        page,
        finalSize);

    Ticker ticker = tickerService.getTicker(symbol);

    if (timeframe == Timeframe.DAILY) {
      return dailyPriceService.getDailyPrice(ticker, page, finalSize);
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyPriceService.getWeeklyPrice(ticker, page, finalSize);
    } else {
      log.error("Unsupported timeframe for price data: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }
}
