package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.api.services.DailyPriceService;
import com.alphaflow.api.services.WeeklyPriceService;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
public class PriceController {

  private final DailyPriceService dailyPriceService;
  private final WeeklyPriceService weeklyPriceService;
  private final TickerRepository tickerRepository;

  public PriceController(
      DailyPriceService dailyPriceService,
      WeeklyPriceService weeklyPriceService,
      TickerRepository tickerRepository) {
    this.dailyPriceService = dailyPriceService;
    this.weeklyPriceService = weeklyPriceService;
    this.tickerRepository = tickerRepository;
  }

  @GetMapping("/tickers/{symbol}/data")
  public List<OhlcvDTO> getPriceDataForTicker(
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

    if (timeframe == Timeframe.MONTHLY) {
      throw new UnsupportedOperationException("Monthly timeframe not yet supported");
    }

    if (timeframe == Timeframe.WEEKLY) {
      return weeklyPriceService.getWeeklyPriceByTickerName(symbol, page, finalSize);
    }

    Ticker ticker =
        tickerRepository
            .findByTickerSymbolIgnoreCase(symbol)
            .orElseThrow(() -> new ResourceNotFoundException("Ticker not found: " + symbol));

    return dailyPriceService.getDailyPrice(ticker, page, finalSize);
  }
}
