package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.TickerDTO;
import com.alphaflow.api.services.TickerService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class TickerController {

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
    log.debug("Request to get ticker by symbol: {}", symbol);
    return tickerService.getTickerBySymbol(symbol);
  }
}
