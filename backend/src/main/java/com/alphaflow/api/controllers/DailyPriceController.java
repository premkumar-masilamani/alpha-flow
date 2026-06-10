package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
public class DailyPriceController {

  private final DailyPriceRepository dailyPriceRepository;
  private final TickerRepository tickerRepository;

  public DailyPriceController(
      DailyPriceRepository dailyPriceRepository, TickerRepository tickerRepository) {
    this.dailyPriceRepository = dailyPriceRepository;
    this.tickerRepository = tickerRepository;
  }

  @GetMapping("/tickers/{symbol}/data")
  public List<OhlcvDTO> getDailyPriceDataForTicker(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "250") int size) {

    int finalSize = Math.clamp(size, size, 1000);
    log.info(
        "Request to get daily data for ticker: {}, page: {}, size: {}", symbol, page, finalSize);

    Ticker ticker =
        tickerRepository
            .findByTickerSymbolIgnoreCase(symbol)
            .orElseThrow(() -> new ResourceNotFoundException("Ticker not found: " + symbol));

    return dailyPriceRepository.getDailyPrice(ticker, page, finalSize);
  }
}
