package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.TickerDTO;
import com.alphaflow.api.services.TickerService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TickerController {

  private static final Logger log = LoggerFactory.getLogger(TickerController.class);

  private final TickerService tickerService;

  public TickerController(TickerService tickerService) {
    this.tickerService = tickerService;
  }

  @GetMapping("/tickers")
  public List<TickerDTO> getAllTickers() {
    log.info("Request to get all tickers");
    return tickerService.getAllTickers();
  }

  @GetMapping("/tickers/{symbol}")
  public TickerDTO getTickerBySymbol(@PathVariable String symbol) {
    log.info("Request to get ticker by symbol: {}", symbol);
    return tickerService.getTickerBySymbol(symbol);
  }
}
